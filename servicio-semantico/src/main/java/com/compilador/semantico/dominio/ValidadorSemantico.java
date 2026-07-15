package com.compilador.semantico.dominio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Dominio puro del Validador Semantico.
 * Recibe el AST serializado como JSON, lo transforma a un objeto
 * de configuracion, y aplica las 3 reglas de negocio deterministas.
 *
 * Regla 1: Integridad Estructural (host y puerto obligatorios)
 * Regla 2: Restriccion de Rango (1 <= puerto <= 65535)
 * Regla 3: Consistencia Logica (modo=produccion -> debug=false)
 */
public class ValidadorSemantico {

    /**
     * Resultado de la validacion semantica.
     * Contiene la configuracion aprobada o el error detectado.
     */
    public static class ResultadoValidacion {
        private final ConfiguracionDTO configuracion;
        private final JsonObject error;

        private ResultadoValidacion(ConfiguracionDTO configuracion, JsonObject error) {
            this.configuracion = configuracion;
            this.error = error;
        }

        public static ResultadoValidacion exitoso(ConfiguracionDTO configuracion) {
            return new ResultadoValidacion(configuracion, null);
        }

        public static ResultadoValidacion fallido(JsonObject error) {
            return new ResultadoValidacion(null, error);
        }

        public boolean esExitoso() { return error == null; }
        public ConfiguracionDTO getConfiguracion() { return configuracion; }
        public JsonObject getError() { return error; }
    }

    /**
     * Valida el AST recibido aplicando las reglas de negocio.
     */
    public ResultadoValidacion validar(JsonObject ast) {
        // Paso 1: Transformar el AST a un objeto de configuracion
        ConfiguracionDTO configuracion = new ConfiguracionDTO();

        try {
            JsonObject nodoObjeto = obtenerNodoObjeto(ast);
            if (nodoObjeto == null) {
                return ResultadoValidacion.fallido(crearError(
                    "Transformacion",
                    "estructura",
                    "El AST no contiene un objeto raiz valido."
                ));
            }

            extraerPares(nodoObjeto, configuracion);
        } catch (Exception excepcion) {
            return ResultadoValidacion.fallido(crearError(
                "Transformacion",
                "ast",
                "Error al procesar el AST: " + excepcion.getMessage()
            ));
        }

        // Paso 2: Regla 1 - Integridad Estructural
        if (configuracion.getHost() == null) {
            return ResultadoValidacion.fallido(crearError(
                "Semantico",
                "host",
                "La clave 'host' es obligatoria en la configuracion."
            ));
        }

        if (configuracion.getPuerto() == 0) {
            return ResultadoValidacion.fallido(crearError(
                "Semantico",
                "puerto",
                "La clave 'puerto' es obligatoria en la configuracion."
            ));
        }

        // Paso 3: Regla 2 - Restriccion de Rango
        int puerto = configuracion.getPuerto();
        if (puerto < 1 || puerto > 65535) {
            JsonObject errorPuerto = crearError(
                "Semantico",
                "puerto",
                "El 'puerto' debe ser un numero entero entre 1 y 65535."
            );
            errorPuerto.addProperty("valor_encontrado", puerto);
            return ResultadoValidacion.fallido(errorPuerto);
        }

        // Paso 4: Regla 3 - Consistencia Logica
        String modo = configuracion.getModo();
        Boolean debug = configuracion.getDebug();

        if (modo != null && (modo.equals("produccion") || modo.equals("producción"))) {
            if (debug == null || debug) {
                JsonObject errorDebug = crearError(
                    "Semantico",
                    "debug",
                    "En modo 'produccion', la variable 'debug' debe ser obligatoriamente 'false'."
                );
                errorDebug.addProperty("modo", modo);
                if (debug != null) errorDebug.addProperty("debug", debug);
                return ResultadoValidacion.fallido(errorDebug);
            }
        }

        // Todo es valido
        return ResultadoValidacion.exitoso(configuracion);
    }

