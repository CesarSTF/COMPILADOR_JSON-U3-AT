package com.compilador.lexico.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dominio puro del Analizador Lexico.
 * Implementa la tokenizacion manual del codigo fuente JSON
 * utilizando expresiones regulares para cada tipo de token.
 *
 * Alfabeto permitido:
 *   - Simbolos estructurales: { } : ,
 *   - T_STRING: cadenas entre comillas dobles
 *   - T_NUMERO: secuencias de digitos (0-9)
 *   - T_BOOLEANO: palabras reservadas true | false
 *   - WS (espacios en blanco): ignorados
 */
public class AnalizadorLexico {

    /**
     * Representa un token individual extraido del codigo fuente.
     */
    public static class Token {
        private final String tipo;
        private final String valor;
        private final int linea;
        private final int columna;

        public Token(String tipo, String valor, int linea, int columna) {
            this.tipo = tipo;
            this.valor = valor;
            this.linea = linea;
            this.columna = columna;
        }

        public String getTipo() { return tipo; }
        public String getValor() { return valor; }
        public int getLinea() { return linea; }
        public int getColumna() { return columna; }
    }

    /**
     * Resultado del analisis lexico.
     * Contiene la lista de tokens o el error detectado.
     */
    public static class ResultadoLexico {
        private final List<Token> tokens;
        private final ErrorLexico error;

        private ResultadoLexico(List<Token> tokens, ErrorLexico error) {
            this.tokens = tokens;
            this.error = error;
        }

        public static ResultadoLexico exitoso(List<Token> tokens) {
            return new ResultadoLexico(tokens, null);
        }

        public static ResultadoLexico fallido(ErrorLexico error) {
            return new ResultadoLexico(null, error);
        }

        public boolean esExitoso() { return error == null; }
        public List<Token> getTokens() { return tokens; }
        public ErrorLexico getError() { return error; }
    }

    /**
     * Informacion detallada de un error lexico.
     */
    public static class ErrorLexico {
        private final int linea;
        private final int columna;
        private final String contexto;
        private final String mensaje;

        public ErrorLexico(int linea, int columna, String contexto, String mensaje) {
            this.linea = linea;
            this.columna = columna;
            this.contexto = contexto;
            this.mensaje = mensaje;
        }

        public int getLinea() { return linea; }
        public int getColumna() { return columna; }
        public String getContexto() { return contexto; }
        public String getMensaje() { return mensaje; }
    }

    // Expresiones regulares para cada tipo de token (orden importa)
    // Nota: Java no permite guiones bajos en nombres de grupos de regex,
    // por eso usamos TSTRING, TNUMERO, TBOOLEANO internamente.
    private static final Pattern PATRON_TOKEN = Pattern.compile(
        "(?<WS>\\s+)" +
        "|(?<TSTRING>\"[^\"]*\")" +
        "|(?<TNUMERO>[0-9]+)" +
        "|(?<TBOOLEANO>\\btrue\\b|\\bfalse\\b)" +
        "|(?<LBRACE>\\{)" +
        "|(?<RBRACE>\\})" +
        "|(?<COLON>:)" +
        "|(?<COMMA>,)"
    );

    /**
     * Tokeniza el codigo fuente recibido.
     * Recorre el texto caracter a caracter usando expresiones regulares.
     * Si encuentra un caracter que no coincide con ningun patron, genera un error lexico.
     */
    public ResultadoLexico tokenizar(String codigoFuente) {
        List<Token> listaTokens = new ArrayList<>();
        Matcher matcher = PATRON_TOKEN.matcher(codigoFuente);
        int posicion = 0;

        while (posicion < codigoFuente.length()) {
            if (matcher.find(posicion) && matcher.start() == posicion) {
                // Se encontro un token valido en la posicion actual
                String textoCoincidente = matcher.group();

                // Ignorar espacios en blanco (WS)
                if (matcher.group("WS") == null) {
                    String tipoToken = determinarTipo(matcher);
                    int[] posicionLinea = calcularLineaColumna(codigoFuente, matcher.start());

                    listaTokens.add(new Token(
                        tipoToken,
                        textoCoincidente,
                        posicionLinea[0],
                        posicionLinea[1]
                    ));
                }

                posicion = matcher.end();
            } else {
                // Caracter invalido encontrado: generar error lexico
                int[] posicionLinea = calcularLineaColumna(codigoFuente, posicion);
                String contexto = generarContexto(codigoFuente, posicion);

                ErrorLexico error = new ErrorLexico(
                    posicionLinea[0],
                    posicionLinea[1],
                    contexto,
                    "Se encontraron caracteres no validos o no reconocidos."
                );

                return ResultadoLexico.fallido(error);
            }
        }

        return ResultadoLexico.exitoso(listaTokens);
    }

    /**
     * Determina el tipo de token segun el grupo que hizo match.
     * Retorna el nombre canonico con guion bajo (T_STRING, T_NUMERO, T_BOOLEANO)
     * para mantener compatibilidad con el Servicio Sintactico.
     */
    private String determinarTipo(Matcher matcher) {
        if (matcher.group("TSTRING") != null) return "T_STRING";
        if (matcher.group("TNUMERO") != null) return "T_NUMERO";
        if (matcher.group("TBOOLEANO") != null) return "T_BOOLEANO";
        if (matcher.group("LBRACE") != null) return "LBRACE";
        if (matcher.group("RBRACE") != null) return "RBRACE";
        if (matcher.group("COLON") != null) return "COLON";
        if (matcher.group("COMMA") != null) return "COMMA";
        return "DESCONOCIDO";
    }

    /**
     * Calcula la linea y columna a partir de una posicion absoluta en el texto.
     */
    private int[] calcularLineaColumna(String texto, int posicion) {
        int linea = 1;
        int columna = 1;
        for (int i = 0; i < posicion && i < texto.length(); i++) {
            if (texto.charAt(i) == '\n') {
                linea++;
                columna = 1;
            } else {
                columna++;
            }
        }
        return new int[]{linea, columna};
    }

    /**
     * Genera un fragmento de contexto alrededor de la posicion del error
     * (similar al get_context de Lark en Python).
     */
    private String generarContexto(String texto, int posicion) {
        int inicio = Math.max(0, posicion - 20);
        int fin = Math.min(texto.length(), posicion + 20);
        String fragmento = texto.substring(inicio, fin);

        // Agregar un indicador de posicion
        int indicador = posicion - inicio;
        StringBuilder marcador = new StringBuilder();
        for (int i = 0; i < indicador; i++) {
            marcador.append(" ");
        }
        marcador.append("^");

        return fragmento + "\n" + marcador;
    }
}
