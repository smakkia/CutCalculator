package com.cutcalculator.gui;

import com.cutcalculator.app.Unita;
import com.cutcalculator.persistenza.CalcoloOrdine;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Il dialogo che mostra i documenti di un ordine già calcolato: carica {@code calcolo.fxml} e lo
 * riempie con quanto {@link CalcoloOrdine} ha riletto da disco.
 * <p>
 * È di sola lettura, quindi un bottone solo (Chiudi) e nessun risultato da restituire.
 */
final class DialogoCalcolo {

    private DialogoCalcolo() {
    }

    static void mostra(CalcoloOrdine calcolo, String ordine, boolean ancoraValido, Unita unita) {
        FXMLLoader caricatore = new FXMLLoader(DialogoCalcolo.class.getResource("calcolo.fxml"));
        Dialog<Void> dialogo = new Dialog<>();
        try {
            dialogo.getDialogPane().setContent(caricatore.load());
        } catch (IOException e) {
            throw new UncheckedIOException("FXML non caricabile: calcolo.fxml", e);
        }
        SchermataCalcolo schermata = caricatore.getController();
        schermata.mostra(calcolo, ancoraValido, unita);

        dialogo.setTitle("Calcolo di " + ordine);
        Icone.applica(dialogo);
        dialogo.setHeaderText(null);
        dialogo.setResizable(true);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.showAndWait();
    }
}
