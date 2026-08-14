package com.cutcalculator.cli;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.View;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.persistenza.ArchivioCalcoli;
import com.cutcalculator.persistenza.ArchivioImpostazioni;
import com.cutcalculator.persistenza.ArchivioMagazzino;
import com.cutcalculator.persistenza.ArchivioOrdini;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Punto d'ingresso del client testuale: crea lo stato ({@link Controller} sul
 * {@link Catalogo#completo() catalogo completo}, con il magazzino persistito su disco) e la
 * view ({@link CliView} sullo stdin), poi avvia l'interazione. È solo il "wiring": nessuna logica.
 * <p>
 * Tutto vive nella cartella {@code dati/} (relativa alla cartella di lavoro): magazzino, ordini e
 * impostazioni si caricano all'avvio e si risalvano a ogni modifica; i risultati di ogni calcolo
 * globale finiscono nei {@code calcoli-*.csv}. Avvio: {@code mvn compile exec:java}.
 */
public final class CliApp {

    private static final Path CARTELLA_DATI = Path.of("dati");
    private static final Path FILE_MAGAZZINO = CARTELLA_DATI.resolve("magazzino.csv");
    private static final Path FILE_ORDINI = CARTELLA_DATI.resolve("ordini.csv");
    private static final Path FILE_IMPOSTAZIONI = CARTELLA_DATI.resolve("impostazioni.properties");

    private CliApp() {
    }

    public static void main(String[] args) {
        Catalogo catalogo = Catalogo.completo();
        ArchivioMagazzino archivio = new ArchivioMagazzino(FILE_MAGAZZINO, catalogo);
        ArchivioOrdini archivioOrdini = new ArchivioOrdini(FILE_ORDINI, catalogo);
        ArchivioImpostazioni impostazioni = new ArchivioImpostazioni(FILE_IMPOSTAZIONI);
        ArchivioCalcoli calcoli = new ArchivioCalcoli(CARTELLA_DATI);
        View view = new CliView(new Scanner(System.in));
        view.avvia(new Controller(catalogo, archivio, archivioOrdini, impostazioni, calcoli));
    }
}
