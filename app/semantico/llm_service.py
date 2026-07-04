import json
import ollama

class LLMStrategy:
    def __init__(self, modelo="qwen2.5-coder:3b"):
        self.modelo = modelo
        self.debug = True

    def analizar_errores_configuracion(self, codigo_fuente: str, error_detectado: dict) -> dict:
        prompt = f"""
Eres un experto en infraestructuras DevOps y soporte técnico.

Has recibido un código fuente de configuración y el reporte de un error técnico. Tu ÚNICO trabajo es traducir este error a un diagnóstico claro, amigable y accionable.

Reglas CRÍTICAS para tu diagnóstico:
1. PRIORIDAD ABSOLUTA: Identifica el "tipo_error".
2. SI EL ERROR ES "Sintactico" o "Lexico":
   - El fallo es puramente de formato de escritura (JSON inválido).
   - IGNORA las reglas de negocio (host, puerto, modo, debug).
   - "errParameter" DEBE ser ["formato_sintactico"].
   - Traduce los tokens de Lark ("COMMA", "RBRACE") a humano ("coma (,)", "llave de cierre (}})").
3. SI EL ERROR ES "Semantico":
   - El fallo viola reglas de negocio (puerto inválido, falta host, conflicto modo/debug).
   - "errParameter" DEBE ser la clave exacta (ej. ["puerto"]).
4. "solution_example" ESTÁ ESTRICTAMENTE PROHIBIDO dejarlo vacío, debe mostrar el JSON corregido.
5. Responde ÚNICAMENTE con JSON válido.

=== EJEMPLOS DE COMPORTAMIENTO ESPERADO (APRENDIZAJE EN CONTEXTO) ===
EJEMPLO 1 (Error Sintáctico por formato):
{{
  "hasError": true,
  "errParameter": ["formato_sintactico"],
  "reason": "Falta una coma (,) al final de la línea anterior para separar los elementos.",
  "solution_example": "'modo': 'produccion',\n  'debug': false"
}}

EJEMPLO 2 (Error Semántico de puerto fuera de rango):
{{
  "hasError": true,
  "errParameter": ["puerto"],
  "reason": "El puerto asignado está fuera del rango permitido. Debe ser un número entre 1 y 65535.",
  "solution_example": "'puerto': 8080"
}}

EJEMPLO 3 (Error Semántico por clave faltante obligatoria):
Si el reporte dice que la clave 'host' es obligatoria:
{{
  "hasError": true,
  "errParameter": ["host"],
  "reason": "La clave 'host' es obligatoria para la configuración y no se encontró en el archivo.",
  "solution_example": "{{\\n  'host': '127.0.0.1',\\n  'modo': 'produccion',\\n  'debug': false\\n}}"
}}
====================================================================
Archivo de configuración proporcionado:
{codigo_fuente}

Error técnico detectado por el analizador estricto:
{json.dumps(error_detectado, ensure_ascii=False, indent=2)}
"""
        try:
            respuesta = ollama.chat(model=self.modelo, messages=[
                {'role': 'user', 'content': prompt}
            ], format='json')
            contenido = respuesta['message']['content']
            
            if self.debug:
                print("\n[LLM] Respuesta cruda del modelo:", flush=True)
                try:
                    print(json.dumps(json.loads(contenido), indent=2, ensure_ascii=False), flush=True)
                except Exception:
                    print(contenido, flush=True)
            
            return json.loads(contenido)
        except Exception as e:
            return {
                "hasError": True,
                "errParameter": ["error_interno"],
                "reason": f"La respuesta del modelo falló. Detalle técnico: {str(e)}",
                "solution_example": "Revise manualmente la sintaxis de su archivo."
            }