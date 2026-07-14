from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from domain.sintactico.parser import ConfigParser
from domain.semantico.validador import ValidadorSemantico
from adapters.out_llm.llm_service import LLMStrategy

from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Analizador Semántico de Configuración")

# Configurar CORS para permitir peticiones desde el frontend de Vite
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Permite todos los orígenes en desarrollo
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
parser = ConfigParser()
validador = ValidadorSemantico()
llm = LLMStrategy(modelo="qwen2.5-coder:3b")

@app.post("/analizar")
async def analizar_configuracion(request: Request):
    """
    Endpoint principal de validación. 
    Recibe texto plano (código fuente JSON) y lo procesa a través de las 3 fases.
    """
    try:
        # Obtenemos el payload crudo
        body_bytes = await request.body()
        codigo_fuente = body_bytes.decode('utf-8')
    except Exception as e:
        raise HTTPException(status_code=400, detail="Error leyendo el cuerpo de la petición.")
    
    if not codigo_fuente.strip():
        return JSONResponse(status_code=400, content={"error": "El cuerpo de la petición no puede estar vacío."})

    # Fase 1 y 2: Análisis Léxico y Sintáctico
    ast, err_sintactico = parser.parsear(codigo_fuente)
    
    if err_sintactico:
        # Hubo un fallo en Lark, activamos el LLM (Fase 3)
        explicacion_llm = llm.analizar_errores_configuracion(codigo_fuente, err_sintactico)
        return JSONResponse(status_code=422, content={
            "fase_fallo": err_sintactico.get("tipo_error", "Sintactico/Lexico"),
            "diagnostico_ia": explicacion_llm,
            "detalle_tecnico": err_sintactico
        })

    # Fase 2b: Validación Semántica
    config_dict, err_semantico = validador.validar(ast)
    
    if err_semantico:
        # Hubo un fallo en las reglas de negocio, activamos el LLM (Fase 3)
        explicacion_llm = llm.analizar_errores_configuracion(codigo_fuente, err_semantico)
        return JSONResponse(status_code=422, content={
            "fase_fallo": "Semantico",
            "diagnostico_ia": explicacion_llm,
            "detalle_tecnico": err_semantico
        })

    # Si todo pasa con éxito
    return JSONResponse(status_code=200, content={
        "mensaje": "La configuración es léxica, sintáctica y semánticamente válida.",
        "configuracion_aprobada": config_dict
    })

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "Analizador Semantico API"}
