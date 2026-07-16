package com.compilador.orquestador.adaptadores.entrada;

import com.compilador.orquestador.dominio.CadenaCompilacion;
import com.compilador.orquestador.adaptadores.salida.ClienteEslabon;
import com.compilador.orquestador.adaptadores.salida.ClienteOllama;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static spark.Spark.*;

/**
 * Adaptador de entrada del Orquestador.
 * Implementa la Cadena de Responsabilidad Distribuida:
 *   1. Llama al Lexico con el codigo fuente
 *   2. Si exito, toma los tokens y llama al Sintactico
 *   3. Si exito, toma el AST y llama al Semantico
 *   4. Si cualquier fase falla, desvia al LLM externo (Ollama)
 */
public class ServidorWeb {

    private final CadenaCompilacion cadena;
    private final ClienteEslabon clienteLexico;
    private final ClienteEslabon clienteSintactico;
    private final ClienteEslabon clienteSemantico;
    private final ClienteOllama clienteOllama;
    private final Gson gson;

    public ServidorWeb(CadenaCompilacion cadena,
                       ClienteEslabon clienteLexico,
                       ClienteEslabon clienteSintactico,
                       ClienteEslabon clienteSemantico,
                       ClienteOllama clienteOllama) {
        this.cadena = cadena;
        this.clienteLexico = clienteLexico;
        this.clienteSintactico = clienteSintactico;
        this.clienteSemantico = clienteSemantico;
        this.clienteOllama = clienteOllama;
        this.gson = new Gson();
    }

    public void iniciar(int puerto) {
        port(puerto);

        // CORS
        before((solicitud, respuesta) -> {
            respuesta.header("Access-Control-Allow-Origin", "*");
            respuesta.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            respuesta.header("Access-Control-Allow-Headers", "Content-Type");
        });

        options("/*", (solicitud, respuesta) -> {
            respuesta.status(200);
            return "";
        });

        // POST /compilar - Punto de entrada unico para el Frontend
        post("/compilar", (solicitud, respuesta) -> {
            respuesta.type("application/json");

            String codigoFuente = solicitud.body();
            if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
                respuesta.status(400);
                return crearError("El codigo fuente no puede estar vacio.");
            }

            System.out.println("=== INICIO DE CADENA DE COMPILACION ===");

            // ============ ESLABON 1: LEXICO ============
            System.out.println("[CADENA] Eslabon 1: Lexico...");
            ClienteEslabon.Respuesta resLexico = clienteLexico.llamar(codigoFuente);
            JsonObject dataLexico = resLexico.getJson();

            if (!dataLexico.has("exito") || !dataLexico.get("exito").getAsBoolean()) {
                System.out.println("[CADENA] FALLO en Lexico. Desviando al LLM...");
                return desviarAlLlm(respuesta, "Lexico", codigoFuente, dataLexico);
            }

            // ============ ESLABON 2: SINTACTICO ============
            System.out.println("[CADENA] Eslabon 2: Sintactico...");
            JsonObject payloadSintactico = new JsonObject();
            payloadSintactico.addProperty("codigo_fuente", codigoFuente);
            payloadSintactico.add("tokens", dataLexico.getAsJsonArray("tokens"));

            ClienteEslabon.Respuesta resSintactico =
                clienteSintactico.llamar(payloadSintactico.toString());
            JsonObject dataSintactico = resSintactico.getJson();

            if (!dataSintactico.has("exito") || !dataSintactico.get("exito").getAsBoolean()) {
                System.out.println("[CADENA] FALLO en Sintactico. Desviando al LLM...");
                return desviarAlLlm(respuesta, "Sintactico", codigoFuente, dataSintactico);
            }

            // ============ ESLABON 3: SEMANTICO ============
            System.out.println("[CADENA] Eslabon 3: Semantico...");
            JsonObject payloadSemantico = new JsonObject();
            payloadSemantico.addProperty("codigo_fuente", codigoFuente);
            payloadSemantico.add("ast", dataSintactico.getAsJsonObject("ast"));

            ClienteEslabon.Respuesta resSemantico =
                clienteSemantico.llamar(payloadSemantico.toString());
            JsonObject dataSemantico = resSemantico.getJson();

            if (!dataSemantico.has("exito") || !dataSemantico.get("exito").getAsBoolean()) {
                System.out.println("[CADENA] FALLO en Semantico. Desviando al LLM...");
                return desviarAlLlm(respuesta, "Semantico", codigoFuente, dataSemantico);
            }

            // ============ EXITO TOTAL ============
            System.out.println("[CADENA] EXITO. Todas las fases superadas.");
            respuesta.status(200);
            return dataSemantico.toString();
        });

        // GET /health
        get("/health", (solicitud, respuesta) -> {
            respuesta.type("application/json");
            JsonObject salud = new JsonObject();
            salud.addProperty("estado", "ok");
            salud.addProperty("servicio", "Orquestador");
            return salud.toString();
        });

        System.out.println("[ORQUESTADOR] Servidor iniciado en el puerto " + puerto);
    }

    /**
     * Desvia el flujo al LLM externo (Ollama) cuando un eslabon falla.
     * El dominio genera el prompt, el adaptador lo envia a Ollama,
     * y se retorna el diagnostico al Frontend.
     */
    private String desviarAlLlm(spark.Response respuesta, String faseFallo,
                                 String codigoFuente, JsonObject dataError) {
        // Extraer detalle tecnico del error
        String detalleTecnico = dataError.has("detalle_tecnico")
            ? dataError.get("detalle_tecnico").toString()
            : dataError.toString();

        // El Dominio genera el prompt especializado
        String prompt = cadena.generarPrompt(faseFallo, codigoFuente, detalleTecnico);

        // El Adaptador de salida envia al LLM externo
        JsonObject diagnosticoIa = clienteOllama.consultar(prompt);

        // Construir respuesta para el Frontend
        respuesta.status(422);
        JsonObject respuestaError = new JsonObject();
        respuestaError.addProperty("exito", false);
        respuestaError.addProperty("fase_fallo", faseFallo);
        if (dataError.has("detalle_tecnico")) {
            respuestaError.add("detalle_tecnico", dataError.get("detalle_tecnico"));
        } else {
            respuestaError.add("detalle_tecnico", dataError);
        }
        respuestaError.add("diagnostico_ia", diagnosticoIa);
        return respuestaError.toString();
    }

    private String crearError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("error", mensaje);
        return error.toString();
    }
}
