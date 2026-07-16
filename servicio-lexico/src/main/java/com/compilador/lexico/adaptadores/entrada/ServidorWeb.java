package com.compilador.lexico.adaptadores.entrada;

import com.compilador.lexico.dominio.AnalizadorLexico;
import com.compilador.lexico.dominio.AnalizadorLexico.ResultadoLexico;
import com.compilador.lexico.dominio.AnalizadorLexico.Token;
import com.compilador.lexico.dominio.AnalizadorLexico.ErrorLexico;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static spark.Spark.*;

/**
 * Adaptador de entrada: servidor HTTP con Spark Java.
 * Expone los endpoints REST del Servicio Lexico.
 *
 * Patron: Cadena de Responsabilidad Distribuida.
 * Este adaptador SOLO tokeniza y retorna el resultado al Orquestador.
 * No llama a ningun otro servicio (ni Sintactico, ni LLM).
 */
public class ServidorWeb {

    private final AnalizadorLexico analizador;

    public ServidorWeb(AnalizadorLexico analizador) {
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

            String codigoFuente = solicitud.body();
            if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
                respuesta.status(400);
                return crearRespuestaError("El cuerpo de la peticion no puede estar vacio.");
            }

            System.out.println("[LEXICO] Codigo fuente recibido. Iniciando tokenizacion...");

            // Fase 1: Analisis Lexico (Tokenizacion)
            ResultadoLexico resultado = analizador.tokenizar(codigoFuente);

            if (!resultado.esExitoso()) {
                // Error lexico: devolver directamente al Orquestador
                ErrorLexico error = resultado.getError();
                System.out.println("[LEXICO] Error lexico detectado en linea "
                    + error.getLinea() + ", columna " + error.getColumna());

                JsonObject errorJson = new JsonObject();
                errorJson.addProperty("tipo_error", "Lexico");
                errorJson.addProperty("linea", error.getLinea());
                errorJson.addProperty("columna", error.getColumna());
                errorJson.addProperty("contexto", error.getContexto());
                errorJson.addProperty("mensaje", error.getMensaje());

                respuesta.status(422);
                JsonObject respuestaError = new JsonObject();
                respuestaError.addProperty("exito", false);
                respuestaError.addProperty("fase_fallo", "Lexico");
                respuestaError.add("detalle_tecnico", errorJson);
                return respuestaError.toString();
            }

            // Tokenizacion exitosa: devolver tokens al Orquestador
            System.out.println("[LEXICO] Tokenizacion exitosa. "
                + resultado.getTokens().size() + " tokens encontrados.");

            JsonArray tokensJson = new JsonArray();
            for (Token token : resultado.getTokens()) {
                JsonObject tokenObj = new JsonObject();
                tokenObj.addProperty("tipo", token.getTipo());
                tokenObj.addProperty("valor", token.getValor());
                tokenObj.addProperty("linea", token.getLinea());
                tokenObj.addProperty("columna", token.getColumna());
                tokensJson.add(tokenObj);
            }

            respuesta.status(200);
            JsonObject respuestaExitosa = new JsonObject();
            respuestaExitosa.addProperty("exito", true);
            respuestaExitosa.add("tokens", tokensJson);
            return respuestaExitosa.toString();
        });

        // Endpoint de salud: GET /health
        get("/health", (solicitud, respuesta) -> {
            respuesta.type("application/json");
            JsonObject salud = new JsonObject();
            salud.addProperty("estado", "ok");
            salud.addProperty("servicio", "Analizador Lexico");
            return salud.toString();
        });

        System.out.println("[LEXICO] Servidor iniciado en el puerto " + puerto);
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
