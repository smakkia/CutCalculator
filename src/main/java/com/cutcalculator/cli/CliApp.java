package com.cutcalculator.cli;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.persistenza.ArchivioMagazzino;
import com.cutcalculator.persistenza.ArchivioOrdini;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Punto d'ingresso del client testuale: crea lo stato ({@link Controller} sul
 * {@link Catalogo#completo() catalogo completo}, con il magazzino persistito su disco) e la
 * view ({@link CliView} sullo stdin), poi avvia l'interazione. È solo il "wiring": nessuna logica.
 * <p>
 * Il magazzino vive in {@code dati/magazzino.csv} (nella cartella di lavoro): viene caricato
 * all'avvio e risalvato a ogni modifica. Gli ordini vivono in {@code dati/ordini.csv} e si
 * salvano/caricano <b>su comando</b> dal menu Ordini. Avvio: {@code mvn compile exec:java}.
 */
public final class CliApp {

    private static final Path FILE_MAGAZZINO = Path.of("dati", "magazzino.csv");
    private static final Path FILE_ORDINI = Path.of("dati", "ordini.csv");

    private CliApp() {
    }

    public static void main(String[] args) {
        Catalogo catalogo = Catalogo.completo();
        ArchivioMagazzino archivio = new ArchivioMagazzino(FILE_MAGAZZINO, catalogo);
        ArchivioOrdini archivioOrdini = new ArchivioOrdini(FILE_ORDINI, catalogo);
        View view = new CliView(new Scanner(System.in));
        view.avvia(new Controller(catalogo, archivio, archivioOrdini));
    }
}
