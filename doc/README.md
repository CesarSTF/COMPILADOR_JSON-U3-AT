# Manual del Compilador DSL Distribuido

Esta carpeta contiene la documentación técnica pura y estricta de las tres fases del compilador. Es la base teórica que sustenta el comportamiento del código y que se utiliza para anclar (Grounding) al LLM local (Ollama) evitando alucinaciones.

## Pipeline en Cascada
El proyecto fue refactorizado a una arquitectura de microservicios 100% Java (Multi-stage Docker builds), donde el análisis fluye en cascada mediante peticiones HTTP. Si una fase detecta un error, el sistema aplica un patrón de **Fail-Fast (Fallo Rápido)** y no avanza a la siguiente.

### Índices de Documentación

1. **[Fase Léxica (Alfabeto y Regex)](01_alfabeto_lexico.md)**
   Documentación del autómata finito y las expresiones regulares que validan los caracteres permitidos.
2. **[Fase Sintáctica (Gramática GLC)](02_gramatica_sintactica.md)**
   Documentación del Parser Descendente Recursivo que asegura la jerarquía y estructura del lenguaje y construye el AST.
3. **[Fase Semántica (Reglas de Negocio)](03_reglas_semanticas.md)**
   Documentación de la lógica de evaluación implementada bajo los principios de Arquitectura Hexagonal.
