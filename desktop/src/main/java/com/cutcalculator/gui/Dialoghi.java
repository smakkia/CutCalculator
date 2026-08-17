package com.cutcalculator.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

import java.util.Optional;

/**
 * I tre dialoghi che servono alla GUI — informazione, errore, conferma — in una riga sola.
 * Sono l'equivalente grafico dei messaggi che la CLI stampa a console.
 * <p>
 * I testi lunghi (l'elenco del catalogo, un riepilogo) finiscono in una {@link TextArea}
 * scorrevole invece che nell'etichetta: un {@code Alert} normale li troncherebbe.
 */
final class Dialoghi {

    /** Oltre questo numero di righe il messaggio va in una TextArea scorrevole. */
    private static final int RIGHE_LUNGHE = 8;

    private Dialoghi() {
    }

    static void informa(String titolo, String messaggio) {
        mostra(Alert.AlertType.INFORMATION, titolo, messaggio);
    }

    static void errore(String titolo, String messaggio) {
        mostra(Alert.AlertType.ERROR, titolo, messaggio);
    }

    /** {@code true} se l'utente ha premuto OK. */
    static boolean conferma(String titolo, String domanda) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, domanda, ButtonType.OK, ButtonType.CANCEL);
        Icone.applica(alert);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private static void mostra(Alert.AlertType tipo, String titolo, String messaggio) {
        Alert alert = new Alert(tipo);
        Icone.applica(alert);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        if (messaggio.lines().count() > RIGHE_LUNGHE) {
            TextArea testo = new TextArea(messaggio);
            testo.setEditable(false);
            testo.setPrefColumnCount(46);
            testo.setPrefRowCount(24);
            alert.getDialogPane().setContent(testo);
        } else {
            alert.setContentText(messaggio);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        }
        alert.showAndWait();
    }

    /** Comodo per i {@code catch}: mostra il messaggio dell'eccezione come errore. */
    static void errore(String titolo, Exception errore) {
        errore(titolo, Optional.ofNullable(errore.getMessage()).orElse(errore.toString()));
    }
}
