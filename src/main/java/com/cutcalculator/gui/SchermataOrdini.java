package com.cutcalculator.gui;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Etichette;
import com.cutcalculator.app.Unita;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

import java.util.List;
import java.util.Optional;

/**
 * Il tab <b>Ordini</b>: a sinistra le commesse, a destra i serramenti di quella scelta.
 * <p>
 * Due calcoli, con effetti diversi: <b>Calcola questo ordine</b> è un'<i>anteprima</i> — riusa gli
 * avanzi correnti senza consumarli — mentre <b>Calcola tutti gli ordini</b> unisce tutte le
 * commesse in un piano unico (i pezzi di ordini diversi condividono le barre) e <b>scarica il
 * magazzino</b>, per cui chiede conferma. I risultati finiscono nel tab Risultati.
 */
public final class SchermataOrdini {

    @FXML private ListView<Ordine> elencoOrdini;
    @FXML private Label riepilogo;
    @FXML private TableView<Serramento> tabellaSerramenti;
    @FXML private TableColumn<Serramento, String> colonnaSistema;
    @FXML private TableColumn<Serramento, String> colonnaTipologia;
    @FXML private TableColumn<Serramento, String> colonnaColore;
    @FXML private TableColumn<Serramento, String> colonnaL;
    @FXML private TableColumn<Serramento, String> colonnaH;
    @FXML private TableColumn<Serramento, String> colonnaHF;
    @FXML private TableColumn<Serramento, String> colonnaQuantita;

    private final ObservableList<Ordine> ordini = FXCollections.observableArrayList();
    private final ObservableList<Serramento> serramenti = FXCollections.observableArrayList();
    private Controller controller;
    private SchermataPrincipale principale;

