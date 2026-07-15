import os
from lark import Lark, Tree, Token
from lark.exceptions import UnexpectedToken


# Ruta absoluta al archivo de la gramatica formal
DIRECTORIO_BASE = os.path.dirname(os.path.abspath(__file__))
RUTA_GRAMATICA = os.path.join(DIRECTORIO_BASE, 'gramatica.lark')


class AnalizadorSintactico:
    """
    Dominio puro del Analizador Sintactico.
    Utiliza el motor Lark para construir el Arbol Sintactico Abstracto (AST)
    a partir del codigo fuente y detectar errores de estructura gramatical.
    """

    def __init__(self):
        with open(RUTA_GRAMATICA, 'r', encoding='utf-8') as archivo:
            contenido_gramatica = archivo.read()

        self.parser = Lark(
            contenido_gramatica,
            start='inicio',
            parser='lalr',
            propagate_positions=True
        )

    def construir_ast(self, codigo_fuente: str):
        """
        Construye el AST a partir del codigo fuente.
        Retorna (ast_serializado, None) si es exitoso.
        Retorna (None, error_dict) si hay un error sintactico.
        """
        try:
            arbol = self.parser.parse(codigo_fuente)
            ast_serializado = self._serializar_arbol(arbol)
            return ast_serializado, None

        except UnexpectedToken as error:
            error_sintactico = {
                "tipo_error": "Sintactico",
                "linea": error.line,
                "columna": error.column,
                "token_inesperado": str(error.token),
                "esperado": [str(t) for t in error.expected],
                "contexto": error.get_context(codigo_fuente),
                "mensaje": f"Se esperaba uno de {list(error.expected)}, pero se encontro '{error.token}'"
            }
            return None, error_sintactico

        except Exception as error:
            error_desconocido = {
                "tipo_error": "Desconocido",
                "mensaje": str(error)
            }
            return None, error_desconocido

    def _serializar_arbol(self, nodo):
        """
        Convierte el arbol de Lark (Tree/Token) en un diccionario
        serializable a JSON para enviarlo al Servicio Semantico.
        """
        if isinstance(nodo, Tree):
            return {
                "tipo": str(nodo.data),
                "hijos": [self._serializar_arbol(hijo) for hijo in nodo.children]
            }
        elif isinstance(nodo, Token):
            return {
                "tipo_token": str(nodo.type),
                "valor": str(nodo),
                "linea": nodo.line,
                "columna": nodo.column
            }
        else:
            return {"valor": str(nodo)}
