package com.compilador.lexico.adaptadores.salida;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida: cliente HTTP hacia el Servicio Sintactico.
 * Se encarga de reenviar los tokens al siguiente microservicio en la cadena.
 */
public class ClienteSintactico {

    private final String urlSintactico;
    private final OkHttpClient clienteHttp;

    public ClienteSintactico(String urlSintactico) {
        this.urlSintactico = urlSintactico;
        this.clienteHttp = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
    }

    /**
     * Resultado de la comunicacion con el servicio externo.
     */
    public static class RespuestaServicio {
        private final int codigoEstado;
        private final String cuerpo;

        public RespuestaServicio(int codigoEstado, String cuerpo) {
            this.codigoEstado = codigoEstado;
            this.cuerpo = cuerpo;
        }

        public int getCodigoEstado() { return codigoEstado; }
        public String getCuerpo() { return cuerpo; }
    }

    /**
     * Reenvia el payload al Servicio Sintactico.
     * Retorna el codigo HTTP y el cuerpo de la respuesta tal cual.
     */
    public RespuestaServicio reenviar(JsonObject payload) {
        RequestBody cuerpo = RequestBody.create(
            payload.toString(),
            MediaType.parse("application/json")
        );

        Request solicitud = new Request.Builder()
            .url(urlSintactico + "/analizar")
            .post(cuerpo)
            .build();

        try (Response respuesta = clienteHttp.newCall(solicitud).execute()) {
            String cuerpoRespuesta = respuesta.body() != null ? respuesta.body().string() : "{}";
            return new RespuestaServicio(respuesta.code(), cuerpoRespuesta);
        } catch (IOException excepcion) {
            System.err.println("[LEXICO] Error de conexion con Servicio Sintactico: "
                + excepcion.getMessage());

            JsonObject errorConexion = new JsonObject();
            errorConexion.addProperty("exito", false);
            errorConexion.addProperty("fase_fallo", "Comunicacion");
            errorConexion.addProperty("error",
                "No se pudo conectar con el Servicio Sintactico: " + excepcion.getMessage());
            return new RespuestaServicio(502, errorConexion.toString());
        }
    }
}
