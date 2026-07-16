package com.compilador.lexico;

import com.compilador.lexico.dominio.AnalizadorLexico;
import com.compilador.lexico.adaptadores.entrada.ServidorWeb;

/**
 * Clase principal del Servicio Lexico.
 * Inicializa los componentes de la arquitectura hexagonal
 * y arranca el servidor HTTP.
 *
 * Patron: Cadena de Responsabilidad Distribuida.
 * Este servicio es un eslabon PURO: solo tokeniza y responde.
 * No conoce al Sintactico ni al LLM. El Orquestador (Frontend)
 * decide que hacer con el resultado.
 */
public class Main {

    public static void main(String[] argumentos) {
        int puerto = 8001;

        System.out.println("=== Servicio Lexico (Eslabon 1) ===");
        System.out.println("Puerto: " + puerto);

        // Inyeccion de dependencias (Arquitectura Hexagonal)
        AnalizadorLexico analizador = new AnalizadorLexico();
        ServidorWeb servidor = new ServidorWeb(analizador);

        // Arrancar el servidor
        servidor.iniciar(puerto);
    }
}
