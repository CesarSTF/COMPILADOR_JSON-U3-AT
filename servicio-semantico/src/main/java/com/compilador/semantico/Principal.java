package com.compilador.semantico;

import com.compilador.semantico.dominio.ValidadorSemantico;
import com.compilador.semantico.adaptadores.entrada.ServidorWeb;
import com.compilador.semantico.adaptadores.salida.ClienteLLM;

/**
 * Clase principal del Servicio Semantico.
 * Inicializa los componentes de la arquitectura hexagonal
 * y arranca el servidor HTTP.
 */
public class Principal {

    public static void main(String[] argumentos) {
        // Configuracion desde variables de entorno
        String urlOllama = System.getenv("OLLAMA_HOST") != null
            ? System.getenv("OLLAMA_HOST")
            : "http://localhost:11434";
        String modeloLlm = "qwen2.5-coder:3b";
        int puerto = 8003;

        System.out.println("=== Servicio Semantico ===");
        System.out.println("Puerto: " + puerto);
        System.out.println("Ollama: " + urlOllama);
        System.out.println("Modelo: " + modeloLlm);

        // Inyeccion de dependencias (Arquitectura Hexagonal)
        ValidadorSemantico validador = new ValidadorSemantico();
        ClienteLLM clienteLlm = new ClienteLLM(urlOllama, modeloLlm);
        ServidorWeb servidor = new ServidorWeb(validador, clienteLlm);

        // Arrancar el servidor
        servidor.iniciar(puerto);
    }
}
