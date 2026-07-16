# FASE 2: Análisis Sintáctico (Parsing)

El microservicio Sintáctico es la segunda etapa del pipeline. Recibe el arreglo lineal de tokens generados por la Fase Léxica y verifica que la estructura de los mismos tenga sentido gramatical (jerarquía). Si la estructura es correcta, genera un **Árbol de Sintaxis Abstracta (AST)**.

## Implementación Técnica
* **Lenguaje:** Java 17
* **Método:** Parser Descendente Recursivo (RDP - *Recursive Descent Parser*). Cada regla de la gramática se implementa como una función recursiva.

## Gramática Libre de Contexto (GLC)
El analizador sintáctico verifica que los tokens cumplan estrictamente el siguiente conjunto de reglas de producción:

```text
inicio   -> objeto
objeto   -> '{' miembros '}' | '{' '}'
miembros -> par (',' par)*
par      -> T_STRING ':' valor
valor    -> T_STRING | T_NUMERO | T_BOOLEANO | objeto
```

### Explicación de las Reglas
1. **Inicio:** Todo código debe derivar en un único objeto principal.
2. **Objeto:** Debe estar delimitado por llaves (`LBRACE` y `RBRACE`). Puede estar vacío o contener miembros.
3. **Miembros:** Si hay varios elementos dentro de un objeto, deben separarse obligatoriamente por una coma (`COMMA`).
4. **Par:** La estructura básica es una **Clave** seguida de dos puntos (`COLON`) y un **Valor**. **IMPORTANTE:** La sintaxis exige que las claves sean *siempre* cadenas de texto (`T_STRING`).
5. **Valor:** Un valor puede ser un número (`T_NUMERO`), un booleano (`T_BOOLEANO`), una cadena de texto (`T_STRING`), o un nuevo objeto anidado (recursividad).

## Mecanismo de Diagnóstico (LLM)
Si los tokens no siguen este orden (por ejemplo, dos pares seguidos sin una coma en medio), el Parser Descendente lanza una excepción indicando qué token esperaba ver y qué token encontró.
* **Malicia del Prompt:** Dado que la falta de comas es el error sintáctico más común, el prompt que interactúa con **Ollama** tiene instrucciones explícitas para priorizar la sugerencia de una coma faltante cuando detecta una colisión de llaves o cadenas inesperadas.
