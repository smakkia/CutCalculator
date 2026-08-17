package com.cutcalculator.gui;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Etichette;
import com.cutcalculator.app.Unita;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Profilo;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.IntStream;

/**
 * Il tab <b>Magazzino</b>: la tabella degli {@link Avanzo avanzi} (nella UI si chiamano "pezzi",
 * come nella CLI) e le operazioni su di essa — aggiungi, rimuovi (anche solo qualche spezzone
 * della riga) e svuota.
 * <p>
 * Non tiene liste proprie: la tabella è sempre una fotografia di {@code controller.magazzino()},
 * e ogni modifica passa dal {@link Controller} (che la persiste su disco).
 */
public final class SchermataMagazzino {

    @FXML private TableView<Avanzo> tabella;
    @FXML private TableColumn<Avanzo, String> colonnaProfilo;
    @FXML private TableColumn<Avanzo, String> colonnaColore;
    @FXML private TableColumn<Avanzo, String> colonnaLunghezza;
    @FXML private TableColumn<Avanzo, String> colonnaQuantita;
    @FXML private ComboBox<Sistema> sceltaSistema;
    @FXML private ComboBox<Profilo> sceltaProfilo;
    @FXML private TextField campoColore;
    @FXML private TextField campoLunghezza;
    @FXML private Spinner<Integer> campoQuantita;
    @FXML private Label riepilogo;

    private final ObservableList<Avanzo> righe = FXCollections.observableArrayList();
    private Controller controller;

    /** Configura tabella e campi. Chiamato da JavaFX dopo aver iniettato i nodi dell'FXML. */
    @FXML
    private void initialize() {
        // I record non hanno i getter "get*" che si aspetta PropertyValueFactory: si legge a mano.
        colonnaProfilo.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.profilo(riga.getValue().profilo())));
        colonnaColore.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().colore().nome()));
        colonnaLunghezza.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.misura(riga.getValue().lunghezza(), unita())));
        colonnaQuantita.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(String.valueOf(riga.getValue().quantita())));
        colonnaLunghezza.setStyle("-fx-alignment: CENTER-RIGHT;");
        colonnaQuantita.setStyle("-fx-alignment: CENTER-RIGHT;");
        tabella.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabella.setItems(righe);
        tabella.setPlaceholder(new Label("Magazzino vuoto: aggiungi un pezzo qui sotto."));

        campoQuantita.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        sceltaSistema.setConverter(Campi.converter(s -> s.nome() + " (" + s.famiglia() + ")"));
        sceltaProfilo.setConverter(Campi.converter(Etichette::profilo));
        sceltaSistema.valueProperty().addListener((osservabile, prima, adesso) -> profiliDi(adesso));
    }

    /** Collega lo stato dell'app e riempie la schermata. */
    public void inizializza(Controller controller) {
        this.controller = controller;
        sceltaSistema.getItems().setAll(controller.catalogo().sistemi());
        sceltaSistema.getSelectionModel().selectFirst();
        aggiornaUnita();
        aggiorna();
    }

    /** Rilegge il magazzino dal controller: da chiamare dopo ogni modifica, anche altrui. */
    public void aggiorna() {
        List<Avanzo> magazzino = controller.magazzino();
        righe.setAll(magazzino);
        riepilogo.setText(magazzino.isEmpty()
                ? "Nessun pezzo a magazzino."
                : magazzino.size() + " righe, " + controller.totaleAvanzi() + " spezzoni in totale.");
    }

    /** L'utente ha cambiato unità: intestazione, prompt e celle vanno rifatti. */
    public void aggiornaUnita() {
        colonnaLunghezza.setText("Lunghezza (" + unita().simbolo() + ")");
        campoLunghezza.setPromptText(unita().simbolo());
        tabella.refresh();
    }

    private Unita unita() {
        return controller.unita();
    }

    private void profiliDi(Sistema sistema) {
        sceltaProfilo.getItems().setAll(sistema == null ? List.of() : sistema.profili());
        sceltaProfilo.getSelectionModel().selectFirst();
    }

    // --- Azioni ------------------------------------------------------------------------

    @FXML
    private void aggiungi() {
        Profilo profilo = sceltaProfilo.getValue();
        if (profilo == null) {
            Dialoghi.errore("Profilo mancante", "Scegli un sistema e un profilo.");
            return;
        }
        Optional<Colore> colore = Campi.colore(campoColore);
        if (colore.isEmpty()) {
            Dialoghi.errore("Colore non valido",
                    "Scrivi un colore (es. bianco, bronzo, RAL9010). Il ';' non e' ammesso.");
            return;
        }
        OptionalDouble lunghezza = Campi.misura(campoLunghezza, unita());
        if (lunghezza.isEmpty()) {
            Dialoghi.errore("Lunghezza non valida", Campi.misuraNonValida(unita()));
            return;
        }
        controller.aggiungiAvanzo(new Avanzo(profilo, colore.get(),
                lunghezza.getAsDouble(), campoQuantita.getValue()));
        campoLunghezza.clear();
        aggiorna();
    }

    @FXML
    private void rimuovi() {
        Avanzo selezionato = tabella.getSelectionModel().getSelectedItem();
        if (selezionato == null) {
            Dialoghi.errore("Nessuna riga scelta", "Seleziona il pezzo da rimuovere.");
            return;
        }
        int quantita = selezionato.quantita() == 1 ? 1 : quantiRimuoverne(selezionato);
        if (quantita <= 0) {
            return;
        }
        // L'indice della riga nella tabella non serve: con l'ordinamento per colonna non
        // coinciderebbe piu' con quello nel magazzino, quindi si cerca l'avanzo scelto.
        int indice = controller.magazzino().indexOf(selezionato);
        if (indice < 0) {
            // La tabella mostrava un magazzino superato (p.es. dopo un calcolo che l'ha consumato):
            // meglio rinfrescarla che passare un -1 al controller, che lancerebbe.
            Dialoghi.errore("Riga non piu' valida",
                    "Il pezzo non e' piu' a magazzino: la tabella e' stata aggiornata.");
            aggiorna();
            return;
        }
        controller.rimuoviAvanzo(indice, quantita);
        aggiorna();
    }

    /** Chiede quanti spezzoni togliere dalla riga; 0 se l'utente annulla. */
    private int quantiRimuoverne(Avanzo avanzo) {
        List<Integer> scelte = IntStream.rangeClosed(1, avanzo.quantita()).boxed().toList();
        ChoiceDialog<Integer> dialogo = new ChoiceDialog<>(avanzo.quantita(), scelte);
        Icone.applica(dialogo);
        dialogo.setTitle("Rimuovi pezzo");
        dialogo.setHeaderText(Etichette.profilo(avanzo.profilo())
                + " [" + avanzo.colore().nome() + "] da "
                + Etichette.misuraConSimbolo(avanzo.lunghezza(), unita()));
        dialogo.setContentText("Quanti toglierne");
        return dialogo.showAndWait().orElse(0);
    }

    @FXML
    private void svuota() {
        if (controller.magazzino().isEmpty()) {
            Dialoghi.errore("Magazzino vuoto", "Non c'e' niente da togliere.");
            return;
        }
        if (!Dialoghi.conferma("Svuota magazzino",
                "Tolgo tutti i " + controller.totaleAvanzi() + " spezzoni a magazzino. Procedo?")) {
            return;
        }
        controller.svuotaMagazzino();
        aggiorna();
    }
}
