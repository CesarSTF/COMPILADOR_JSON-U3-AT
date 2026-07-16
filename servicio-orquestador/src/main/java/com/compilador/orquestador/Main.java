package com.compilador.orquestador;

import com.compilador.orquestador.dominio.CadenaCompilacion;
import com.compilador.orquestador.adaptadores.entrada.ServidorWeb;
import com.compilador.orquestador.adaptadores.salida.ClienteEslabon;
import com.compilador.orquestador.adaptadores.salida.ClienteOllama;

/**
 * Clase principal del Servicio Orquestador.
 * Gestiona la Cadena de Responsabilidad Distribuida.
 *
 * Es el unico servicio que conoce a los demas. Los eslabones
 * (Lexico, Sintactico, Semantico) son 100% independientes.
 * El LLM (Ollama) es externo, se accede via .env.
 */
public class Main {

    public static void main(String[] argumentos) {
        // URLs de los eslabones (internas de Docker)
        String urlLexico = System.getenv("URL_SERVICIO_LEXICO") != null
            ? System.getenv("URL_SERVICIO_LEXICO")
            : "http://localhost:8001";
        String urlSintactico = System.getenv("URL_SERVICIO_SINTACTICO") != null
            ? System.getenv("URL_SERVICIO_SINTACTICO")
            : "http://localhost:8002";
        String urlSemantico = System.getenv("URL_SERVICIO_SEMANTICO") != null
            ? System.getenv("URL_SERVICIO_SEMANTICO")
            : "http://localhost:8003";

        // LLM externo (Ollama via .env)
        String urlOllama = System.getenv("OLLAMA_HOST") != null
            ? System.getenv("OLLAMA_HOST")
            : "http://localhost:11434";
        String modeloLlm = System.getenv("OLLAMA_MODELO") != null
            ? System.getenv("OLLAMA_MODELO")
            : "qwen2.5-coder:3b";

        int puerto = 8000;

        System.out.println("=== Servicio Orquestador ===");
        System.out.println("Puerto: " + puerto);
        System.out.println("Lexico: " + urlLexico);
        System.out.println("Sintactico: " + urlSintactico);
        System.out.println("Semantico: " + urlSemantico);
        System.out.println("Ollama: " + urlOllama);
        System.out.println("Modelo: " + modeloLlm);

        // Adaptadores de salida (clientes HTTP)
        ClienteEslabon clienteLexico = new ClienteEslabon(urlLexico + "/analizar");
        ClienteEslabon clienteSintactico = new ClienteEslabon(urlSintactico + "/analizar");
        ClienteEslabon clienteSemantico = new ClienteEslabon(urlSemantico + "/analizar");
        ClienteOllama clienteOllama = new ClienteOllama(urlOllama, modeloLlm);

        // Dominio
        CadenaCompilacion cadena = new CadenaCompilacion();

        // Adaptador de entrada
        ServidorWeb servidor = new ServidorWeb(
            cadena, clienteLexico, clienteSintactico, clienteSemantico, clienteOllama
        );

        servidor.iniciar(puerto);
    }
}
