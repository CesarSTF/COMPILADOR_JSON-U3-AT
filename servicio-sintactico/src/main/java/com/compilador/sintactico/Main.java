package com.compilador.sintactico;

import com.compilador.sintactico.dominio.AnalizadorSintactico;
import com.compilador.sintactico.adaptadores.entrada.ServidorWeb;

/**
 * Clase principal del Servicio Sintactico.
 * Inicializa los componentes de la arquitectura hexagonal
 * y arranca el servidor HTTP.
 *
 * Patron: Cadena de Responsabilidad Distribuida.
 * Este servicio es un eslabon PURO: solo parsea tokens y responde.
 * No conoce al Semantico ni al LLM. El Orquestador (Frontend)
 * decide que hacer con el resultado.
 */
public class Main {

    public static void main(String[] argumentos) {
        int puerto = 8002;

        System.out.println("=== Servicio Sintactico (Eslabon 2) ===");
        System.out.println("Puerto: " + puerto);

        // Inyeccion de dependencias (Arquitectura Hexagonal)
        AnalizadorSintactico analizador = new AnalizadorSintactico();
        ServidorWeb servidor = new ServidorWeb(analizador);

        // Arrancar el servidor
        servidor.iniciar(puerto);
    }
}