    /**
     * Navega el AST serializado para encontrar el nodo "objeto" raiz.
     */
    private JsonObject obtenerNodoObjeto(JsonObject nodo) {
        String tipo = nodo.has("tipo") ? nodo.get("tipo").getAsString() : "";

        if (tipo.equals("objeto")) {
            return nodo;
        }

        if (nodo.has("hijos") && nodo.get("hijos").isJsonArray()) {
            JsonArray hijos = nodo.getAsJsonArray("hijos");
            for (JsonElement hijo : hijos) {
                if (hijo.isJsonObject()) {
                    JsonObject resultado = obtenerNodoObjeto(hijo.getAsJsonObject());
                    if (resultado != null) return resultado;
                }
            }
        }

        return null;
    }

    /**
     * Extrae los pares clave-valor del nodo "objeto" del AST
     * y los asigna al DTO de configuracion.
     */
    private void extraerPares(JsonObject nodoObjeto, ConfiguracionDTO configuracion) {
        if (!nodoObjeto.has("hijos")) return;

        JsonArray hijos = nodoObjeto.getAsJsonArray("hijos");

        for (JsonElement hijo : hijos) {
            if (!hijo.isJsonObject()) continue;
            JsonObject nodoHijo = hijo.getAsJsonObject();
            String tipoHijo = nodoHijo.has("tipo") ? nodoHijo.get("tipo").getAsString() : "";

            if (tipoHijo.equals("miembros")) {
                // Recorrer los pares dentro de "miembros"
                extraerPares(nodoHijo, configuracion);
            } else if (tipoHijo.equals("par")) {
                procesarPar(nodoHijo, configuracion);
            }
        }
    }

    /**
     * Procesa un nodo "par" del AST para extraer la clave y el valor.
     */
    private void procesarPar(JsonObject nodoPar, ConfiguracionDTO configuracion) {
        JsonArray hijosDelPar = nodoPar.getAsJsonArray("hijos");
        if (hijosDelPar == null || hijosDelPar.size() < 2) return;

        // El primer hijo es la clave (T_STRING), el segundo es el valor
        JsonObject nodoClave = hijosDelPar.get(0).getAsJsonObject();
        JsonObject nodoValor = hijosDelPar.get(1).getAsJsonObject();

        String clave = extraerTextoToken(nodoClave);
        if (clave == null) return;

        // Quitar comillas de la clave
        clave = clave.replace("\"", "");

        // El nodoValor es de tipo "valor" y contiene un hijo con el token real
        String valorTexto = null;
        String tipoToken = null;

        if (nodoValor.has("tipo") && nodoValor.get("tipo").getAsString().equals("valor")) {
            JsonArray hijosValor = nodoValor.getAsJsonArray("hijos");
            if (hijosValor != null && hijosValor.size() > 0) {
                JsonObject tokenReal = hijosValor.get(0).getAsJsonObject();
                valorTexto = extraerTextoToken(tokenReal);
                tipoToken = tokenReal.has("tipo_token") ? tokenReal.get("tipo_token").getAsString() : "";
            }
        }

        if (valorTexto == null) return;

        // Asignar al DTO segun la clave
        switch (clave) {
            case "host":
                configuracion.setHost(valorTexto.replace("\"", ""));
                break;
            case "puerto":
                try {
                    configuracion.setPuerto(Integer.parseInt(valorTexto));
                } catch (NumberFormatException excepcion) {
                    configuracion.setPuerto(-1); // Forzar error de rango
                }
                break;
            case "modo":
                configuracion.setModo(valorTexto.replace("\"", ""));
                break;
            case "debug":
                configuracion.setDebug(valorTexto.equals("true"));
                break;
        }
    }

    /**
     * Extrae el texto de un nodo token del AST.
     */
    private String extraerTextoToken(JsonObject nodoToken) {
        if (nodoToken.has("valor")) {
            return nodoToken.get("valor").getAsString();
        }
        return null;
    }

    /**
     * Crea un objeto de error estandarizado.
     */
    private JsonObject crearError(String tipoError, String clave, String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo_error", tipoError);
        error.addProperty("clave", clave);
        error.addProperty("mensaje", mensaje);
        return error;
    }
}
