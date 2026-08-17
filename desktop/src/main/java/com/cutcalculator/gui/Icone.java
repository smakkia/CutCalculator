package com.cutcalculator.gui;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * L'icona dell'applicazione, per le finestre del client grafico.
 * <p>
 * Le taglie sono <b>ridisegnate una per una</b> ({@code icona/genera-icone.ps1}) e si passano tutte a
 * JavaFX, che sceglie da sé la più adatta a ogni posto in cui la mostra: la barra del titolo la vuole
 * intorno ai 16-24 pixel, la barra delle applicazioni ai 32-48, {@code Alt+Tab} molto più grande. Con
 * una sola immagine grande sarebbe JavaFX a rimpicciolirla al volo, e a 16 pixel un disegno in
 * assonometria rimpicciolito così diventa una macchia.
 * <p>
 * Non è l'icona dell'<b>eseguibile</b>: quella sta dentro il {@code .exe}, ce la mette {@code jpackage}
 * con {@code --icon} e vive in un file {@code .ico} (vedi {@code desktop/build.gradle.kts}). Questa
 * riguarda il programma mentre gira.
 */
final class Icone {

    /** Le taglie presenti fra le risorse. */
    private static final int[] TAGLIE = {16, 32, 48, 64, 128, 256};

    /** Caricate una volta sola: le stesse immagini servono a tutte le finestre. */
    private static List<Image> immagini;

    private Icone() {
    }

    static List<Image> immagini() {
        if (immagini == null) {
            List<Image> caricate = new ArrayList<>();
            for (int taglia : TAGLIE) {
                // Se una taglia mancasse si va avanti con le altre: un'icona incompleta è meglio di
                // una finestra che non si apre.
                try (InputStream flusso = Icone.class.getResourceAsStream("icona-" + taglia + ".png")) {
                    if (flusso != null) {
                        caricate.add(new Image(flusso));
                    }
                } catch (Exception ignorata) {
                    // taglia saltata
                }
            }
            immagini = List.copyOf(caricate);
        }
        return immagini;
    }

    /** Mette l'icona sulla finestra. */
    static void applica(Stage finestra) {
        finestra.getIcons().setAll(immagini());
    }

    /**
     * Mette l'icona su un dialogo. Un {@code Dialog} (e quindi anche un {@code Alert}) ha una finestra
     * <b>sua</b> e non erediterebbe quella di chi l'ha aperto: senza questo, in mezzo all'app comparivano
     * dialoghi col logo di Java.
     * <p>
     * Si aspetta l'evento di apertura invece di agire subito perché la finestra del dialogo non esiste
     * finché non gli serve; e si usa {@code addEventHandler} invece di {@code setOnShowing} per non
     * portare via il posto a un eventuale gestore di chi chiama.
     */
    static void applica(Dialog<?> dialogo) {
        dialogo.addEventHandler(DialogEvent.DIALOG_SHOWING, evento -> {
            if (dialogo.getDialogPane().getScene() != null
                    && dialogo.getDialogPane().getScene().getWindow() instanceof Stage finestra) {
                applica(finestra);
            }
        });
    }
}
