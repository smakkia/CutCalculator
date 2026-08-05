package com.cutcalculator.cli;

import com.cutcalculator.catalogo.Catalogo;

import java.util.Scanner;

/**
 * Punto d'ingresso del client testuale: crea lo stato ({@link Controller} sul
 * {@link Catalogo#completo() catalogo completo}) e la view ({@link CliView} sullo stdin),
 * poi avvia l'interazione. È solo il "wiring": nessuna logica.
 * <p>
 * Avvio: {@code mvn compile exec:java}.
 */
public final class CliApp {

    private CliApp() {
    }

    public static void main(String[] args) {
        View view = new CliView(new Scanner(System.in));
        view.avvia(new Controller(Catalogo.completo()));
    }
}
