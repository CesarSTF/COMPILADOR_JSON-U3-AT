# Documentación Oficial de Reglas del Compilador DSL

Este documento detalla las reglas estrictas bajo las cuales opera el pipeline de microservicios del Compilador DSL. Estas mismas reglas son las que utiliza el LLM (Ollama) para fundamentar sus diagnósticos sin inventar información (estrategia de *Grounding*).

---

## 1. Reglas Léxicas (Servicio Léxico)
**Objetivo:** Validar que todos los caracteres y tokens ingresados pertenezcan al alfabeto permitido, descartando símbolos extraños o ilegales.

**Alfabeto Permitido:**
*   **Símbolos Estructurales:**
    *   Llave de apertura: `{`
    *   Llave de cierre: `}`
    *   Dos puntos: `:`
    *   Coma: `,`
*   **Tipos de Datos (Tokens):**
    *   `STRING`: Cadenas de texto encerradas estrictamente entre comillas dobles (ej. `"localhost"`).
    *   `NUMBER`: Números enteros del 0 al 9 (ej. `8080`).
    *   `BOOLEAN`: Palabras reservadas `true` o `false` (sin comillas).
*   **Espacios en blanco:** Los espacios, tabulaciones y saltos de línea (`\n`, `\t`, ` `) son permitidos pero ignorados por el analizador.

**Restricciones Léxicas:**
Cualquier carácter o cadena que no encaje en lo anterior (como letras sin comillas, símbolos como `@`, `#`, `$`, emojis, o comillas simples `'`) disparará un Error Léxico.

---

## 2. Reglas Sintácticas (Servicio Sintáctico)
**Objetivo:** Validar que los tokens válidos formen una estructura con sentido (basado en la gramática libre de contexto).

**Gramática (Reglas Estructurales):**
1.  **Objeto Raíz:** El archivo entero debe estar contenido dentro de llaves de apertura y cierre (`{ ... }`).
2.  **Pares Clave-Valor:** El contenido interior debe estar formado exclusivamente por pares. La clave *siempre* debe ser un `STRING` (entre comillas), seguido de dos puntos `:`, seguido de un valor válido.
3.  **Separación de Pares:** Todos los pares (excepto el último) deben estar separados obligatoriamente por una coma `,`.
4.  **Valores Aceptados:** Un valor puede ser un `STRING`, un `NUMBER`, un `BOOLEAN` u otro Objeto Anidado.

**Restricciones Sintácticas:**
Una coma sobrante al final, una coma faltante entre pares, o un valor sin clave provocarán un Error Sintáctico, ya que violan la gramática de formato.

---

## 3. Reglas Semánticas (Servicio Semántico)
**Objetivo:** Validar la lógica del negocio determinista una vez que la estructura es correcta. Se ejecuta en el servicio Java.

**Reglas de Negocio Estrictas:**
1.  **Integridad Estructural:**
    *   La clave `"host"` debe existir siempre en la configuración.
    *   La clave `"puerto"` debe existir siempre en la configuración.
2.  **Restricción de Rango:**
    *   El valor asociado a `"puerto"` debe ser un número entero válido comprendido entre `1` y `65535` (ambos inclusive).
3.  **Consistencia Lógica (Conflictos de Estado):**
    *   Si la clave `"modo"` tiene el valor `"produccion"`, entonces la clave `"debug"` debe existir obligatoriamente y su valor debe ser estrictamente `false`. (No se permite depuración en entornos de producción).

**Restricciones Semánticas:**
Aunque el JSON esté perfectamente escrito (Léxico y Sintáctico correctos), si alguna de estas lógicas se rompe, se lanzará un Error Semántico.
