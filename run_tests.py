import json
import urllib.request
import urllib.error
import time

URL = "http://Grupo4/analizar"
HEALTH_URL = "http://Grupo4/health"

TESTS = [
    {
        "nombre": "1. Configuración Válida",
        "payload": """{
            "host": "0.0.0.0",
            "puerto": 8080,
            "modo": "desarrollo",
            "debug": true,
            "database": {
                "user": "admin",
                "timeout": 30
            }
        }""",
        "esperado_status": 200
    },
    {
        "nombre": "2. Error Léxico / Sintáctico (Falta coma y comillas)",
        "payload": """{
            "host": "0.0.0.0"
            puerto: 8080
        }""",
        "esperado_status": 422
    },
    {
        "nombre": "3. Error Semántico 1: Falta 'puerto' (Integridad)",
        "payload": """{
            "host": "192.168.1.1",
            "modo": "producción"
        }""",
        "esperado_status": 422
    },
    {
        "nombre": "4. Error Semántico 2: Puerto fuera de rango",
        "payload": """{
            "host": "localhost",
            "puerto": 70000,
            "modo": "desarrollo"
        }""",
        "esperado_status": 422
    },
    {
        "nombre": "5. Error Semántico 3: Consistencia Lógica (Producción con debug=true)",
        "payload": """{
            "host": "localhost",
            "puerto": 443,
            "modo": "producción",
            "debug": true
        }""",
        "esperado_status": 422
    }
]

def esperar_servidor():
    print("⏳ Esperando a que el servidor esté disponible en Grupo4...")
    for _ in range(15):
        try:
            req = urllib.request.Request(HEALTH_URL)
            with urllib.request.urlopen(req, timeout=2) as response:
                if response.status == 200:
                    print("✅ Servidor disponible.\n")
                    return True
        except Exception:
            time.sleep(1)
    print("❌ El servidor no respondió. Asegúrate de ejecutar 'make up' antes de correr este script.")
    return False

def ejecutar_pruebas():
    if not esperar_servidor():
        return

    exitosos = 0
    totales = len(TESTS)

    for i, test in enumerate(TESTS, 1):
        print(f"--- Ejecutando Prueba {i}/{totales}: {test['nombre']} ---")
        
        req = urllib.request.Request(
            URL, 
            data=test['payload'].encode('utf-8'),
            headers={'Content-Type': 'application/json'},
            method='POST'
        )
        
        status = None
        respuesta_json = None
        
        try:
            with urllib.request.urlopen(req) as response:
                status = response.status
                respuesta_json = json.loads(response.read().decode('utf-8'))
        except urllib.error.HTTPError as e:
            status = e.code
            respuesta_json = json.loads(e.read().decode('utf-8'))
        except Exception as e:
            print(f"❌ Fallo de conexión: {e}\n")
            continue

        if status == test['esperado_status']:
            print(f"✅ PASÓ (Status {status})")
            exitosos += 1
            if status != 200:
                diagnostico = respuesta_json.get('diagnostico_ia', {})
                print(f"   🤖 Diagnóstico LLM: {diagnostico.get('reason', 'Sin razón devuelta')}")
                if 'solution_example' in diagnostico:
                    print(f"   💡 Ejemplo de solución:\n      {diagnostico['solution_example']}")
        else:
            print(f"❌ FALLÓ (Esperaba {test['esperado_status']}, obtuvo {status})")
            print(f"   Respuesta: {json.dumps(respuesta_json, indent=2, ensure_ascii=False)}")
        
        print("")
        time.sleep(1) # Pequeña pausa para no saturar al LLM en las peticiones seguidas

    print(f"=== Resultados: {exitosos}/{totales} pruebas exitosas ===")

if __name__ == "__main__":
    ejecutar_pruebas()
