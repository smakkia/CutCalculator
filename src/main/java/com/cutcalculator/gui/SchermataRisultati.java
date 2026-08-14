package com.cutcalculator.gui;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Etichette;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.TipoTaglio;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.preventivo.Preventivo;
import com.cutcalculator.preventivo.RigaProfilo;
import com.cutcalculator.preventivo.RigaVetro;
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
    @FXML private TableColumn<RigaProfilo, String> preventivoPeso;
    @FXML private TableColumn<RigaProfilo, String> preventivoCosto;
    @FXML private Label totaliPreventivo;
    @FXML private Label costiPreventivo;

    @FXML private Tab schedaVetri;
    @FXML private TableView<RigaVetro> tabellaVetri;
    @FXML private TableColumn<RigaVetro, String> vetriAltezza;
    @FXML private TableColumn<RigaVetro, String> vetriLarghezza;
    @FXML private TableColumn<RigaVetro, String> vetriQuantita;
    @FXML private TableColumn<RigaVetro, String> vetriAreaLastra;
    @FXML private TableColumn<RigaVetro, String> vetriAreaTotale;
    @FXML private TableColumn<RigaVetro, String> vetriCosto;
    @FXML private Label totaliVetri;

    private final ObservableList<RigaDistinta> righeDistinta = FXCollections.observableArrayList();
    private final ObservableList<BarraTagliata> righeSfridi = FXCollections.observableArrayList();
    private final ObservableList<RigaProfilo> righePreventivo = FXCollections.observableArrayList();
    private final ObservableList<RigaVetro> righeVetri = FXCollections.observableArrayList();

    private Controller controller;
    private Preventivo preventivoMostrato;

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
        preventivoPeso.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(kg(riga.getValue().peso())));
        preventivoCosto.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(euro(riga.getValue().costo(prezzi()))));
        allineaDestra(preventivoBarreNuove, preventivoAvanzi, preventivoLunghezza, preventivoSfrido,
                preventivoRitagli, preventivoRecuperabile, preventivoPeso, preventivoCosto);
        tabellaPreventivo.setItems(righePreventivo);
        tabellaPreventivo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabellaPreventivo.setPlaceholder(new Label("Nessun calcolo ancora: usa il tab Ordini."));

        vetriAltezza.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().lunghezza())));
        vetriLarghezza.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(misura(riga.getValue().larghezza())));
        vetriQuantita.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(String.valueOf(riga.getValue().quantita())));
        vetriAreaLastra.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(mq(riga.getValue().areaMq())));
        vetriAreaTotale.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(mq(riga.getValue().areaTotaleMq())));
        vetriCosto.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(euro(riga.getValue().costo(prezzi()))));
        allineaDestra(vetriAltezza, vetriLarghezza, vetriQuantita, vetriAreaLastra, vetriAreaTotale,
                vetriCosto);
        tabellaVetri.setItems(righeVetri);
        tabellaVetri.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabellaVetri.setPlaceholder(new Label("Nessuna quota vetro per queste tipologie."));
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
        vetriAltezza.setText("Altezza H" + simbolo);
        vetriLarghezza.setText("Larghezza L" + simbolo);
        tabellaDistinta.refresh();
        alberoPiano.refresh();
        tabellaSfridi.refresh();
        tabellaPreventivo.refresh();
        tabellaVetri.refresh();
    }

    /**
     * L'utente ha cambiato il listino: i costi mostrati vanno rifatti. Il preventivo però porta
     * dentro di sé i prezzi con cui è stato generato, quindi l'unico modo onesto di aggiornarlo è
     * ricalcolarlo — qui ci limitiamo a dirlo, senza far finta che i vecchi numeri siano nuovi.
     */
    public void aggiornaPrezzi() {
        if (!righePreventivo.isEmpty() || !righeVetri.isEmpty()) {
            titolo.setText("Prezzi aggiornati: ricalcola per vedere i nuovi costi.");
        }
    }

    /** Una misura del modello (mm) nell'unità scelta dall'utente. */
    private String misura(double mm) {
        return Etichette.misura(mm, controller.unita());
    }

    /**
     * Un'area in metri quadri: il vetro si compra a mq qualunque sia l'unità scelta per le
     * lunghezze, quindi questa colonna non segue l'impostazione.
     */
    private static String mq(double areaMq) {
        return String.format("%.3f", areaMq);
    }

    /** Un importo in euro. */
    private static String euro(double importo) {
        return String.format("%.2f €", importo);
    }

    /** Un peso in chilogrammi (l'unità sta nell'intestazione di colonna). */
    private static String kg(double chili) {
        return String.format("%.2f", chili);
    }

    /**
     * Il listino con cui valorizzare le celle: quello del preventivo mostrato, non quello corrente
     * del controller — una tabella deve restare coerente con i totali sotto di lei.
     */
    private Prezzi prezzi() {
        return preventivoMostrato == null ? controller.prezzi() : preventivoMostrato.prezzi();
    }

    /** Una misura col simbolo dell'unità: per i riepiloghi a testo libero. */
    private String conSimbolo(double mm) {
        return Etichette.misuraConSimbolo(mm, controller.unita());
    }

    // --- Riempimento delle viste -------------------------------------------------------

    public void mostraDistinta(Distinta distinta) {
        righeDistinta.setAll(aggrega(distinta));
        riepilogoDistinta.setText(distinta.totalePezzi() + " pezzi da tagliare, "
                + distinta.perMateriale().size() + " materiali"
                + (distinta.vetri().isEmpty() ? "." : "; " + distinta.totaleLastre() + " lastre di vetro ("
                        + mq(distinta.areaVetroTotaleMq()) + " mq) nel tab Vetri."));
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
        preventivoMostrato = preventivo;
        mostraVetri(preventivo);
        mostraCosti(preventivo);
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

    /**
     * Il conto dell'ordine: barre nuove + vetro. Se manca un prezzo lo dice, invece di mostrare uno
     * zero che sembrerebbe un calcolo riuscito; peso a zero significa che i profili del catalogo non
     * hanno ancora i kg/m di scheda, quindi le barre non si possono valorizzare.
     */
    private void mostraCosti(Preventivo preventivo) {
        Prezzi prezzi = preventivo.prezzi();
        if (!prezzi.impostati()) {
            costiPreventivo.setText("Costi non calcolati: imposta i prezzi da File -> "
                    + "Prezzi del materiale...");
            return;
        }
        String avviso = prezzi.alChiloBarre() > 0 && preventivo.pesoTotale() == 0
                ? "   (le barre pesano 0: i profili del catalogo non hanno ancora i kg/m)"
                : "";
        costiPreventivo.setText("Barre nuove " + euro(preventivo.costoBarre())
                + " (" + kg(preventivo.pesoTotale()) + " kg)"
                + "   +   vetro " + euro(preventivo.costoVetro())
                + " (" + mq(preventivo.areaVetroTotaleMq()) + " mq)"
                + "   =   TOTALE ORDINE " + euro(preventivo.costoTotale()) + avviso);
    }

    /**
     * Le lastre da ordinare, aggregate per misura. Non seleziona il suo tab: il vetro accompagna il
     * preventivo, non lo sostituisce, e chi calcola si aspetta di trovarsi davanti quello.
     */
    private void mostraVetri(Preventivo preventivo) {
        righeVetri.setAll(preventivo.righeVetro());
        schedaVetri.setDisable(preventivo.righeVetro().isEmpty());
        totaliVetri.setText(preventivo.righeVetro().isEmpty()
                ? "Nessuna quota vetro per queste tipologie."
                : preventivo.totaleLastre() + " lastre da ordinare, "
                        + mq(preventivo.areaVetroTotaleMq()) + " mq di superficie in "
                        + preventivo.righeVetro().size() + " misure diverse"
                        + (preventivo.prezzi().alMqVetro() > 0
                                ? ", per " + euro(preventivo.costoVetro()) + "."
                                : "."));
    }

    /** Il calcolo globale: piano unico degli ordini evasi, magazzino già aggiornato. */
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
