# FASE 3: Análisis Semántico (Reglas de Negocio)

El microservicio Semántico es la etapa final del compilador. Recibe el **Árbol de Sintaxis Abstracta (AST)** garantizado por la Fase Sintáctica y evalúa el significado, lógica y contexto de los datos configurados.

## Implementación Técnica
* **Lenguaje:** Java 17
* **Arquitectura:** Arquitectura Hexagonal. El dominio semántico está puramente aislado y no conoce nada de HTTP ni de bases de datos.
* **Método:** Evaluación determinista sobre un AST mapeado a Objetos de Transferencia de Datos (DTO).

## Reglas de Negocio Estrictas
A diferencia de las fases anteriores, que evalúan la forma, esta fase evalúa el fondo. Se aplican las siguientes validaciones en cascada:

### Regla 1: Integridad Estructural (Presencia de campos)
Para que una configuración de servidor sea viable, debe contener obligatoriamente ciertos parámetros.
* **Validación:** El AST debe contener obligatoriamente la clave `"host"`.
* **Validación:** El AST debe contener obligatoriamente la clave `"puerto"`.

### Regla 2: Restricción de Rango (Límites físicos)
Un número sintácticamente válido puede ser lógicamente inválido para el dominio de un servidor.
* **Validación:** El valor asignado a la clave `"puerto"` debe estar en el rango de los puertos TCP válidos: **1 al 65535**.

### Regla 3: Consistencia Lógica (Conflictos de estado)
Dos parámetros, aunque individualmente correctos, pueden crear una contradicción al evaluarse en conjunto.
* **Validación:** Si el atributo `"modo"` tiene el valor `"produccion"`, entonces el atributo `"debug"` debe ser **estrictamente `false`**. (No se permite imprimir logs de depuración en un entorno productivo).

## Mecanismo de Diagnóstico (LLM)
Al ser reglas altamente deterministas, las fallas son específicas. El prompt enviado a **Ollama** recibe la regla exacta que fue violada. La restricción del LLM aquí es absoluta: se le prohíbe hablar de errores de escritura (sintaxis) y se le obliga a formular su diagnóstico utilizando *únicamente* la regla de negocio proporcionada por el evaluador.
