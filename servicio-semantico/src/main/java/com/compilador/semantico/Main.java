package com.compilador.semantico;

import com.compilador.semantico.dominio.ValidadorSemantico;
import com.compilador.semantico.adaptadores.entrada.ServidorWeb;

/**
 * Clase principal del Servicio Semantico.
 * Inicializa los componentes de la arquitectura hexagonal
 * y arranca el servidor HTTP.
 *
 * Patron: Cadena de Responsabilidad Distribuida.
 * Este servicio es un eslabon PURO: solo valida reglas de negocio y responde.
 * No conoce al LLM. El Orquestador (Frontend) decide que hacer con el resultado.
 */
public class Main {

    public static void main(String[] argumentos) {
        int puerto = 8003;

        System.out.println("=== Servicio Semantico (Eslabon 3) ===");
        System.out.println("Puerto: " + puerto);

        // Inyeccion de dependencias (Arquitectura Hexagonal)
        ValidadorSemantico validador = new ValidadorSemantico();
        ServidorWeb servidor = new ServidorWeb(validador);

        // Arrancar el servidor
        servidor.iniciar(puerto);
    }
}
