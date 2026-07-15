import json
import os
import urllib.request
import urllib.error
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from dominio.analizador_sintactico import AnalizadorSintactico
from adaptadores.salida_llm import ClienteLLM


# URL del siguiente microservicio en la cadena (Servicio Semantico)
URL_SERVICIO_SEMANTICO = os.environ.get("URL_SERVICIO_SEMANTICO", "http://localhost:8003")

aplicacion = FastAPI(title="Servicio Sintactico - Analizador de Estructura")

# Configurar CORS
aplicacion.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

analizador = AnalizadorSintactico()
cliente_llm = ClienteLLM()


@aplicacion.post("/analizar")
async def analizar_sintactico(solicitud: Request):
    """
    Recibe el codigo fuente y los tokens del Servicio Lexico.
    Construye el AST y, si es exitoso, reenvia al Servicio Semantico.
    """
    try:
        cuerpo = await solicitud.json()
        codigo_fuente = cuerpo.get("codigo_fuente", "")
        tokens_recibidos = cuerpo.get("tokens", [])
    except Exception:
        return JSONResponse(
            status_code=400,
            content={"error": "Error leyendo el cuerpo de la peticion."}
        )

    if not codigo_fuente.strip():
        return JSONResponse(
            status_code=400,
            content={"error": "El codigo fuente no puede estar vacio."}
        )

    print(f"[SINTACTICO] Recibidos {len(tokens_recibidos)} tokens del Servicio Lexico.", flush=True)

    # Fase 2: Analisis Sintactico (Construccion del AST)
    ast_serializado, error_sintactico = analizador.construir_ast(codigo_fuente)

    if error_sintactico:
        # Error sintactico detectado: consultamos al LLM para diagnostico
        diagnostico_ia = cliente_llm.diagnosticar_error(codigo_fuente, error_sintactico)
        return JSONResponse(status_code=422, content={
            "exito": False,
            "fase_fallo": "Sintactico",
            "detalle_tecnico": error_sintactico,
            "diagnostico_ia": diagnostico_ia
        })

    # AST construido exitosamente: reenviamos al Servicio Semantico
    print(f"[SINTACTICO] AST construido exitosamente. Reenviando al Servicio Semantico.", flush=True)

    payload_semantico = json.dumps({
        "codigo_fuente": codigo_fuente,
        "ast": ast_serializado
    }).encode("utf-8")

    peticion_semantico = urllib.request.Request(
        f"{URL_SERVICIO_SEMANTICO}/analizar",
        data=payload_semantico,
        headers={"Content-Type": "application/json"},
        method="POST"
    )

    try:
        with urllib.request.urlopen(peticion_semantico, timeout=120) as respuesta:
            datos_respuesta = json.loads(respuesta.read().decode("utf-8"))
            return JSONResponse(
                status_code=respuesta.status,
                content=datos_respuesta
            )
    except urllib.error.HTTPError as error_http:
        datos_error = json.loads(error_http.read().decode("utf-8"))
        return JSONResponse(
            status_code=error_http.code,
            content=datos_error
        )
    except Exception as error:
        return JSONResponse(status_code=502, content={
            "exito": False,
            "fase_fallo": "Comunicacion",
            "error": f"No se pudo conectar con el Servicio Semantico: {str(error)}"
        })


@aplicacion.get("/health")
def verificar_salud():
    return {"estado": "ok", "servicio": "Analizador Sintactico"}
