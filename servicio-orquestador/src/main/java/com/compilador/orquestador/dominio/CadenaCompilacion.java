package com.compilador.orquestador.dominio;

/**
 * Dominio puro del Orquestador.
 * Genera los prompts especializados para el LLM externo (Ollama)
 * segun la fase del compilador que fallo.
 *
 * No conoce HTTP ni Ollama. Solo genera texto.
 */
public class CadenaCompilacion {

    /**
     * Genera el prompt adecuado segun la fase que fallo.
     */
    public String generarPrompt(String faseFallo, String codigoFuente, String detalleTecnico) {
        return switch (faseFallo) {
            case "Lexico" -> generarPromptLexico(codigoFuente, detalleTecnico);
            case "Sintactico" -> generarPromptSintactico(codigoFuente, detalleTecnico);
            case "Semantico" -> generarPromptSemantico(codigoFuente, detalleTecnico);
            default -> "Explica este error de compilacion: " + detalleTecnico;
        };
    }

    private String generarPromptLexico(String codigoFuente, String detalleTecnico) {
        return """
Eres un asistente estricto. Tu unica tarea es explicar un error LEXICO (caracteres invalidos).

REGLAS DEL ANALIZADOR LEXICO:
El analizador reconoce EXCLUSIVAMENTE estos tokens:
1. LBRACE '{', RBRACE '}', COLON ':', COMMA ','
2. T_STRING: Cadenas entre comillas dobles ("texto")
3. T_NUMERO: Digitos del 0 al 9 (ej. 8080)
4. T_BOOLEANO: 'true' o 'false' (sin comillas)
5. WS: Espacios/tabs/saltos de linea (ignorados)

Cualquier otro caracter es un TOKEN INVALIDO.

ERROR DETECTADO:
""" + detalleTecnico + """

CODIGO FUENTE:
""" + codigoFuente + """

Responde estrictamente en este formato JSON:
{
  "hasError": true,
  "errParameter": ["formato_lexico"],
  "reason": "<explicacion_amigable>",
  "solution_example": "<codigo_fuente_completo_corregido>"
}

No agregues texto adicional.
No agregues markdown (como ```json).
No agregues comentarios.
""";
    }

    private String generarPromptSintactico(String codigoFuente, String detalleTecnico) {
        return """
Eres un experto en compiladores. Analiza el resultado de un error SINTACTICO (de estructura JSON).

═══════════════════════════════════════════
GRAMÁTICA DEL LENGUAJE (Sintaxis válida)
═══════════════════════════════════════════
- Todo debe estar dentro de un objeto JSON: { }
- Los miembros (pares clave-valor) se separan obligatoriamente por COMAS (,).
- No se permite una coma antes del primer elemento, ni después del último elemento (Trailing Comma).
- Cada par se forma por: "clave" : valor
- La clave debe ser SIEMPRE una cadena de texto (T_STRING).
- Los valores permitidos son: T_STRING ("texto"), T_NUMERO (ej. 8080), T_BOOLEANO (true/false) u otro objeto {}.

═══════════════════════════════════════════
TIPOS DE ERROR Y CÓMO RESPONDER (Ejemplos)
═══════════════════════════════════════════

Ejemplo 1 (Falta una coma entre elementos):
Entrada: {"clave": "valor" "otra": 2}
Respuesta esperada:
{
  "hasError": true,
  "errParameter": ["formato_sintactico"],
  "reason": "Falta una coma (',') para separar los elementos del JSON. Asegurate de colocar comas entre cada par de datos.",
  "solution_example": "{\\n  \\"clave\\": \\"valor\\",\\n  \\"otra\\": 2\\n}"
}

Ejemplo 2 (Coma sobrante al inicio o al final):
Entrada: { , "clave": "valor" }
Respuesta esperada:
{
  "hasError": true,
  "errParameter": ["formato_sintactico"],
  "reason": "Se encontro una coma sobrante al inicio del JSON. Los elementos no deben empezar ni terminar con coma.",
  "solution_example": "{\\n  \\"clave\\": \\"valor\\"\\n}"
}

═══════════════════════════════════════════
INSTRUCCIONES FINALES
═══════════════════════════════════════════
1. Analiza el siguiente ERROR DETECTADO POR EL PARSER y el CODIGO FUENTE proporcionado.
2. Identifica la causa técnica usando las reglas de la gramática.
3. Responde ÚNICAMENTE con JSON válido siguiendo EXACTAMENTE el formato de los ejemplos.
4. "solution_example" DEBE SER UN STRING PLANO con saltos de línea (\\n) formateando correctamente el código, NO UN OBJETO ANIDADO.

ERROR DETECTADO POR EL PARSER:
""" + detalleTecnico + """

CODIGO FUENTE CON EL ERROR:
""" + codigoFuente + """

Responde SOLO con el JSON:

No agregues texto adicional.
No agregues markdown.
No agregues comentarios.
""";
    }

    private String generarPromptSemantico(String codigoFuente, String detalleTecnico) {
        return """
Eres un asistente estricto. Tu unica tarea es explicar un error SEMANTICO (reglas de negocio).

REGLAS:
1. "host" y "puerto" son obligatorios.
2. "puerto" debe estar entre 1 y 65535.
3. Si "modo" es "produccion", "debug" debe ser false.

ERROR DETECTADO:
""" + detalleTecnico + """

CODIGO FUENTE:
""" + codigoFuente + """

Responde estrictamente en este formato JSON:
{
  "hasError": true,
  "errParameter": ["<clave_del_error>"],
  "reason": "<explicacion_amigable>",
  "solution_example": "<codigo_fuente_completo_corregido>"
}

No agregues texto adicional.
No agregues markdown.
No agregues comentarios.
""";
    }
}
