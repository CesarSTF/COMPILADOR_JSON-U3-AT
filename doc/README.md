# Manual del Compilador DSL Distribuido

Esta carpeta contiene la documentación técnica de las tres fases del compilador y su integración con el servicio de IA.

## Patrón Arquitectónico: Cadena de Responsabilidad Distribuida

El compilador implementa una **Cadena de Responsabilidad Distribuida** orquestada desde el Frontend (API Gateway). Cada microservicio es un **eslabón puro e independiente** que solo conoce su dominio.

### Flujo de la Cadena
```
Frontend (Orquestador)
    │
    ├──→ [1] Servicio Léxico  (8001)  → Tokens o Error
    │         Si error → desviar al Servicio LLM
    │
    ├──→ [2] Servicio Sintáctico (8002) → AST o Error
    │         Si error → desviar al Servicio LLM
    │
    ├──→ [3] Servicio Semántico (8003) → Validación o Error
    │         Si error → desviar al Servicio LLM
    │
    └──→ [4] Servicio LLM (8004) → Diagnóstico IA (solo si hay error)
```

### Principios Clave
1. **Independencia total**: Ningún eslabón conoce al siguiente. No hay llamadas directas entre servicios.
2. **Orquestación centralizada**: El Frontend decide qué eslabón llamar y cuándo desviar al LLM.
3. **Fail-Fast**: Si un eslabón falla, la cadena se detiene inmediatamente.
4. **LLM como servicio transversal**: La IA no está dentro de la lógica de negocio; es un adaptador común externo.

## Índices de Documentación

1. **[Fase Léxica (Alfabeto y Regex)](01_alfabeto_lexico.md)** — Autómata finito y expresiones regulares.
2. **[Fase Sintáctica (Gramática GLC)](02_gramatica_sintactica.md)** — Parser Descendente Recursivo y AST.
3. **[Fase Semántica (Reglas de Negocio)](03_reglas_semanticas.md)** — Validación lógica bajo Arquitectura Hexagonal.
