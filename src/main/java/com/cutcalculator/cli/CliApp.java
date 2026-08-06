package com.cutcalculator.cli;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.persistenza.ArchivioMagazzino;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Punto d'ingresso del client testuale: crea lo stato ({@link Controller} sul
 * {@link Catalogo#completo() catalogo completo}, con il magazzino persistito su disco) e la
 * view ({@link CliView} sullo stdin), poi avvia l'interazione. È solo il "wiring": nessuna logica.
 * <p>
 * Il magazzino vive in {@code dati/magazzino.csv} (nella cartella di lavoro): viene caricato
 * all'avvio e risalvato a ogni modifica. Avvio: {@code mvn compile exec:java}.
 */
public final class CliApp {

    private static final Path FILE_MAGAZZINO = Path.of("dati", "magazzino.csv");

    private CliApp() {
    }

    public static void main(String[] args) {
        Catalogo catalogo = Catalogo.completo();
        ArchivioMagazzino archivio = new ArchivioMagazzino(FILE_MAGAZZINO, catalogo);
        View view = new CliView(new Scanner(System.in));
        view.avvia(new Controller(catalogo, archivio));
    }
}
