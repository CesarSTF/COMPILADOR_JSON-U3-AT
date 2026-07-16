# FASE 1: Análisis Léxico (Tokenización)

El microservicio Léxico es la primera etapa del pipeline. Recibe una cadena de texto (código fuente) y la divide en componentes lógicos indivisibles llamados **Tokens**. Esta fase detecta errores ortográficos o caracteres ilegales en el código.

## Implementación Técnica
* **Lenguaje:** Java 17
* **Método:** Autómatas Finitos a través de Expresiones Regulares (`java.util.regex.Pattern`).

## Alfabeto Permitido
El analizador léxico está programado para reconocer única y exclusivamente los siguientes lexemas. Cualquier otro carácter (como `@`, `'`, `#`) desencadena un **Fail-Fast** y lanza un error léxico.

### 1. Símbolos Estructurales (Terminales)
| Nombre del Token | Lexema | Descripción | Regex |
|------------------|--------|-------------|-------|
| `LBRACE` | `{` | Llave de apertura (Inicio de objeto) | `\{` |
| `RBRACE` | `}` | Llave de cierre (Fin de objeto) | `\}` |
| `COLON` | `:` | Dos puntos (Separador clave-valor)| `:` |
| `COMMA` | `,` | Coma (Separador de miembros) | `,` |

### 2. Valores
| Nombre del Token | Lexema | Descripción | Regex |
|------------------|--------|-------------|-------|
| `T_STRING` | `"texto"` | Cadena de texto encerrada estrictamente entre comillas dobles. | `"[^"]*"` |
| `T_NUMERO` | `123` | Secuencia de dígitos numéricos del 0 al 9. | `[0-9]+` |
| `T_BOOLEANO`| `true/false`| Palabras reservadas booleanas. | `\btrue\b\|\bfalse\b` |

### 3. Símbolos Ignorados
* **`WS` (Espacios en Blanco):** Los espacios, tabulaciones (`\t`) y saltos de línea (`\n`, `\r`) son identificados por el léxico con la regex `\s+` pero son descartados inmediatamente. No viajan a la siguiente fase.

## Mecanismo de Diagnóstico (LLM)
Si el autómata detecta un carácter que no coincide con ninguno de los patrones del alfabeto, el servicio corta el análisis y envía el error a **Ollama**. El *Prompt* del Léxico incluye explícitamente la lista de estos 7 tokens permitidos para asegurar que el modelo solo corrija errores de caracteres extraños y no alucine intentando adivinar reglas de negocio.
