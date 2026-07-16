package com.compilador.sintactico;

import com.compilador.sintactico.dominio.AnalizadorSintactico;
import com.compilador.sintactico.adaptadores.entrada.ServidorWeb;
import com.compilador.sintactico.adaptadores.salida.ClienteLLM;
import com.compilador.sintactico.adaptadores.salida.ClienteSemantico;

/**
 * Clase principal del Servicio Sintactico.
 * Inicializa los componentes de la arquitectura hexagonal
 * y arranca el servidor HTTP.
 */
public class Principal {

    public static void main(String[] argumentos) {
        // Configuracion desde variables de entorno
        String urlOllama = System.getenv("OLLAMA_HOST") != null
            ? System.getenv("OLLAMA_HOST")
            : "http://localhost:11434";
        String urlSemantico = System.getenv("URL_SERVICIO_SEMANTICO") != null
            ? System.getenv("URL_SERVICIO_SEMANTICO")
            : "http://localhost:8003";
        String modeloLlm = System.getenv("OLLAMA_MODELO") != null
            ? System.getenv("OLLAMA_MODELO")
            : "qwen2.5-coder:3b";
        int puerto = 8002;

        System.out.println("=== Servicio Sintactico ===");
        System.out.println("Puerto: " + puerto);
        System.out.println("Ollama: " + urlOllama);
        System.out.println("Modelo: " + modeloLlm);
        System.out.println("Semantico: " + urlSemantico);

        // Inyeccion de dependencias (Arquitectura Hexagonal)
        AnalizadorSintactico analizador = new AnalizadorSintactico();
        ClienteLLM clienteLlm = new ClienteLLM(urlOllama, modeloLlm);
        ClienteSemantico clienteSemantico = new ClienteSemantico(urlSemantico);
        ServidorWeb servidor = new ServidorWeb(analizador, clienteLlm, clienteSemantico);

        // Arrancar el servidor
        servidor.iniciar(puerto);
    }
}
