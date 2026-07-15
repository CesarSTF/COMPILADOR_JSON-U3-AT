import json
import os
import urllib.request
import urllib.error


# URL del servicio Ollama (configurable por variable de entorno)
OLLAMA_HOST = os.environ.get("OLLAMA_HOST", "http://localhost:11434")


class ClienteLLM:
    """
    Adaptador de salida: cliente HTTP hacia el servicio Ollama.
    Se encarga de enviar errores sintacticos al LLM para obtener
    un diagnostico amigable y una solucion sugerida.
    """

    def __init__(self, modelo: str = "qwen2.5-coder:3b"):
        self.modelo = modelo
        self.url_api = f"{OLLAMA_HOST}/api/chat"

    def diagnosticar_error(self, codigo_fuente: str, error_detectado: dict) -> dict:
        """
        Envia el error al LLM y retorna un diagnostico estructurado.
        """
        esperados = ", ".join(error_detectado.get("esperado", []))
        encontrado = error_detectado.get("token_inesperado", "")
        contexto = error_detectado.get("contexto", "")

        prompt = f"""
Eres un asistente estricto. Tu unica tarea es explicar un error SINTACTICO (de estructura).

REGLAS DEL ANALIZADOR SINTACTICO (Gramatica permitida):
- Estructura JSON: Todo debe estar dentro de llaves {{ }}
- Los elementos son pares de "clave": valor
- Los pares se separan estrictamente por comas (,)

ERROR DETECTADO POR EL COMPILADOR:
Contexto exacto del error:
{contexto}
Problema: Se esperaba uno de los siguientes simbolos/tokens [{esperados}], pero se encontro '{encontrado}'.

CODIGO FUENTE ORIGINAL (Para referencia de la solucion):
{codigo_fuente}

INSTRUCCIONES:
1. TRADUCCION: 'RBRACE' = llave de cierre '}}', 'COMMA' = coma ','. 
2. Si el problema dice que se esperaba 'RBRACE' o 'COMMA', el error real es casi seguro que falta una coma (,) para separar los elementos. ENFOCATE SOLO EN LA FALTA DE COMA, omite lo de la llave de cierre.
3. Explica brevemente el error y qué simbolo deberia ir en su lugar. NO inventes otras reglas.
4. En "solution_example", devuelve el CODIGO FUENTE ORIGINAL completo pero aplicando la correccion de la sintaxis.
5. Responde estrictamente en este formato JSON (sin texto adicional):
{{
  "hasError": true,
  "errParameter": ["formato_sintactico"],
  "reason": "<tu_explicacion_amigable>",
  "solution_example": "<codigo_fuente_completo_corregido>"
}}
"""

        cuerpo_peticion = json.dumps({
            "model": self.modelo,
            "messages": [{"role": "user", "content": prompt}],
            "format": "json",
            "stream": False
        }).encode("utf-8")

        solicitud = urllib.request.Request(
            self.url_api,
            data=cuerpo_peticion,
            headers={"Content-Type": "application/json"},
            method="POST"
        )

        try:
            with urllib.request.urlopen(solicitud, timeout=60) as respuesta:
                datos_respuesta = json.loads(respuesta.read().decode("utf-8"))
                contenido = datos_respuesta["message"]["content"]

                print(f"\n[LLM] Respuesta cruda del modelo:", flush=True)
                try:
                    print(json.dumps(json.loads(contenido), indent=2, ensure_ascii=False), flush=True)
                except Exception:
                    print(contenido, flush=True)

                return json.loads(contenido)

        except Exception as error:
            return {
                "hasError": True,
                "errParameter": ["error_interno"],
                "reason": f"La respuesta del modelo fallo. Detalle tecnico: {str(error)}",
                "solution_example": "Revise manualmente la sintaxis de su archivo."
            }
