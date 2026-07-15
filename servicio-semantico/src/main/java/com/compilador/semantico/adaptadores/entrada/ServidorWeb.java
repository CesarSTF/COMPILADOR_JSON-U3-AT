package com.compilador.semantico.adaptadores.entrada;

import com.compilador.semantico.dominio.ValidadorSemantico;
import com.compilador.semantico.dominio.ValidadorSemantico.ResultadoValidacion;
import com.compilador.semantico.adaptadores.salida.ClienteLLM;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static spark.Spark.*;

/**
 * Adaptador de entrada: servidor HTTP con Spark Java.
 * Expone los endpoints REST del Servicio Semantico.
 */
public class ServidorWeb {

    private final ValidadorSemantico validador;
    private final ClienteLLM clienteLlm;

    public ServidorWeb(ValidadorSemantico validador, ClienteLLM clienteLlm) {
        this.validador = validador;
        this.clienteLlm = clienteLlm;
    }

    /**
     * Configura e inicia el servidor Spark en el puerto indicado.
     */
    public void iniciar(int puerto) {
        port(puerto);

        // Configurar CORS
        before((solicitud, respuesta) -> {
            respuesta.header("Access-Control-Allow-Origin", "*");
            respuesta.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            respuesta.header("Access-Control-Allow-Headers", "Content-Type");
        });

        options("/*", (solicitud, respuesta) -> {
            respuesta.status(200);
            return "";
        });

        // Endpoint principal: POST /analizar
        post("/analizar", (solicitud, respuesta) -> {
            respuesta.type("application/json");

            String cuerpo = solicitud.body();
            if (cuerpo == null || cuerpo.trim().isEmpty()) {
                respuesta.status(400);
                return crearRespuestaError("El cuerpo de la peticion no puede estar vacio.");
            }

            JsonObject datosRecibidos;
            try {
                datosRecibidos = JsonParser.parseString(cuerpo).getAsJsonObject();
            } catch (Exception excepcion) {
                respuesta.status(400);
                return crearRespuestaError("El cuerpo no es JSON valido.");
            }

            String codigoFuente = datosRecibidos.has("codigo_fuente")
                ? datosRecibidos.get("codigo_fuente").getAsString()
                : "";
            JsonObject ast = datosRecibidos.has("ast")
                ? datosRecibidos.getAsJsonObject("ast")
                : null;

            if (ast == null) {
                respuesta.status(400);
                return crearRespuestaError("No se recibio el AST.");
            }

            System.out.println("[SEMANTICO] AST recibido del Servicio Sintactico.");

            // Fase 3: Validacion Semantica
            ResultadoValidacion resultado = validador.validar(ast);

            if (resultado.esExitoso()) {
                System.out.println("[SEMANTICO] Configuracion valida.");
                respuesta.status(200);

                JsonObject respuestaExitosa = new JsonObject();
                respuestaExitosa.addProperty("exito", true);
                respuestaExitosa.addProperty("mensaje",
                    "La configuracion es lexica, sintactica y semanticamente valida.");
                respuestaExitosa.add("configuracion_aprobada", resultado.getConfiguracion().aJson());
                respuestaExitosa.add("ast", ast); // <- AST AGREGADO AQUI
                return respuestaExitosa.toString();
            } else {
                System.out.println("[SEMANTICO] Error semantico detectado: "
                    + resultado.getError().toString());

                // Consultar al LLM para diagnostico
                JsonObject diagnosticoIa = clienteLlm.diagnosticarError(
                    codigoFuente, resultado.getError()
                );

                respuesta.status(422);

                JsonObject respuestaError = new JsonObject();
                respuestaError.addProperty("exito", false);
                respuestaError.addProperty("fase_fallo", "Semantico");
                respuestaError.add("detalle_tecnico", resultado.getError());
                respuestaError.add("diagnostico_ia", diagnosticoIa);
                return respuestaError.toString();
            }
        });

        // Endpoint de salud: GET /health
        get("/health", (solicitud, respuesta) -> {
            respuesta.type("application/json");
            JsonObject salud = new JsonObject();
            salud.addProperty("estado", "ok");
            salud.addProperty("servicio", "Analizador Semantico");
            return salud.toString();
        });

        System.out.println("[SEMANTICO] Servidor iniciado en el puerto " + puerto);
    }

    /**
     * Crea una respuesta de error estandarizada.
     */
    private String crearRespuestaError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("error", mensaje);
        return error.toString();
    }
}
