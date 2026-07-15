package com.compilador.semantico.dominio;

import com.google.gson.JsonObject;

/**
 * Modelo de datos que representa la configuracion
 * extraida del AST despues de la transformacion.
 */
public class ConfiguracionDTO {

    private String host;
    private int puerto;
    private String modo;
    private Boolean debug;

    public ConfiguracionDTO() {}

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPuerto() { return puerto; }
    public void setPuerto(int puerto) { this.puerto = puerto; }

    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }

    public Boolean getDebug() { return debug; }
    public void setDebug(Boolean debug) { this.debug = debug; }

    /**
     * Convierte la configuracion a un JsonObject para la respuesta HTTP.
     */
    public JsonObject aJson() {
        JsonObject json = new JsonObject();
        if (host != null) json.addProperty("host", host);
        json.addProperty("puerto", puerto);
        if (modo != null) json.addProperty("modo", modo);
        if (debug != null) json.addProperty("debug", debug);
        return json;
    }
}
