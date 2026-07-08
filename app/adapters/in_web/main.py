from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from domain.sintactico.parser import ConfigParser
from domain.semantico.validador import ValidadorSemantico
from adapters.out_llm.llm_service import LLMStrategy

from fastapi.middleware.cors import CORSMiddleware

import yaml
import json

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


def detectar_y_transpilar(codigo_fuente: str) -> tuple:
    """
    Pre-procesador / Transpilador:
    Detecta si el texto recibido es JSON o YAML.
    Si es YAML, lo transpila internamente a JSON estricto para que
    el motor LALR (Lark) lo procese sin modificar la gramática formal.
    
    Retorna: (codigo_json, formato_original, error_yaml)
    """
    texto = codigo_fuente.strip()
    
    # Detección Léxica: Si empieza con '{', es JSON puro
    if texto.startswith('{'):
        return codigo_fuente, "JSON", None
    
    # Si no empieza con '{', asumimos YAML y transpilamos
    try:
        diccionario = yaml.safe_load(texto)
        
        # Validamos que el resultado sea un diccionario (objeto raíz)
        if not isinstance(diccionario, dict):
            return None, "YAML", {
                "tipo_error": "Sintactico",
                "mensaje": "El archivo YAML debe contener un objeto raíz con claves y valores (no una lista ni un valor suelto)."
            }
        
        # Transpilación exitosa: convertimos a JSON estricto
        codigo_json = json.dumps(diccionario)
        return codigo_json, "YAML", None
        
    except yaml.YAMLError as e:
        # Error de sintaxis YAML (mala indentación, caracteres inválidos, etc.)
        detalle = str(e)
        return None, "YAML", {
            "tipo_error": "Sintactico",
            "mensaje": f"Error de formato YAML: {detalle}"
        }


@app.post("/analizar")
async def analizar_configuracion(request: Request):
    """
    Endpoint principal de validación. 
    Recibe texto plano (código fuente JSON/YAML) y lo procesa a través de las 3 fases.
    Soporta detección automática del formato de entrada.
    """
    try:
        # Obtenemos el payload crudo
        body_bytes = await request.body()
        codigo_fuente = body_bytes.decode('utf-8')
    except Exception as e:
        raise HTTPException(status_code=400, detail="Error leyendo el cuerpo de la petición.")
    
    if not codigo_fuente.strip():
        return JSONResponse(status_code=400, content={"error": "El cuerpo de la petición no puede estar vacío."})

    # Fase 0: Pre-procesador / Transpilador (Detección JSON vs YAML)
    codigo_json, formato, err_yaml = detectar_y_transpilar(codigo_fuente)
    
    if err_yaml:
        # El archivo YAML estaba roto, le pedimos diagnóstico al LLM
        explicacion_llm = llm.analizar_errores_configuracion(codigo_fuente, err_yaml, formato)
        return JSONResponse(status_code=422, content={
            "fase_fallo": "Sintactico",
            "formato_detectado": formato,
            "diagnostico_ia": explicacion_llm,
            "detalle_tecnico": err_yaml
        })

    # Fase 1 y 2: Análisis Léxico y Sintáctico (Motor LALR / Lark)
    ast, err_sintactico = parser.parsear(codigo_json)
    
    if err_sintactico:
        # Hubo un fallo en Lark, activamos el LLM (Fase 3)
        explicacion_llm = llm.analizar_errores_configuracion(codigo_fuente, err_sintactico, formato)
        return JSONResponse(status_code=422, content={
            "fase_fallo": err_sintactico.get("tipo_error", "Sintactico/Lexico"),
            "formato_detectado": formato,
            "diagnostico_ia": explicacion_llm,
            "detalle_tecnico": err_sintactico
        })

    # Fase 2b: Validación Semántica
    config_dict, err_semantico = validador.validar(ast)
    
    if err_semantico:
        # Hubo un fallo en las reglas de negocio, activamos el LLM (Fase 3)
        explicacion_llm = llm.analizar_errores_configuracion(codigo_fuente, err_semantico, formato)
        return JSONResponse(status_code=422, content={
            "fase_fallo": "Semantico",
            "formato_detectado": formato,
            "diagnostico_ia": explicacion_llm,
            "detalle_tecnico": err_semantico
        })

    # Si todo pasa con éxito
    return JSONResponse(status_code=200, content={
        "mensaje": "La configuración es léxica, sintáctica y semánticamente válida.",
        "formato_detectado": formato,
        "configuracion_aprobada": config_dict
    })

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "Analizador Semantico API"}
