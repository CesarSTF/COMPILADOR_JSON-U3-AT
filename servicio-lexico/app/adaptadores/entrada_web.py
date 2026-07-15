import json
import os
import urllib.request
import urllib.error
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from dominio.analizador_lexico import AnalizadorLexico
from adaptadores.salida_llm import ClienteLLM


# URL del siguiente microservicio en la cadena (Servicio Sintactico)
URL_SERVICIO_SINTACTICO = os.environ.get("URL_SERVICIO_SINTACTICO", "http://localhost:8002")

aplicacion = FastAPI(title="Servicio Lexico - Analizador de Tokens")

# Configurar CORS para permitir peticiones desde el frontend
aplicacion.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

analizador = AnalizadorLexico()
cliente_llm = ClienteLLM()


@aplicacion.post("/analizar")
async def analizar_lexico(solicitud: Request):
    """
    Punto de entrada del pipeline de compilacion.
    Recibe texto plano (codigo fuente JSON), lo tokeniza,
    y si es exitoso, reenvía al Servicio Sintactico.
    """
    try:
        cuerpo_bytes = await solicitud.body()
        codigo_fuente = cuerpo_bytes.decode("utf-8")
    except Exception:
        return JSONResponse(
            status_code=400,
            content={"error": "Error leyendo el cuerpo de la peticion."}
        )

    if not codigo_fuente.strip():
        return JSONResponse(
            status_code=400,
            content={"error": "El cuerpo de la peticion no puede estar vacio."}
        )

    # Fase 1: Analisis Lexico (Tokenizacion)
    lista_tokens, error_lexico = analizador.tokenizar(codigo_fuente)

    if error_lexico:
        # Error lexico detectado: consultamos al LLM para diagnostico
        diagnostico_ia = cliente_llm.diagnosticar_error(codigo_fuente, error_lexico)
        return JSONResponse(status_code=422, content={
            "exito": False,
            "fase_fallo": "Lexico",
            "detalle_tecnico": error_lexico,
            "diagnostico_ia": diagnostico_ia
        })

    # Tokenizacion exitosa: reenviamos al Servicio Sintactico
    print(f"[LEXICO] Tokenizacion exitosa. {len(lista_tokens)} tokens encontrados.", flush=True)
    print(f"[LEXICO] Reenviando al Servicio Sintactico: {URL_SERVICIO_SINTACTICO}", flush=True)

    payload_sintactico = json.dumps({
        "codigo_fuente": codigo_fuente,
        "tokens": lista_tokens
    }).encode("utf-8")

    peticion_sintactico = urllib.request.Request(
        f"{URL_SERVICIO_SINTACTICO}/analizar",
        data=payload_sintactico,
        headers={"Content-Type": "application/json"},
        method="POST"
    )

    try:
        with urllib.request.urlopen(peticion_sintactico, timeout=120) as respuesta:
            datos_respuesta = json.loads(respuesta.read().decode("utf-8"))
            return JSONResponse(
                status_code=respuesta.status,
                content=datos_respuesta
            )
    except urllib.error.HTTPError as error_http:
        # El Servicio Sintactico devolvio un error HTTP (ej. 422)
        datos_error = json.loads(error_http.read().decode("utf-8"))
        return JSONResponse(
            status_code=error_http.code,
            content=datos_error
        )
    except Exception as error:
        return JSONResponse(status_code=502, content={
            "exito": False,
            "fase_fallo": "Comunicacion",
            "error": f"No se pudo conectar con el Servicio Sintactico: {str(error)}"
        })


@aplicacion.get("/health")
def verificar_salud():
    return {"estado": "ok", "servicio": "Analizador Lexico"}