    @FXML
    private void initialize() {
        elencoOrdini.setItems(ordini);
        elencoOrdini.setPlaceholder(new Label("Nessun ordine: creane uno."));
        elencoOrdini.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Ordine ordine, boolean vuota) {
                super.updateItem(ordine, vuota);
                setText(vuota || ordine == null ? null
                        : ordine.nome() + "  (" + ordine.totaleSerramenti() + " serramenti)");
            }
        });
        elencoOrdini.getSelectionModel().selectedItemProperty()
                .addListener((osservabile, prima, adesso) -> mostraSerramentiDi(adesso));

        colonnaSistema.setCellValueFactory(riga -> new ReadOnlyStringWrapper(
                controller.catalogo().sistemaDi(riga.getValue().tipologia())
                        .map(Sistema::nome).orElse("?")));
        colonnaTipologia.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().tipologia().nome()));
        colonnaColore.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().colore().nome()));
        colonnaL.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.misura(riga.getValue().dimensione().L(), unita())));
        colonnaH.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.misura(riga.getValue().dimensione().H(), unita())));
        colonnaHF.setCellValueFactory(riga -> new ReadOnlyStringWrapper(
                riga.getValue().dimensione().HF() > 0
                        ? Etichette.misura(riga.getValue().dimensione().HF(), unita())
                        : "-"));
        colonnaQuantita.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(String.valueOf(riga.getValue().quantita())));
        for (TableColumn<Serramento, String> colonna :
                List.of(colonnaL, colonnaH, colonnaHF, colonnaQuantita)) {
            colonna.setStyle("-fx-alignment: CENTER-RIGHT;");
        }
        tabellaSerramenti.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabellaSerramenti.setItems(serramenti);
        tabellaSerramenti.setPlaceholder(new Label("Ordine vuoto: aggiungi un serramento."));
    }

    public void inizializza(Controller controller, SchermataPrincipale principale) {
        this.controller = controller;
        this.principale = principale;
        aggiornaUnita();
        aggiorna();
    }

    /** L'utente ha cambiato unità: intestazioni delle misure e celle vanno rifatte. */
    public void aggiornaUnita() {
        String simbolo = " (" + unita().simbolo() + ")";
        colonnaL.setText("L" + simbolo);
        colonnaH.setText("H" + simbolo);
        colonnaHF.setText("HF" + simbolo);
        tabellaSerramenti.refresh();
    }

    private Unita unita() {
        return controller.unita();
    }

    /** Rilegge gli ordini dal controller mantenendo, se possibile, quello selezionato. */
    public void aggiorna() {
        Ordine selezionato = elencoOrdini.getSelectionModel().getSelectedItem();
        ordini.setAll(controller.ordini());
        if (selezionato != null && ordini.contains(selezionato)) {
            elencoOrdini.getSelectionModel().select(selezionato);
        } else {
            elencoOrdini.getSelectionModel().selectFirst();
        }
        mostraSerramentiDi(elencoOrdini.getSelectionModel().getSelectedItem());
        riepilogo.setText(ordini.isEmpty()
                ? "Nessun ordine."
                : ordini.size() + " ordini in memoria.");
    }

    /** Porta in primo piano l'ordine dato. */
    public void seleziona(Ordine ordine) {
        if (!ordini.contains(ordine)) {
            aggiorna();
        }
        elencoOrdini.getSelectionModel().select(ordine);
    }

    private void mostraSerramentiDi(Ordine ordine) {
        serramenti.setAll(ordine == null ? List.of() : ordine.serramenti());
    }

    /** L'ordine su cui agire, o vuoto (con messaggio) se non ce n'è uno selezionato. */
    private Optional<Ordine> scelto() {
        Ordine ordine = elencoOrdini.getSelectionModel().getSelectedItem();
        if (ordine == null) {
            Dialoghi.errore("Nessun ordine scelto", "Seleziona un ordine dall'elenco.");
        }
        return Optional.ofNullable(ordine);
    }

    // --- Azioni sugli ordini -----------------------------------------------------------

    @FXML
    private void nuovoOrdine() {
        chiediNome("Nuovo ordine", "Ordine " + (controller.ordini().size() + 1))
                .ifPresent(nome -> {
                    Ordine ordine = controller.nuovoOrdine(nome);
                    aggiorna();
                    elencoOrdini.getSelectionModel().select(ordine);
                });
    }

    @FXML
    private void rinominaOrdine() {
        scelto().ifPresent(ordine -> chiediNome("Rinomina ordine", ordine.nome())
                .ifPresent(nome -> {
                    ordine.rinomina(nome);
                    elencoOrdini.refresh();
                }));
    }

    @FXML
    private void rimuoviOrdine() {
        scelto().ifPresent(ordine -> {
            if (Dialoghi.conferma("Rimuovi ordine", "Rimuovo l'ordine \"" + ordine.nome() + "\"?")) {
                controller.rimuoviOrdine(ordine);
                aggiorna();
            }
        });
    }

    /** Chiede un nome d'ordine valido: non vuoto e senza {@code ;} (separatore del CSV). */
    private Optional<String> chiediNome(String titolo, String iniziale) {
        while (true) {
            TextInputDialog dialogo = new TextInputDialog(iniziale);
            dialogo.setTitle(titolo);
            dialogo.setHeaderText(null);
            dialogo.setContentText("Nome");
            Optional<String> risposta = dialogo.showAndWait().map(String::trim);
            if (risposta.isEmpty()) {
                return Optional.empty();
            }
            String nome = risposta.get();
            if (nome.isBlank()) {
                Dialoghi.errore("Nome mancante", "Dai un nome all'ordine.");
            } else if (nome.contains(";")) {
                Dialoghi.errore("Nome non valido",
                        "Il nome non puo' contenere ';' (e' il separatore del file).");
            } else {
                return Optional.of(nome);
            }
        }
    }

    // --- Azioni sui serramenti ---------------------------------------------------------

    @FXML
    private void aggiungiSerramento() {
        scelto().ifPresent(ordine -> DialogoSerramento.chiedi(controller.catalogo(), unita())
                .ifPresent(serramento -> {
                    ordine.aggiungi(serramento);
                    mostraSerramentiDi(ordine);
                    elencoOrdini.refresh();
                }));
    }

    @FXML
    private void rimuoviSerramento() {
        Serramento selezionato = tabellaSerramenti.getSelectionModel().getSelectedItem();
        if (selezionato == null) {
            Dialoghi.errore("Nessun serramento scelto", "Seleziona la riga da rimuovere.");
            return;
        }
        scelto().ifPresent(ordine -> {
            // Come in magazzino: si cerca l'oggetto, non l'indice di riga (l'ordinamento
            // per colonna scollerebbe i due indici).
            ordine.rimuovi(ordine.serramenti().indexOf(selezionato));
            mostraSerramentiDi(ordine);
            elencoOrdini.refresh();
        });
    }

    // --- Calcoli -----------------------------------------------------------------------

    @FXML
    private void calcolaOrdine() {
        scelto().ifPresent(ordine -> {
            if (ordine.serramenti().isEmpty()) {
                Dialoghi.errore("Ordine vuoto", "Aggiungi almeno un serramento.");
                return;
            }
            try {
                principale.mostraRisultato(controller.calcola(ordine),
                        "Anteprima dell'ordine \"" + ordine.nome() + "\""
                                + " (gli avanzi non vengono consumati)");
            } catch (IllegalArgumentException nonCalcolabile) {
                Dialoghi.errore("Calcolo non riuscito", nonCalcolabile);
            }
        });
    }

    @FXML
    private void calcolaTutti() {
        if (controller.ordini().isEmpty()) {
            Dialoghi.errore("Nessun ordine", "Crea almeno un ordine.");
            return;
        }
        boolean conferma = Dialoghi.conferma("Calcola tutti gli ordini",
                "Tutti gli ordini vengono uniti in un piano di taglio unico e il magazzino viene "
                        + "aggiornato: gli avanzi usati sono consumati e i ritagli sopra "
                        + Etichette.misuraConSimbolo(controller.sogliaRitaglio(), unita())
                        + " rientrano. Procedo?");
        if (!conferma) {
            return;
        }
        try {
            EvasioneOrdini evasione = controller.evadiOrdini();
            principale.mostraEvasione(evasione);
        } catch (IllegalArgumentException nonCalcolabile) {
            Dialoghi.errore("Calcolo non riuscito", nonCalcolabile);
        }
    }
}
