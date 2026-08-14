package com.cutcalculator.gui;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.View;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.pianificazione.DistintaOrdine;
import com.cutcalculator.persistenza.ArchivioCalcoli;
import com.cutcalculator.persistenza.ArchivioImpostazioni;
import com.cutcalculator.persistenza.ArchivioMagazzino;
import com.cutcalculator.persistenza.ArchivioOrdini;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.preventivo.Preventivo;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Realizzazione grafica di {@link View}: monta la finestra JavaFX sopra lo stesso
 * {@link Controller} che usa il client testuale — il core non sa quale front-end lo stia usando.
 * <p>
 * Qui c'è solo il <b>wiring</b> (catalogo, archivi su disco, caricamento dell'FXML radice): tutta
 * l'interazione vive nelle schermate ({@link SchermataPrincipale} e i suoi tab), disegnate in
 * SceneBuilder e collegate via {@code fx:controller}. I metodi di {@code View} sono deleghe alla
 * schermata principale, che sa quale tab mostrare.
 * <p>
 * Non si lancia da qui: il {@code main} sta in {@link GuiApp} (vedi il perché nel suo javadoc).
 */
public final class GuiFx extends Application implements View {

    private static final Path CARTELLA_DATI = Path.of("dati");
    private static final Path FILE_MAGAZZINO = CARTELLA_DATI.resolve("magazzino.csv");
    private static final Path FILE_ORDINI = CARTELLA_DATI.resolve("ordini.csv");
    private static final Path FILE_IMPOSTAZIONI = CARTELLA_DATI.resolve("impostazioni.properties");

    private Stage finestra;
    private SchermataPrincipale schermata;

    @Override
    public void start(Stage finestra) {
        this.finestra = finestra;
        Catalogo catalogo = Catalogo.completo();
        ArchivioMagazzino archivio = new ArchivioMagazzino(FILE_MAGAZZINO, catalogo);
        ArchivioOrdini archivioOrdini = new ArchivioOrdini(FILE_ORDINI, catalogo);
        ArchivioImpostazioni impostazioni = new ArchivioImpostazioni(FILE_IMPOSTAZIONI);
        avvia(new Controller(catalogo, archivio, archivioOrdini, impostazioni,
                new ArchivioCalcoli(CARTELLA_DATI)));
    }

    /**
     * Carica l'FXML radice, gli passa il controller e mostra la finestra. A differenza della CLI
     * non blocca: da qui in poi guida tutto il thread degli eventi JavaFX.
     */
    @Override
    public void avvia(Controller controller) {
        FXMLLoader caricatore = new FXMLLoader(getClass().getResource("principale.fxml"));
        Parent radice;
        try {
            radice = caricatore.load();
        } catch (IOException e) {
            throw new UncheckedIOException("FXML non caricabile: principale.fxml", e);
        }
        schermata = caricatore.getController();
        schermata.inizializza(controller);
        finestra.setTitle("CutCalculator");
        finestra.setScene(new Scene(radice, 1100, 720));
        finestra.show();
    }

    // --- Viste sui dati: deleghe alla schermata principale ------------------------------

    @Override
    public void catalogo(Catalogo catalogo) {
        schermata.mostraCatalogo(catalogo);
    }

    @Override
    public void ordine(Ordine ordine) {
        schermata.mostraOrdine(ordine);
    }

    @Override
    public void magazzino(List<Avanzo> avanzi) {
        schermata.mostraMagazzino();
    }

    @Override
    public void distinta(List<DistintaOrdine> distinte) {
        schermata.mostraDistinta(distinte);
    }

    @Override
    public void piano(PianoDiTaglio piano) {
        schermata.mostraPiano(piano);
    }

    @Override
    public void sfridi(PianoDiTaglio piano) {
        schermata.mostraSfridi(piano);
    }

    @Override
    public void preventivo(Preventivo preventivo) {
        schermata.mostraPreventivo(preventivo);
    }

    @Override
    public void evasione(EvasioneOrdini evasione) {
        schermata.mostraEvasione(evasione);
    }
}
