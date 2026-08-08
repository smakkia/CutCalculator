package com.cutcalculator.gui;

import com.cutcalculator.app.Unita;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Dimensione;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.dominio.Tipologia;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Il modulo "aggiungi serramento" ({@code serramento.fxml}), mostrato dentro un dialogo da
 * {@link DialogoSerramento}: scelta del sistema, della tipologia, colore, misure e quantità.
 * <p>
 * Le due ComboBox sono <b>a cascata</b> (il sistema decide le tipologie) e il campo <b>HF</b>
 * (altezza parziale) si abilita solo per le tipologie che la usano — le porte, dove il traverso
 * spezza il vetro: {@link Tipologia#usaHF()}.
 */
public final class SchermataSerramento {

    @FXML private ComboBox<Sistema> sceltaSistema;
    @FXML private ComboBox<Tipologia> sceltaTipologia;
    @FXML private TextField campoColore;
    @FXML private TextField campoL;
    @FXML private TextField campoH;
    @FXML private TextField campoHF;
    @FXML private Label etichettaL;
    @FXML private Label etichettaH;
    @FXML private Label etichettaHF;
    @FXML private Spinner<Integer> campoQuantita;

    private Unita unita = Unita.PREDEFINITA;

    @FXML
    private void initialize() {
        campoQuantita.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        sceltaSistema.setConverter(Campi.converter(s -> s.nome() + " (" + s.famiglia() + ")"));
        sceltaTipologia.setConverter(Campi.converter(Tipologia::nome));
        sceltaSistema.valueProperty().addListener((osservabile, prima, adesso) -> tipologieDi(adesso));
        sceltaTipologia.valueProperty().addListener((osservabile, prima, adesso) -> abilitaHF(adesso));
    }

    /** Riempie le scelte col catalogo dell'app e adotta l'unità di misura scelta dall'utente. */
    public void inizializza(Catalogo catalogo, Unita unita) {
        this.unita = unita;
        sceltaSistema.getItems().setAll(catalogo.sistemi());
        sceltaSistema.getSelectionModel().selectFirst();
        campoL.setPromptText(unita.simbolo());
        campoH.setPromptText(unita.simbolo());
        campoHF.setPromptText("solo per le porte, in " + unita.simbolo());
        etichettaL.setText("Larghezza L (" + unita.simbolo() + ")");
        etichettaH.setText("Altezza H (" + unita.simbolo() + ")");
        etichettaHF.setText("Altezza parziale HF (" + unita.simbolo() + ")");
    }

    private void tipologieDi(Sistema sistema) {
        sceltaTipologia.getItems().setAll(sistema == null ? List.of() : sistema.tipologie());
        sceltaTipologia.getSelectionModel().selectFirst();
    }

    private void abilitaHF(Tipologia tipologia) {
        boolean serve = tipologia != null && tipologia.usaHF();
        campoHF.setDisable(!serve);
        etichettaHF.setDisable(!serve);
        if (!serve) {
            campoHF.clear();
        }
    }

    /**
     * Il serramento descritto dai campi, oppure vuoto se manca qualcosa: in quel caso il messaggio
     * per l'utente è già stato mostrato.
     */
    public Optional<Serramento> serramento() {
        Tipologia tipologia = sceltaTipologia.getValue();
        if (tipologia == null) {
            Dialoghi.errore("Tipologia mancante", "Scegli un sistema e una tipologia.");
            return Optional.empty();
        }
        Optional<Colore> colore = Campi.colore(campoColore);
        if (colore.isEmpty()) {
            Dialoghi.errore("Colore non valido",
                    "Scrivi un colore (es. bianco, bronzo, RAL9010). Il ';' non e' ammesso.");
            return Optional.empty();
        }
        OptionalDouble l = Campi.misura(campoL, unita);
        OptionalDouble h = Campi.misura(campoH, unita);
        if (l.isEmpty() || h.isEmpty()) {
            Dialoghi.errore("Misure non valide", "L e H: " + Campi.misuraNonValida(unita));
            return Optional.empty();
        }
        double hf = 0;
        if (tipologia.usaHF()) {
            OptionalDouble parziale = Campi.misura(campoHF, unita);
            if (parziale.isEmpty()) {
                Dialoghi.errore("Altezza parziale mancante",
                        "Questa tipologia usa l'altezza parziale HF: inseriscila in "
                                + unita.simbolo() + ".");
                return Optional.empty();
            }
            hf = parziale.getAsDouble();
        }
        return Optional.of(new Serramento(tipologia, colore.get(),
                new Dimensione(l.getAsDouble(), h.getAsDouble(), hf), campoQuantita.getValue()));
    }
}
