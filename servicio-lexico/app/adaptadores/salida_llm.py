import json
import os
import urllib.request
import urllib.error


# URL del servicio Ollama (configurable por variable de entorno)
OLLAMA_HOST = os.environ.get("OLLAMA_HOST", "http://localhost:11434")


class ClienteLLM:
    """
    Adaptador de salida: cliente HTTP hacia el servicio Ollama.
    Se encarga de enviar errores detectados al LLM para obtener
    un diagnostico amigable y una solucion sugerida.
    """

    def __init__(self, modelo: str = "qwen2.5-coder:3b"):
        self.modelo = modelo
        self.url_api = f"{OLLAMA_HOST}/api/chat"

    def diagnosticar_error(self, codigo_fuente: str, error_detectado: dict) -> dict:
        """
        Envia el error al LLM y retorna un diagnostico estructurado.
        """
        contexto = error_detectado.get("contexto", "")
        mensaje = error_detectado.get("mensaje", "")

        prompt = f"""
Eres un asistente estricto. Tu unica tarea es explicar un error LEXICO (caracteres invalidos).

REGLAS DEL ANALIZADOR LEXICO (Gramatica estricta):
El analizador reconoce EXCLUSIVAMENTE los siguientes tokens y lexemas:
1. Simbolos Terminales (Estructurales):
   - LBRACE: Llave de apertura '{{'
   - RBRACE: Llave de cierre '}}'
   - COLON: Dos puntos ':'
   - COMMA: Coma ','
2. Simbolos Terminales (Valores):
   - T_STRING: Cadenas de texto encerradas ESTRICTAMENTE entre comillas dobles (ej. "texto").
   - T_NUMERO: Secuencias de digitos numericos del 0 al 9 (ej. 8080).
   - T_BOOLEANO: Las palabras exactas 'true' o 'false' (sin comillas).
3. Espacios (WS): Espacios, tabs y saltos de linea son ignorados.

Cualquier lexema (caracter o palabra) que no encaje en las definiciones anteriores es considerado un TOKEN INVALIDO (ej. arrobas, palabras sin comillas dobles, comillas simples, etc).

ERROR DETECTADO POR EL COMPILADOR:
Contexto exacto del error:
{contexto}
Detalle: {mensaje}

CODIGO FUENTE ORIGINAL (Para referencia de la solucion):
{codigo_fuente}

INSTRUCCIONES:
1. Explica brevemente que caracter extraño o invalido se encontro en el contexto dado que no pertenece al alfabeto permitido. NO inventes otras reglas.
2. En "solution_example", devuelve el CODIGO FUENTE ORIGINAL completo pero corrigiendo la linea que tiene el error.
3. Responde estrictamente en este formato JSON (sin texto adicional):
{{
  "hasError": true,
  "errParameter": ["formato_lexico"],
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
