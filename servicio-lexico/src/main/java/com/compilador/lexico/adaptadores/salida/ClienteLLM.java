package com.compilador.lexico.adaptadores.salida;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida: cliente HTTP hacia el servicio Ollama.
 * Se encarga de enviar errores lexicos al LLM para obtener
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

                System.out.println("\n[LLM-LEXICO] Respuesta cruda del modelo:");
                try {
                    JsonObject jsonFormateado = JsonParser.parseString(contenido).getAsJsonObject();
                    System.out.println(gson.toJson(jsonFormateado));
                } catch (Exception excepcion) {
                    System.out.println(contenido);
                }

                return JsonParser.parseString(contenido).getAsJsonObject();
            }
        } catch (IOException excepcion) {
            System.err.println("[LLM-LEXICO] Error de conexion con Ollama: " + excepcion.getMessage());
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
     * Construye el prompt especializado para errores lexicos.
     */
    private String construirPrompt(String codigoFuente, JsonObject errorDetectado) {
        String contexto = errorDetectado.has("contexto") ? errorDetectado.get("contexto").getAsString() : "";
        String mensaje = errorDetectado.has("mensaje") ? errorDetectado.get("mensaje").getAsString() : "";

        String plantilla = """
Eres un asistente estricto. Tu unica tarea es explicar un error LEXICO (caracteres invalidos).

REGLAS DEL ANALIZADOR LEXICO (Gramatica estricta):
El analizador reconoce EXCLUSIVAMENTE los siguientes tokens y lexemas:
1. Simbolos Terminales (Estructurales):
   - LBRACE: Llave de apertura '{'
   - RBRACE: Llave de cierre '}'
   - COLON: Dos puntos ':'
   - COMMA: Coma ','
2. Simbolos Terminales (Valores):
   - T_STRING: Cadenas de texto encerradas ESTRICTAMENTE entre comillas dobles (ej. "texto").
   - T_NUMERO: Secuencias de digitos numericos del 0 al 9 (ej. 8080).
   - T_BOOLEANO: Las palabras exactas 'true' o 'false' (sin comillas).
3. Espacios (WS): Espacios, tabs y saltos de linea son ignorados.

Cualquier lexema (caracter o palabra) que no encaje en las definiciones anteriores es considerado un TOKEN INVALIDO (ej. arrobas, palabras sin comillas dobles, comillas simples, etc).

ERROR DETECTADO POR EL COMPILADOR:
Contexto exacto del error:
__CONTEXTO__
Detalle: __MENSAJE__

CODIGO FUENTE ORIGINAL (Para referencia de la solucion):
__CODIGO_FUENTE__

INSTRUCCIONES:
1. Explica brevemente que caracter extraño o invalido se encontro en el contexto dado que no pertenece al alfabeto permitido. NO inventes otras reglas.
2. En "solution_example", devuelve el CODIGO FUENTE ORIGINAL completo pero corrigiendo la linea que tiene el error.
3. Responde estrictamente en este formato JSON (sin texto adicional):
{
  "hasError": true,
  "errParameter": ["formato_lexico"],
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
