package com.compilador.orquestador.adaptadores.salida;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida generico: cliente HTTP para llamar a cualquier eslabon
 * de la cadena (Lexico, Sintactico o Semantico).
 */
public class ClienteEslabon {

    private final String url;
    private final OkHttpClient clienteHttp;

    public ClienteEslabon(String url) {
        this.url = url;
        this.clienteHttp = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
    }

    /**
     * Resultado de la llamada al eslabon.
     */
    public static class Respuesta {
        private final int codigoEstado;
        private final String cuerpo;

        public Respuesta(int codigoEstado, String cuerpo) {
            this.codigoEstado = codigoEstado;
            this.cuerpo = cuerpo;
        }

        public int getCodigoEstado() { return codigoEstado; }
        public String getCuerpo() { return cuerpo; }

        public JsonObject getJson() {
            return JsonParser.parseString(cuerpo).getAsJsonObject();
        }
    }

    /**
     * Envia un payload al eslabon y retorna su respuesta.
     */
    public Respuesta llamar(String payload) {
        RequestBody cuerpo = RequestBody.create(
            payload,
            MediaType.parse("application/json")
        );

        Request solicitud = new Request.Builder()
            .url(url)
            .post(cuerpo)
            .build();

        try (Response respuesta = clienteHttp.newCall(solicitud).execute()) {
            String cuerpoRespuesta = respuesta.body() != null ? respuesta.body().string() : "{}";
            return new Respuesta(respuesta.code(), cuerpoRespuesta);
        } catch (IOException excepcion) {
            System.err.println("[ORQUESTADOR] Error de conexion con eslabon " + url
                + ": " + excepcion.getMessage());

            JsonObject errorConexion = new JsonObject();
            errorConexion.addProperty("exito", false);
            errorConexion.addProperty("fase_fallo", "Comunicacion");
            errorConexion.addProperty("error",
                "No se pudo conectar con el servicio: " + excepcion.getMessage());
            return new Respuesta(502, errorConexion.toString());
        }
    }
}
