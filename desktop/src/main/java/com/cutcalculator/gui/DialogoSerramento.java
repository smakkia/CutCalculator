package com.cutcalculator.gui;

import com.cutcalculator.app.Unita;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Serramento;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Il dialogo che chiede un {@link Serramento}: mostra {@code serramento.fxml} e restituisce il
 * serramento compilato, oppure vuoto se l'utente annulla.
 * <p>
 * Lo stesso dialogo serve per <b>aggiungerne</b> uno nuovo ({@link #chiedi}) e per <b>correggerne</b>
 * uno già inserito ({@link #modifica}, che parte coi campi riempiti): cambiano il titolo e il punto
 * di partenza, non le regole.
 * <p>
 * Il pulsante OK è filtrato: se i campi non sono validi il messaggio d'errore parte e l'evento
 * viene consumato, così il dialogo <b>resta aperto</b> invece di perdere quanto scritto.
 */
final class DialogoSerramento {

    private DialogoSerramento() {
    }

    /** Un serramento nuovo, coi prezzi proposti già scritti nei campi. */
    static Optional<Serramento> chiedi(Catalogo catalogo, Unita unita, Prezzi predefiniti) {
        return mostra(catalogo, unita, predefiniti, null);
    }

    /**
     * La versione corretta di un serramento già inserito: i campi partono dai suoi valori, e chi
     * conferma senza toccare niente riottiene lo stesso serramento.
     */
    static Optional<Serramento> modifica(Catalogo catalogo, Unita unita, Serramento partenza) {
        return mostra(catalogo, unita, partenza.prezzi(), partenza);
    }

    /** @param partenza il serramento da correggere, oppure {@code null} se se ne aggiunge uno */
    private static Optional<Serramento> mostra(Catalogo catalogo, Unita unita, Prezzi predefiniti,
            Serramento partenza) {
        FXMLLoader caricatore = new FXMLLoader(
                DialogoSerramento.class.getResource("serramento.fxml"));
        Dialog<Serramento> dialogo = new Dialog<>();
        try {
            dialogo.getDialogPane().setContent(caricatore.load());
        } catch (IOException e) {
            throw new UncheckedIOException("FXML non caricabile: serramento.fxml", e);
        }
        SchermataSerramento schermata = caricatore.getController();
        schermata.inizializza(catalogo, unita, predefiniti);
        if (partenza != null) {
            schermata.precompila(partenza);
        }

        dialogo.setTitle(partenza == null ? "Aggiungi serramento" : "Modifica serramento");
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Contenitore di una riga: il filtro sul bottone non può ritornare valori.
        Serramento[] compilato = new Serramento[1];
        Node ok = dialogo.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, evento -> {
            Optional<Serramento> serramento = schermata.serramento();
            if (serramento.isEmpty()) {
                evento.consume();
                return;
            }
            compilato[0] = serramento.get();
        });
        dialogo.setResultConverter(bottone -> bottone == ButtonType.OK ? compilato[0] : null);
        return dialogo.showAndWait();
    }
}
