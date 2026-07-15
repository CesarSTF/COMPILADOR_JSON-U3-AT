import json
import urllib.request
import urllib.error
import time

# URL del punto de entrada del pipeline (Servicio Lexico)
URL = "http://localhost:8001/analizar"
HEALTH_URL = "http://localhost:8001/health"

TESTS = [
    {
        "nombre": "1. Configuracion Valida",
        "payload": """{
            "host": "0.0.0.0",
            "puerto": 8080,
            "modo": "desarrollo",
            "debug": true
        }""",
        "esperado_status": 200
    },
    {
        "nombre": "2. Error Lexico (caracter invalido)",
        "payload": """{
            "host": "localhost",
            "puerto": 8080,
            "modo": @invalido@
        }""",
        "esperado_status": 422
    },
    {
        "nombre": "3. Error Sintactico (falta coma)",
        "payload": """{
            "host": "localhost"
            "puerto": 8080
        }""",
        "esperado_status": 422
    },
    {
        "nombre": "4. Error Semantico (puerto fuera de rango)",
        "payload": """{
            "host": "localhost",
            "puerto": 999999,
            "modo": "desarrollo",
            "debug": true
        }""",
        "esperado_status": 422
    },
    {
        "nombre": "5. Error Semantico (debug=true en produccion)",
        "payload": """{
            "host": "localhost",
            "puerto": 8080,
            "modo": "produccion",
            "debug": true
        }""",
        "esperado_status": 422
    },
    {
        "nombre": "6. Error Semantico (falta host)",
        "payload": """{
            "puerto": 8080,
            "modo": "desarrollo",
            "debug": false
        }""",
        "esperado_status": 422
    }
]

def esperar_servidor(intentos=15):
    print(f"Esperando a que el pipeline este disponible ({HEALTH_URL})...")
    for i in range(intentos):
        try:
            req = urllib.request.Request(HEALTH_URL)
            with urllib.request.urlopen(req, timeout=2) as response:
                if response.status == 200:
                    print("[OK] Servidor disponible.\n")
                    return True
        except Exception:
            time.sleep(1)
    print("[ERROR] El servidor no respondio. Asegurate de ejecutar 'make up' antes de correr este script.")
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
            print(f"[ERROR] Fallo de conexion: {e}\n")
            continue

        if status == test['esperado_status']:
            print(f"[OK] PASO (Status {status})")
            exitosos += 1
            if status != 200:
                diagnostico = respuesta_json.get('diagnostico_ia', {})
                print(f"   [LLM] Diagnostico LLM: {diagnostico.get('reason', 'Sin razon devuelta')}")
                if 'solution_example' in diagnostico:
                    print(f"   [SOLUCION] Ejemplo de solucion:\n      {diagnostico['solution_example']}")
        else:
            print(f"[ERROR] FALLO (Esperaba {test['esperado_status']}, obtuvo {status})")
            print(f"   Respuesta: {json.dumps(respuesta_json, indent=2, ensure_ascii=False)}")
        
        print("")
        time.sleep(1)

    print(f"========================================")
    print(f"Resultado: {exitosos}/{totales} pruebas pasaron.")
    print(f"========================================")

if __name__ == "__main__":
    ejecutar_pruebas()
