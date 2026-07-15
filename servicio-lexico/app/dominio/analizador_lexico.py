import os
from lark import Lark
from lark.exceptions import UnexpectedCharacters


# Ruta absoluta al archivo de la gramatica formal
DIRECTORIO_BASE = os.path.dirname(os.path.abspath(__file__))
RUTA_GRAMATICA = os.path.join(DIRECTORIO_BASE, 'gramatica.lark')


class AnalizadorLexico:
    """
    Dominio puro del Analizador Lexico.
    Utiliza el motor Lark para tokenizar el codigo fuente
    y detectar caracteres invalidos (errores lexicos).
    """

    def __init__(self):
        with open(RUTA_GRAMATICA, 'r', encoding='utf-8') as archivo:
            contenido_gramatica = archivo.read()

        # Inicializamos Lark en modo LALR para poder usar lex()
        self.parser = Lark(
            contenido_gramatica,
            start='inicio',
            parser='lalr',
            propagate_positions=True
        )

    def tokenizar(self, codigo_fuente: str):
        """
        Tokeniza el codigo fuente recibido.
        Retorna (lista_tokens, None) si es exitoso.
        Retorna (None, error_dict) si encuentra caracteres no validos.
        """
        try:
            iterador_tokens = self.parser.lex(codigo_fuente)
            lista_tokens = []

            for token in iterador_tokens:
                lista_tokens.append({
                    "tipo": str(token.type),
                    "valor": str(token),
                    "linea": token.line,
                    "columna": token.column
                })

            return lista_tokens, None

        except UnexpectedCharacters as error:
            error_lexico = {
                "tipo_error": "Lexico",
                "linea": error.line,
                "columna": error.column,
                "contexto": error.get_context(codigo_fuente),
                "mensaje": "Se encontraron caracteres no validos o no reconocidos."
            }
            return None, error_lexico

        except Exception as error:
            error_desconocido = {
                "tipo_error": "Desconocido",
                "mensaje": str(error)
            }
            return None, error_desconocido
