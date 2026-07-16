package com.compilador.sintactico.adaptadores.entrada;

import com.compilador.sintactico.dominio.AnalizadorSintactico;
import com.compilador.sintactico.dominio.AnalizadorSintactico.ResultadoSintactico;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static spark.Spark.*;

/**
 * Adaptador de entrada: servidor HTTP con Spark Java.
 * Expone los endpoints REST del Servicio Sintactico.
 *
 * Patron: Cadena de Responsabilidad Distribuida.
 * Este adaptador SOLO construye el AST y retorna el resultado al Orquestador.
 * No llama a ningun otro servicio (ni Semantico, ni LLM).
 */
public class ServidorWeb {

    private final AnalizadorSintactico analizador;

    public ServidorWeb(AnalizadorSintactico analizador) {
        this.analizador = analizador;
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
            JsonArray tokensRecibidos = datosRecibidos.has("tokens")
                ? datosRecibidos.getAsJsonArray("tokens")
                : new JsonArray();

            System.out.println("[SINTACTICO] Recibidos " + tokensRecibidos.size()
                + " tokens del Orquestador.");

            // Fase 2: Analisis Sintactico (Construccion del AST)
            ResultadoSintactico resultado = analizador.construirAst(tokensRecibidos, codigoFuente);

            if (!resultado.esExitoso()) {
                // Error sintactico: devolver directamente al Orquestador
                System.out.println("[SINTACTICO] Error sintactico detectado.");

                respuesta.status(422);
                JsonObject respuestaError = new JsonObject();
                respuestaError.addProperty("exito", false);
                respuestaError.addProperty("fase_fallo", "Sintactico");
                respuestaError.add("detalle_tecnico", resultado.getError());
                return respuestaError.toString();
            }

            // AST construido exitosamente: devolver al Orquestador
            System.out.println("[SINTACTICO] AST construido exitosamente.");

            respuesta.status(200);
            JsonObject respuestaExitosa = new JsonObject();
            respuestaExitosa.addProperty("exito", true);
            respuestaExitosa.add("ast", resultado.getAst());
            return respuestaExitosa.toString();
        });

        // Endpoint de salud: GET /health
        get("/health", (solicitud, respuesta) -> {
            respuesta.type("application/json");
            JsonObject salud = new JsonObject();
            salud.addProperty("estado", "ok");
            salud.addProperty("servicio", "Analizador Sintactico");
            return salud.toString();
        });

        System.out.println("[SINTACTICO] Servidor iniciado en el puerto " + puerto);
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
