package com.compilador.lexico;

import com.compilador.lexico.dominio.AnalizadorLexico;
import com.compilador.lexico.adaptadores.entrada.ServidorWeb;
import com.compilador.lexico.adaptadores.salida.ClienteLLM;
import com.compilador.lexico.adaptadores.salida.ClienteSintactico;

/**
 * Clase principal del Servicio Lexico.
 * Inicializa los componentes de la arquitectura hexagonal
 * y arranca el servidor HTTP.
 */
public class Principal {

    public static void main(String[] argumentos) {
        // Configuracion desde variables de entorno
        String urlOllama = System.getenv("OLLAMA_HOST") != null
            ? System.getenv("OLLAMA_HOST")
            : "http://localhost:11434";
        String urlSintactico = System.getenv("URL_SERVICIO_SINTACTICO") != null
            ? System.getenv("URL_SERVICIO_SINTACTICO")
            : "http://localhost:8002";
        String modeloLlm = System.getenv("OLLAMA_MODELO") != null
            ? System.getenv("OLLAMA_MODELO")
            : "qwen2.5-coder:3b";
        int puerto = 8001;

        System.out.println("=== Servicio Lexico ===");
        System.out.println("Puerto: " + puerto);
        System.out.println("Ollama: " + urlOllama);
        System.out.println("Modelo: " + modeloLlm);
        System.out.println("Sintactico: " + urlSintactico);

        // Inyeccion de dependencias (Arquitectura Hexagonal)
        AnalizadorLexico analizador = new AnalizadorLexico();
        ClienteLLM clienteLlm = new ClienteLLM(urlOllama, modeloLlm);
        ClienteSintactico clienteSintactico = new ClienteSintactico(urlSintactico);
        ServidorWeb servidor = new ServidorWeb(analizador, clienteLlm, clienteSintactico);

        // Arrancar el servidor
        servidor.iniciar(puerto);
    }
}
