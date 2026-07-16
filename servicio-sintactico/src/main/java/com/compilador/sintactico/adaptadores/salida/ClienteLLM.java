package com.compilador.sintactico.adaptadores.salida;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida: cliente HTTP hacia el servicio Ollama.
 * Se encarga de enviar errores sintacticos al LLM para obtener
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

                System.out.println("\n[LLM-SINTACTICO] Respuesta cruda del modelo:");
                try {
                    JsonObject jsonFormateado = JsonParser.parseString(contenido).getAsJsonObject();
                    System.out.println(gson.toJson(jsonFormateado));
                } catch (Exception excepcion) {
                    System.out.println(contenido);
                }

                return JsonParser.parseString(contenido).getAsJsonObject();
            }
        } catch (IOException excepcion) {
            System.err.println("[LLM-SINTACTICO] Error de conexion con Ollama: " + excepcion.getMessage());
        }

        // Respuesta por defecto si el LLM falla
        JsonObject respuestaError = new JsonObject();
        respuestaError.addProperty("hasError", true);
        com.google.gson.JsonArray parametros = new com.google.gson.JsonArray();
        parametros.add("error_interno");
        respuestaError.add("errParameter", parametros);
        respuestaError.addProperty("reason", "La respuesta del modelo fallo.");
        respuestaError.addProperty("solution_example", "Revise manualmente la sintaxis de su archivo.");
        return respuestaError;
    }

    /**
     * Construye el prompt especializado para errores sintacticos.
     */
    private String construirPrompt(String codigoFuente, JsonObject errorDetectado) {
        String tokenInesperado = errorDetectado.has("token_inesperado")
            ? errorDetectado.get("token_inesperado").getAsString() : "";
        String contexto = errorDetectado.has("contexto")
            ? errorDetectado.get("contexto").getAsString() : "";
        String mensaje = errorDetectado.has("mensaje")
            ? errorDetectado.get("mensaje").getAsString() : "";

        // Construir la lista de tokens esperados
        StringBuilder esperados = new StringBuilder();
        if (errorDetectado.has("esperado")) {
            com.google.gson.JsonArray esperadosArray = errorDetectado.getAsJsonArray("esperado");
            for (int i = 0; i < esperadosArray.size(); i++) {
                if (i > 0) esperados.append(", ");
                esperados.append(esperadosArray.get(i).getAsString());
            }
        }

        String plantilla = """
Eres un asistente estricto. Tu unica tarea es explicar un error SINTACTICO (de estructura).

REGLAS DEL ANALIZADOR SINTACTICO (Gramatica permitida):
- Estructura JSON: Todo debe estar dentro de llaves { }
- Los elementos son pares de "clave": valor
- Los pares se separan estrictamente por comas (,)

ERROR DETECTADO POR EL COMPILADOR:
Contexto exacto del error:
__CONTEXTO__
Problema: __MENSAJE__

CODIGO FUENTE ORIGINAL (Para referencia de la solucion):
__CODIGO_FUENTE__

INSTRUCCIONES:
1. TRADUCCION: 'RBRACE' = llave de cierre '}', 'COMMA' = coma ',', 'LBRACE' = llave de apertura '{', 'COLON' = dos puntos ':', 'T_STRING' = cadena de texto.
2. Si el problema dice que se esperaba 'RBRACE' o 'COMMA', el error real es casi seguro que falta una coma (,) para separar los elementos. ENFOCATE SOLO EN LA FALTA DE COMA, omite lo de la llave de cierre.
3. Explica brevemente el error y que simbolo deberia ir en su lugar. NO inventes otras reglas.
4. En "solution_example", devuelve el CODIGO FUENTE ORIGINAL completo pero aplicando la correccion de la sintaxis.
5. Responde estrictamente en este formato JSON (sin texto adicional):
{
  "hasError": true,
  "errParameter": ["formato_sintactico"],
  "reason": "<tu_explicacion_amigable>",
  "solution_example": "<codigo_fuente_completo_corregido>"
}
""";
        return plantilla
            .replace("__CONTEXTO__", contexto)
            .replace("__MENSAJE__", mensaje)
            .replace("__CODIGO_FUENTE__", codigoFuente);
    }
}
