package com.compilador.orquestador.adaptadores.salida;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida: cliente HTTP hacia el LLM externo (Ollama).
 * Ollama NO es un microservicio nuestro, es una herramienta externa
 * cuya URL se configura via .env (OLLAMA_HOST, OLLAMA_MODELO).
 */
public class ClienteOllama {

    private final String modelo;
    private final String urlApi;
    private final OkHttpClient clienteHttp;
    private final Gson gson;

    public ClienteOllama(String urlOllama, String modelo) {
        this.modelo = modelo;
        this.urlApi = urlOllama + "/api/chat";
        this.clienteHttp = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    /**
     * Envia un prompt al LLM externo y retorna la respuesta como JsonObject.
     */
    public JsonObject consultar(String prompt) {
        JsonObject mensajeUsuario = new JsonObject();
        mensajeUsuario.addProperty("role", "user");
        mensajeUsuario.addProperty("content", prompt);

        com.google.gson.JsonArray mensajes = new com.google.gson.JsonArray();
        mensajes.add(mensajeUsuario);

        JsonObject cuerpoPeticion = new JsonObject();
        cuerpoPeticion.addProperty("model", modelo);
        cuerpoPeticion.add("messages", mensajes);
        cuerpoPeticion.addProperty("format", "json");
        cuerpoPeticion.addProperty("stream", false);

        RequestBody cuerpo = RequestBody.create(
            gson.toJson(cuerpoPeticion),
            MediaType.parse("application/json")
        );

        Request solicitud = new Request.Builder()
            .url(urlApi)
            .post(cuerpo)
            .build();

        try (Response respuesta = clienteHttp.newCall(solicitud).execute()) {
            if (respuesta.isSuccessful() && respuesta.body() != null) {
                String cuerpoRespuesta = respuesta.body().string();
                JsonObject datosRespuesta = JsonParser.parseString(cuerpoRespuesta).getAsJsonObject();
                String contenido = datosRespuesta
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();

                System.out.println("\n[LLM] Respuesta del modelo:");
                System.out.println(contenido);

                return JsonParser.parseString(contenido).getAsJsonObject();
            }
        } catch (IOException excepcion) {
            System.err.println("[LLM] Error de conexion con Ollama: " + excepcion.getMessage());
        }

        JsonObject respuestaError = new JsonObject();
        respuestaError.addProperty("hasError", true);
        com.google.gson.JsonArray parametros = new com.google.gson.JsonArray();
        parametros.add("error_interno");
        respuestaError.add("errParameter", parametros);
        respuestaError.addProperty("reason", "El modelo de IA no esta disponible.");
        respuestaError.addProperty("solution_example", "Revise manualmente la configuracion.");
        return respuestaError;
    }
}
