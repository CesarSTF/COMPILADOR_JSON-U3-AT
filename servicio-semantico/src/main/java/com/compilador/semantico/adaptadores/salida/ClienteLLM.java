package com.compilador.semantico.adaptadores.salida;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida: cliente HTTP hacia el servicio Ollama.
 * Se encarga de enviar errores semanticos al LLM para obtener
 * un diagnostico amigable y una solucion sugerida.
 */
public class ClienteLLM {

    private final String modelo;
    private final String urlApi;
    private final OkHttpClient clienteHttp;
    private final Gson gson;

    public ClienteLLM(String urlOllama, String modelo) {
        this.modelo = modelo;
        this.urlApi = urlOllama + "/api/chat";
        this.clienteHttp = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    /**
     * Envia el error al LLM y retorna un diagnostico estructurado.
     */
    public JsonObject diagnosticarError(String codigoFuente, JsonObject errorDetectado) {
        String prompt = construirPrompt(codigoFuente, errorDetectado);

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

                System.out.println("\n[LLM] Respuesta cruda del modelo:");
                try {
                    JsonObject jsonFormateado = JsonParser.parseString(contenido).getAsJsonObject();
                    System.out.println(gson.toJson(jsonFormateado));
                } catch (Exception excepcion) {
                    System.out.println(contenido);
                }

                return JsonParser.parseString(contenido).getAsJsonObject();
            }
        } catch (IOException excepcion) {
            System.err.println("[LLM] Error de conexion con Ollama: " + excepcion.getMessage());
        }

        // Respuesta por defecto si el LLM falla
        JsonObject respuestaError = new JsonObject();
        respuestaError.addProperty("hasError", true);
        com.google.gson.JsonArray parametros = new com.google.gson.JsonArray();
        parametros.add("error_interno");
        respuestaError.add("errParameter", parametros);
        respuestaError.addProperty("reason", "La respuesta del modelo fallo.");
        respuestaError.addProperty("solution_example", "Revise manualmente la configuracion.");
        return respuestaError;
    }

    private String construirPrompt(String codigoFuente, JsonObject errorDetectado) {
        String clave = errorDetectado.has("clave") ? errorDetectado.get("clave").getAsString() : "desconocida";
        String mensajeError = errorDetectado.has("mensaje") ? errorDetectado.get("mensaje").getAsString() : "";

        String plantilla = """
Eres un asistente estricto. Tu unica tarea es explicar un error SEMANTICO (reglas de negocio).

REGLAS DE NEGOCIO DEL COMPILADOR SEMANTICO:
1. 'host' es obligatorio.
2. 'puerto' es obligatorio y debe ser un numero entre 1 y 65535.
3. Si 'modo' es "produccion", entonces 'debug' debe ser estrictamente 'false'.

ERROR DETECTADO POR EL COMPILADOR:
Clave que fallo: __CLAVE__
Detalle: __MENSAJE__

CODIGO FUENTE ORIGINAL (Para referencia de la solucion):
__CODIGO_FUENTE__

INSTRUCCIONES:
1. Usando SOLAMENTE la regla de negocio violada mostrada arriba, redacta una explicacion amigable del error para el usuario. NO inventes otras reglas ni hables de sintaxis.
2. En "solution_example", devuelve el CODIGO FUENTE ORIGINAL completo pero aplicando la correccion semantica para arreglar el error.
3. Responde estrictamente en este formato JSON (sin texto adicional):
{
  "hasError": true,
  "errParameter": ["__CLAVE__"],
  "reason": "<tu_explicacion_amigable>",
  "solution_example": "<codigo_fuente_completo_corregido>"
}
""";
        return plantilla
            .replace("__CLAVE__", clave)
            .replace("__MENSAJE__", mensajeError)
            .replace("__CODIGO_FUENTE__", codigoFuente);
    }
}
