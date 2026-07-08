from lark import Transformer, Tree

class ASTaDiccionario(Transformer):
    """
    Transforma el Árbol Sintáctico Abstracto (AST) de Lark
    en un diccionario de Python para facilitar la validación semántica.
    """
    def inicio(self, items):
        return items[0]
        
    def objeto(self, items):
        # items puede estar vacío {} o contener una lista de pares de 'miembros'
        if not items:
            return {}
        # El primer elemento de items corresponde a 'miembros', que es un diccionario
        return items[0]
        
    def miembros(self, items):
        # items es una lista de pares (clave, valor)
        return dict(items)
        
    def par(self, items):
        # items[0] es la clave (T_STRING), items[1] es el valor
        clave = str(items[0]).strip('"')
        valor = items[1]
        return (clave, valor)
        
    def valor(self, items):
        # items[0] puede ser Token (T_STRING, T_NUMERO, T_BOOLEANO) o un dict (objeto)
        val = items[0]
        if isinstance(val, dict):
            return val
        
        tipo = val.type
        texto = str(val)
        
        if tipo == 'T_STRING':
            return texto.strip('"')
        elif tipo == 'T_NUMERO':
            return int(texto)
        elif tipo == 'T_BOOLEANO':
            return True if texto == 'true' else False
        return texto

class ValidadorSemantico:
    def __init__(self):
        self.transformer = ASTaDiccionario()

    def validar(self, ast: Tree):
        """
        Recibe el AST validado sintácticamente.
        Lo transforma a diccionario y aplica las 3 reglas deterministas.
        Retorna (config_dict, None) si es válido.
        Retorna (None, error_dict) si viola reglas semánticas.
        """
        # 1. Transformar AST a Diccionario
        try:
            config = self.transformer.transform(ast)
        except Exception as e:
            return None, {
                "tipo_error": "Transformacion",
                "mensaje": f"Error al procesar el AST: {str(e)}"
            }

        # 2. Validar Reglas Semánticas
        
        # Regla 1: Integridad Estructural (host y puerto obligatorios)
        if 'host' not in config:
            return None, {
                "tipo_error": "Semantico",
                "clave": "host",
                "mensaje": "La clave 'host' es obligatoria en la configuración."
            }
        
        if 'puerto' not in config:
            return None, {
                "tipo_error": "Semantico",
                "clave": "puerto",
                "mensaje": "La clave 'puerto' es obligatoria en la configuración."
            }

        # Regla 2: Restricción de Rango (1 <= puerto <= 65535)
        puerto = config.get('puerto')
        if not isinstance(puerto, int) or puerto < 1 or puerto > 65535:
            return None, {
                "tipo_error": "Semantico",
                "clave": "puerto",
                "valor_encontrado": puerto,
                "mensaje": "El 'puerto' debe ser un número entero entre 1 y 65535."
            }

        # Regla 3: Consistencia Lógica (modo="producción" -> debug=false)
        modo = config.get('modo')
        debug = config.get('debug')
        
        if modo == 'producción' or modo == 'produccion':
            if debug is not False:  # Si es True, o si no está definido (aunque la regla dice estrictamente false)
                return None, {
                    "tipo_error": "Semantico",
                    "clave": "debug",
                    "modo": modo,
                    "debug": debug,
                    "mensaje": "En modo 'producción', la variable 'debug' debe ser obligatoriamente 'false'."
                }

        # Todo es válido
        return config, None
