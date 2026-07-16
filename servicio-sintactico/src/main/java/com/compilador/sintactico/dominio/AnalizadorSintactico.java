package com.compilador.sintactico.dominio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Dominio puro del Analizador Sintactico.
 * Implementa un parser descendente recursivo (RDP) para validar
 * la estructura gramatical del JSON y construir el AST.
 *
 * Gramatica Libre de Contexto (GLC):
 *   inicio   -> objeto
 *   objeto   -> '{' miembros '}' | '{' '}'
 *   miembros -> par (',' par)*
 *   par      -> T_STRING ':' valor
 *   valor    -> T_STRING | T_NUMERO | T_BOOLEANO | objeto
 */
public class AnalizadorSintactico {

    /**
     * Representa un token recibido del Servicio Lexico.
     */
    private static class TokenEntrada {
        final String tipo;
        final String valor;
        final int linea;
        final int columna;

        TokenEntrada(String tipo, String valor, int linea, int columna) {
            this.tipo = tipo;
            this.valor = valor;
            this.linea = linea;
            this.columna = columna;
        }
    }

    /**
     * Resultado del analisis sintactico.
     */
    public static class ResultadoSintactico {
        private final JsonObject ast;
        private final JsonObject error;

        private ResultadoSintactico(JsonObject ast, JsonObject error) {
            this.ast = ast;
            this.error = error;
        }

        public static ResultadoSintactico exitoso(JsonObject ast) {
            return new ResultadoSintactico(ast, null);
        }

        public static ResultadoSintactico fallido(JsonObject error) {
            return new ResultadoSintactico(null, error);
        }

        public boolean esExitoso() { return error == null; }
        public JsonObject getAst() { return ast; }
        public JsonObject getError() { return error; }
    }

    // Estado del parser
    private List<TokenEntrada> tokens;
    private int posicionActual;
    private String codigoFuente;

    /**
     * Construye el AST a partir de la lista de tokens recibida del Lexico.
     */
    public ResultadoSintactico construirAst(JsonArray tokensJson, String codigoFuente) {
        this.codigoFuente = codigoFuente;
        this.tokens = new ArrayList<>();
        this.posicionActual = 0;

        // Convertir el JsonArray de tokens a la lista interna
        for (int i = 0; i < tokensJson.size(); i++) {
            JsonObject tokenObj = tokensJson.get(i).getAsJsonObject();
            tokens.add(new TokenEntrada(
                tokenObj.get("tipo").getAsString(),
                tokenObj.get("valor").getAsString(),
                tokenObj.get("linea").getAsInt(),
                tokenObj.get("columna").getAsInt()
            ));
        }

        try {
            JsonObject ast = parseInicio();

            // Verificar que se consumieron todos los tokens
            if (posicionActual < tokens.size()) {
                TokenEntrada sobrante = tokens.get(posicionActual);
                return ResultadoSintactico.fallido(crearError(
                    sobrante.linea, sobrante.columna,
                    sobrante.valor,
                    List.of("FIN_DE_ARCHIVO"),
                    "Se esperaba el fin del archivo, pero se encontro '" + sobrante.valor + "'"
                ));
            }

            return ResultadoSintactico.exitoso(ast);

        } catch (ErrorSintacticoExcepcion excepcion) {
            return ResultadoSintactico.fallido(excepcion.getError());
        }
    }

    // ========== REGLAS DE LA GRAMATICA (Parser Descendente Recursivo) ==========

    /**
     * inicio -> objeto
     */
    private JsonObject parseInicio() throws ErrorSintacticoExcepcion {
        JsonObject nodo = new JsonObject();
        nodo.addProperty("tipo", "inicio");
        JsonArray hijos = new JsonArray();
        hijos.add(parseObjeto());
        nodo.add("hijos", hijos);
        return nodo;
    }

    /**
     * objeto -> '{' miembros '}' | '{' '}'
     */
    private JsonObject parseObjeto() throws ErrorSintacticoExcepcion {
        consumir("LBRACE", "{");

        JsonObject nodo = new JsonObject();
        nodo.addProperty("tipo", "objeto");
        JsonArray hijos = new JsonArray();

        // Verificar si es un objeto vacio
        if (!verificarTipo("RBRACE")) {
            hijos.add(parseMiembros());
        }

        consumir("RBRACE", "}");
        nodo.add("hijos", hijos);
        return nodo;
    }

    /**
     * miembros -> par (',' par)*
     */
    private JsonObject parseMiembros() throws ErrorSintacticoExcepcion {
        JsonObject nodo = new JsonObject();
        nodo.addProperty("tipo", "miembros");
        JsonArray hijos = new JsonArray();

        hijos.add(parsePar());

        while (verificarTipo("COMMA")) {
            consumirActual(); // consumir la coma
            hijos.add(parsePar());
        }

        nodo.add("hijos", hijos);
        return nodo;
    }

    /**
     * par -> T_STRING ':' valor
     */
    private JsonObject parsePar() throws ErrorSintacticoExcepcion {
        JsonObject nodo = new JsonObject();
        nodo.addProperty("tipo", "par");
        JsonArray hijos = new JsonArray();

        // Clave (T_STRING)
        TokenEntrada tokenClave = consumir("T_STRING", "cadena de texto (clave)");
        hijos.add(crearNodoToken(tokenClave));

        // Dos puntos
        consumir("COLON", ":");

        // Valor
        hijos.add(parseValor());

        nodo.add("hijos", hijos);
        return nodo;
    }

