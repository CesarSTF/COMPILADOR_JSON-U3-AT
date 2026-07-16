package com.compilador.sintactico.adaptadores.salida;

import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de salida: cliente HTTP hacia el Servicio Semantico.
 * Se encarga de reenviar el AST al siguiente microservicio en la cadena.
 */
public class ClienteSemantico {

    private final String urlSemantico;
    private final OkHttpClient clienteHttp;

    public ClienteSemantico(String urlSemantico) {
        this.urlSemantico = urlSemantico;
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
     * Reenvia el payload al Servicio Semantico.
     */
    public RespuestaServicio reenviar(JsonObject payload) {
        RequestBody cuerpo = RequestBody.create(
            payload.toString(),
            MediaType.parse("application/json")
        );

        Request solicitud = new Request.Builder()
            .url(urlSemantico + "/analizar")
            .post(cuerpo)
            .build();

        try (Response respuesta = clienteHttp.newCall(solicitud).execute()) {
            String cuerpoRespuesta = respuesta.body() != null ? respuesta.body().string() : "{}";
            return new RespuestaServicio(respuesta.code(), cuerpoRespuesta);
        } catch (IOException excepcion) {
            System.err.println("[SINTACTICO] Error de conexion con Servicio Semantico: "
                + excepcion.getMessage());

            JsonObject errorConexion = new JsonObject();
            errorConexion.addProperty("exito", false);
            errorConexion.addProperty("fase_fallo", "Comunicacion");
            errorConexion.addProperty("error",
                "No se pudo conectar con el Servicio Semantico: " + excepcion.getMessage());
            return new RespuestaServicio(502, errorConexion.toString());
        }
    }
}
