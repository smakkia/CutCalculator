package com.cutcalculator.gui;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Etichette;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.TipoTaglio;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.preventivo.Preventivo;
import com.cutcalculator.preventivo.RigaProfilo;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Il tab <b>Risultati</b>: i tre output della pipeline in quattro viste — <b>distinta</b> (i pezzi
 * da tagliare), <b>piano</b> (le barre, ad albero, coi pezzi dentro), <b>sfridi</b> (le barre dallo
 * scarto maggiore) e <b>preventivo</b> (le barre da comprare).
 * <p>
 * È una schermata di sola lettura: si limita a mostrare quello che il {@link Controller} ha
 * calcolato. L'etichetta in alto dice a cosa si riferisce e se il magazzino è stato consumato.
 */
public final class SchermataRisultati {

    @FXML private Label titolo;
    @FXML private TabPane schede;
    @FXML private Tab schedaDistinta;
    @FXML private Tab schedaPiano;
    @FXML private Tab schedaSfridi;
    @FXML private Tab schedaPreventivo;

    @FXML private TableView<RigaDistinta> tabellaDistinta;
    @FXML private TableColumn<RigaDistinta, String> distintaMateriale;
    @FXML private TableColumn<RigaDistinta, String> distintaLunghezza;
    @FXML private TableColumn<RigaDistinta, String> distintaTaglio;
    @FXML private TableColumn<RigaDistinta, String> distintaQuantita;
    @FXML private Label riepilogoDistinta;

    @FXML private TreeTableView<NodoPiano> alberoPiano;
    @FXML private TreeTableColumn<NodoPiano, String> pianoVoce;
    @FXML private TreeTableColumn<NodoPiano, String> pianoLunghezza;
    @FXML private TreeTableColumn<NodoPiano, String> pianoTaglio;
    @FXML private TreeTableColumn<NodoPiano, String> pianoSfrido;
    @FXML private Label riepilogoPiano;

    @FXML private TableView<BarraTagliata> tabellaSfridi;
    @FXML private TableColumn<BarraTagliata, String> sfridiMateriale;
    @FXML private TableColumn<BarraTagliata, String> sfridiTipo;
    @FXML private TableColumn<BarraTagliata, String> sfridiBarra;
    @FXML private TableColumn<BarraTagliata, String> sfridiOccupato;
    @FXML private TableColumn<BarraTagliata, String> sfridiSfrido;
    @FXML private TableColumn<BarraTagliata, String> sfridiEsito;

    @FXML private TableView<RigaProfilo> tabellaPreventivo;
    @FXML private TableColumn<RigaProfilo, String> preventivoProfilo;
    @FXML private TableColumn<RigaProfilo, String> preventivoColore;
    @FXML private TableColumn<RigaProfilo, String> preventivoBarreNuove;
    @FXML private TableColumn<RigaProfilo, String> preventivoAvanzi;
    @FXML private TableColumn<RigaProfilo, String> preventivoLunghezza;
    @FXML private TableColumn<RigaProfilo, String> preventivoSfrido;
    @FXML private TableColumn<RigaProfilo, String> preventivoRitagli;
    @FXML private TableColumn<RigaProfilo, String> preventivoRecuperabile;
    @FXML private Label totaliPreventivo;

    private final ObservableList<RigaDistinta> righeDistinta = FXCollections.observableArrayList();
    private final ObservableList<BarraTagliata> righeSfridi = FXCollections.observableArrayList();
    private final ObservableList<RigaProfilo> righePreventivo = FXCollections.observableArrayList();

    private Controller controller;

    /** Una riga della distinta: pezzi uguali (stesso materiale, lunghezza e taglio) contati insieme. */
    public record RigaDistinta(Materiale materiale, double lunghezza, TipoTaglio taglio, long quantita) {
    }

    /** Una riga dell'albero del piano: o una barra, o un pezzo dentro di essa. */
    public record NodoPiano(String voce, String lunghezza, String taglio, String sfrido) {
    }