    /**
     * valor -> T_STRING | T_NUMERO | T_BOOLEANO | objeto
     */
    private JsonObject parseValor() throws ErrorSintacticoExcepcion {
        JsonObject nodo = new JsonObject();
        nodo.addProperty("tipo", "valor");
        JsonArray hijos = new JsonArray();

        if (verificarTipo("T_STRING")) {
            TokenEntrada token = consumirActual();
            hijos.add(crearNodoToken(token));
        } else if (verificarTipo("T_NUMERO")) {
            TokenEntrada token = consumirActual();
            hijos.add(crearNodoToken(token));
        } else if (verificarTipo("T_BOOLEANO")) {
            TokenEntrada token = consumirActual();
            hijos.add(crearNodoToken(token));
        } else if (verificarTipo("LBRACE")) {
            hijos.add(parseObjeto());
        } else {
            // Error: se esperaba un valor
            TokenEntrada actual = obtenerActual();
            throw new ErrorSintacticoExcepcion(crearError(
                actual != null ? actual.linea : 0,
                actual != null ? actual.columna : 0,
                actual != null ? actual.valor : "FIN_DE_ARCHIVO",
                List.of("T_STRING", "T_NUMERO", "T_BOOLEANO", "LBRACE"),
                "Se esperaba un valor, pero se encontro '" +
                    (actual != null ? actual.valor : "fin del archivo") + "'"
            ));
        }

        nodo.add("hijos", hijos);
        return nodo;
    }

    // ========== UTILIDADES DEL PARSER ==========

    /**
     * Verifica si el token actual es del tipo esperado (sin consumirlo).
     */
    private boolean verificarTipo(String tipo) {
        TokenEntrada actual = obtenerActual();
        return actual != null && actual.tipo.equals(tipo);
    }

    /**
     * Obtiene el token actual sin avanzar la posicion.
     */
    private TokenEntrada obtenerActual() {
        if (posicionActual < tokens.size()) {
            return tokens.get(posicionActual);
        }
        return null;
    }

    /**
     * Consume el token actual y avanza la posicion.
     */
    private TokenEntrada consumirActual() {
        TokenEntrada token = tokens.get(posicionActual);
        posicionActual++;
        return token;
    }

    /**
     * Consume un token del tipo esperado. Si no coincide, lanza un error sintactico.
     */
    private TokenEntrada consumir(String tipoEsperado, String descripcion) throws ErrorSintacticoExcepcion {
        TokenEntrada actual = obtenerActual();

        if (actual == null) {
            throw new ErrorSintacticoExcepcion(crearError(
                0, 0, "FIN_DE_ARCHIVO",
                List.of(tipoEsperado),
                "Se esperaba " + descripcion + ", pero se encontro el fin del archivo"
            ));
        }

        if (!actual.tipo.equals(tipoEsperado)) {
            List<String> esperados = new ArrayList<>();
            esperados.add(tipoEsperado);

            // Si esperamos RBRACE, tambien podria ser una COMMA
            if (tipoEsperado.equals("RBRACE")) {
                esperados.add("COMMA");
            }

            throw new ErrorSintacticoExcepcion(crearError(
                actual.linea, actual.columna,
                actual.valor,
                esperados,
                "Se esperaba " + descripcion + ", pero se encontro '" + actual.valor + "'"
            ));
        }

        posicionActual++;
        return actual;
    }

    /**
     * Crea un nodo hoja del AST a partir de un token.
     */
    private JsonObject crearNodoToken(TokenEntrada token) {
        JsonObject nodo = new JsonObject();
        nodo.addProperty("tipo_token", token.tipo);
        nodo.addProperty("valor", token.valor);
        nodo.addProperty("linea", token.linea);
        nodo.addProperty("columna", token.columna);
        return nodo;
    }

    /**
     * Crea un objeto de error sintactico estandarizado.
     */
    private JsonObject crearError(int linea, int columna, String tokenInesperado,
                                   List<String> esperados, String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("tipo_error", "Sintactico");
        error.addProperty("linea", linea);
        error.addProperty("columna", columna);
        error.addProperty("token_inesperado", tokenInesperado);

        JsonArray esperadosArray = new JsonArray();
        for (String esperado : esperados) {
            esperadosArray.add(esperado);
        }
        error.add("esperado", esperadosArray);

        // Generar contexto
        String contexto = generarContexto(linea, columna);
        error.addProperty("contexto", contexto);
        error.addProperty("mensaje", mensaje);

        return error;
    }

    /**
     * Genera un fragmento de contexto para el error.
     */
    private String generarContexto(int linea, int columna) {
        if (codigoFuente == null || codigoFuente.isEmpty()) return "";

        String[] lineas = codigoFuente.split("\n");
        if (linea < 1 || linea > lineas.length) return "";

        String lineaTexto = lineas[linea - 1];
        StringBuilder marcador = new StringBuilder();
        for (int i = 1; i < columna && i <= lineaTexto.length(); i++) {
            marcador.append(" ");
        }
        marcador.append("^");

        return lineaTexto + "\n" + marcador;
    }

    /**
     * Excepcion interna para manejar errores sintacticos durante el parsing.
     */
    private static class ErrorSintacticoExcepcion extends Exception {
        private final JsonObject error;

        ErrorSintacticoExcepcion(JsonObject error) {
            super(error.has("mensaje") ? error.get("mensaje").getAsString() : "Error sintactico");
            this.error = error;
        }

        JsonObject getError() { return error; }
    }
}
