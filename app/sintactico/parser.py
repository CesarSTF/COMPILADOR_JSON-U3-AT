import os
from lark import Lark
from lark.exceptions import UnexpectedInput, UnexpectedToken, UnexpectedCharacters

# Obtener la ruta absoluta a la gramática
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRAMATICA_PATH = os.path.join(BASE_DIR, 'lexico', 'gramatica.lark')

class ConfigParser:
    def __init__(self):
        with open(GRAMATICA_PATH, 'r', encoding='utf-8') as file:
            self.grammar = file.read()
        
        # Inicializamos Lark con la gramática. 
        # propagate_positions=True es útil para obtener línea y columna en los errores
        self.parser = Lark(self.grammar, start='inicio', parser='lalr', propagate_positions=True)

    def parsear(self, contenido: str):
        """
        Intenta parsear el código fuente (archivo de configuración).
        Retorna (AST, None) si es exitoso.
        Retorna (None, error_dict) si falla, estructurando el error para el LLM.
        """
        try:
            ast = self.parser.parse(contenido)
            return ast, None
        except UnexpectedCharacters as e:
            error_msg = {
                "tipo_error": "Lexico",
                "linea": e.line,
                "columna": e.column,
                "contexto": e.get_context(contenido),
                "mensaje": "Se encontraron caracteres no válidos o no reconocidos."
            }
            return None, error_msg
        except UnexpectedToken as e:
            error_msg = {
                "tipo_error": "Sintactico",
                "linea": e.line,
                "columna": e.column,
                "token_inesperado": e.token.value,
                "esperado": [t for t in e.expected],
                "contexto": e.get_context(contenido),
                "mensaje": f"Se esperaba uno de {e.expected}, pero se encontró '{e.token.value}'"
            }
            return None, error_msg
        except Exception as e:
            error_msg = {
                "tipo_error": "Desconocido",
                "mensaje": str(e)
            }
            return None, error_msg