    @FXML
    private void initialize() {
        distintaMateriale.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.materiale(riga.getValue().materiale())));
        distintaLunghezza.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().lunghezza())));
        distintaTaglio.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.taglio(riga.getValue().taglio())));
        distintaQuantita.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper("x" + riga.getValue().quantita()));
        allineaDestra(distintaLunghezza, distintaQuantita);
        tabellaDistinta.setItems(righeDistinta);
        tabellaDistinta.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabellaDistinta.setPlaceholder(new Label("Nessun calcolo ancora: usa il tab Ordini."));

        pianoVoce.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().getValue().voce()));
        pianoLunghezza.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().getValue().lunghezza()));
        pianoTaglio.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().getValue().taglio()));
        pianoSfrido.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().getValue().sfrido()));
        pianoLunghezza.setStyle("-fx-alignment: CENTER-RIGHT;");
        pianoSfrido.setStyle("-fx-alignment: CENTER-RIGHT;");
        alberoPiano.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        alberoPiano.setShowRoot(false);
        alberoPiano.setPlaceholder(new Label("Nessun calcolo ancora: usa il tab Ordini."));

        sfridiMateriale.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.materiale(riga.getValue().materiale())));
        sfridiTipo.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().avanzo() ? "avanzo" : "barra nuova"));
        sfridiBarra.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().lunghezzaBarra())));
        sfridiOccupato.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().occupato())));
        sfridiSfrido.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().sfrido())));
        sfridiEsito.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().sfrido() >= controller.sogliaRitaglio()
                        ? "torna a magazzino"
                        : "scarto"));
        allineaDestra(sfridiBarra, sfridiOccupato, sfridiSfrido);
        tabellaSfridi.setItems(righeSfridi);
        tabellaSfridi.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabellaSfridi.setPlaceholder(new Label("Nessun calcolo ancora: usa il tab Ordini."));

        preventivoProfilo.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.profilo(riga.getValue().profilo())));
        preventivoColore.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(riga.getValue().colore().nome()));
        preventivoBarreNuove.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(String.valueOf(riga.getValue().barreNuove())));
        preventivoAvanzi.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(String.valueOf(riga.getValue().avanziUsati())));
        preventivoLunghezza.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().lunghezzaNuova())));
        preventivoSfrido.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().sfrido())));
        preventivoRitagli.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(String.valueOf(riga.getValue().ritagliRecuperabili())));
        preventivoRecuperabile.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().lunghezzaRecuperabile())));
        allineaDestra(preventivoBarreNuove, preventivoAvanzi, preventivoLunghezza, preventivoSfrido,
                preventivoRitagli, preventivoRecuperabile);
        tabellaPreventivo.setItems(righePreventivo);
        tabellaPreventivo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabellaPreventivo.setPlaceholder(new Label("Nessun calcolo ancora: usa il tab Ordini."));
    }

    public void inizializza(Controller controller) {
        this.controller = controller;
        aggiornaUnita();
    }

    /** L'utente ha cambiato unità: intestazioni delle misure e celle vanno rifatte. */
    public void aggiornaUnita() {
        String simbolo = " (" + controller.unita().simbolo() + ")";
        distintaLunghezza.setText("Lunghezza" + simbolo);
        pianoLunghezza.setText("Lunghezza" + simbolo);
        pianoSfrido.setText("Sfrido" + simbolo);
        sfridiBarra.setText("Barra" + simbolo);
        sfridiOccupato.setText("Occupato" + simbolo);
        sfridiSfrido.setText("Sfrido" + simbolo);
        preventivoLunghezza.setText("Nuovo" + simbolo);
        preventivoSfrido.setText("Sfrido" + simbolo);
        preventivoRecuperabile.setText("Da riusare" + simbolo);
        tabellaDistinta.refresh();
        alberoPiano.refresh();
        tabellaSfridi.refresh();
        tabellaPreventivo.refresh();
    }

    /** Una misura del modello (mm) nell'unità scelta dall'utente. */
    private String misura(double mm) {
        return Etichette.misura(mm, controller.unita());
    }

    /** Una misura col simbolo dell'unità: per i riepiloghi a testo libero. */
    private String conSimbolo(double mm) {
        return Etichette.misuraConSimbolo(mm, controller.unita());
    }

    // --- Riempimento delle viste -------------------------------------------------------

    public void mostraDistinta(Distinta distinta) {
        righeDistinta.setAll(aggrega(distinta));
        riepilogoDistinta.setText(distinta.totalePezzi() + " pezzi da tagliare, "
                + distinta.perMateriale().size() + " materiali.");
        schede.getSelectionModel().select(schedaDistinta);
    }

    public void mostraPiano(PianoDiTaglio piano) {
        alberoPiano.setRoot(albero(piano));
        riepilogoPiano.setText(piano.numeroBarre() + " barre usate, di cui "
                + piano.barreNuove() + " nuove; media geometrica dello sfrido "
                + conSimbolo(piano.mediaGeometricaSfrido()) + ".");
        schede.getSelectionModel().select(schedaPiano);
    }

    public void mostraSfridi(PianoDiTaglio piano) {
        righeSfridi.setAll(piano.barre().stream()
                .sorted(Comparator.comparingDouble(BarraTagliata::sfrido).reversed())
                .toList());
        schede.getSelectionModel().select(schedaSfridi);
    }

    public void mostraPreventivo(Preventivo preventivo) {
        righePreventivo.setAll(preventivo.righe());
        totaliPreventivo.setText(preventivo.totaleBarreNuove() + " barre nuove da comprare ("
                + conSimbolo(preventivo.lunghezzaNuovaTotale()) + "), "
                + preventivo.totaleAvanziUsati() + " avanzi riusati."
                + "  Sfrido totale " + conSimbolo(preventivo.sfridoTotale())
                + ", di cui " + conSimbolo(preventivo.lunghezzaRecuperabileTotale())
                + " in " + preventivo.totaleRitagliRecuperabili() + " ritagli che tornano a magazzino (>= "
                + conSimbolo(controller.sogliaRitaglio()) + "); scarto effettivo "
                + conSimbolo(preventivo.scartoTotale()) + ".");
        schede.getSelectionModel().select(schedaPreventivo);
    }

    /** L'anteprima di un solo ordine: riempie tutte le viste e apre il preventivo. */
    public void mostraRisultato(Controller.Risultato risultato, String descrizione) {
        mostraDistinta(risultato.distinta());
        mostraPiano(risultato.piano());
        mostraSfridi(risultato.piano());
        mostraPreventivo(risultato.preventivo());
        titolo.setText(descrizione);
    }

    /** Il calcolo globale: piano unico di tutti gli ordini, magazzino già aggiornato. */
    public void mostraEvasione(EvasioneOrdini evasione) {
        mostraPiano(evasione.piano());
        mostraSfridi(evasione.piano());
        mostraPreventivo(evasione.preventivoTotale());
        righeDistinta.clear();
        riepilogoDistinta.setText("Il calcolo globale non produce una distinta per ordine: "
                + "i pezzi di tutti gli ordini sono impacchettati insieme.");
        titolo.setText("Calcolo globale di " + evasione.ordini().size()
                + " ordini - magazzino aggiornato: "
                + evasione.magazzinoAggiornato().stream().mapToInt(Avanzo::quantita).sum()
                + " spezzoni (ritagli oltre "
                + conSimbolo(controller.sogliaRitaglio()) + " rientrati)");
    }

    // --- Costruzione dei modelli di riga -----------------------------------------------

    /** Pezzi uguali contati insieme, per materiale e dal più lungo al più corto. */
    private List<RigaDistinta> aggrega(Distinta distinta) {
        record Chiave(double lunghezza, TipoTaglio taglio) {
        }
        return distinta.perMateriale().entrySet().stream()
                .flatMap(voce -> {
                    Map<Chiave, Long> conteggio = voce.getValue().stream()
                            .collect(Collectors.groupingBy(
                                    p -> new Chiave(p.lunghezza(), p.tipoTaglio()),
                                    Collectors.counting()));
                    return conteggio.entrySet().stream()
                            .map(e -> new RigaDistinta(voce.getKey(), e.getKey().lunghezza(),
                                    e.getKey().taglio(), e.getValue()))
                            .sorted(Comparator.comparingDouble(RigaDistinta::lunghezza).reversed());
                })
                .toList();
    }

    /** L'albero del piano: un nodo per barra, i pezzi come figli. */
    private TreeItem<NodoPiano> albero(PianoDiTaglio piano) {
        TreeItem<NodoPiano> radice = new TreeItem<>(new NodoPiano("Piano", "", "", ""));
        int numero = 0;
        for (BarraTagliata barra : piano.barre()) {
            numero++;
            TreeItem<NodoPiano> nodo = new TreeItem<>(new NodoPiano(
                    "#" + numero + "  " + Etichette.materiale(barra.materiale())
                            + (barra.avanzo() ? "  (avanzo)" : "  (barra nuova)"),
                    misura(barra.lunghezzaBarra()),
                    barra.pezzi().size() + " pezzi",
                    misura(barra.sfrido())));
            for (Pezzo pezzo : barra.pezzi()) {
                nodo.getChildren().add(new TreeItem<>(new NodoPiano(
                        "    " + pezzo.descrizione(),
                        misura(pezzo.lunghezza()),
                        Etichette.taglio(pezzo.tipoTaglio()),
                        "")));
            }
            radice.getChildren().add(nodo);
        }
        radice.setExpanded(true);
        return radice;
    }

    @SafeVarargs
    private static <T> void allineaDestra(TableColumn<T, String>... colonne) {
        for (TableColumn<T, String> colonna : colonne) {
            colonna.setStyle("-fx-alignment: CENTER-RIGHT;");
        }
    }
}
