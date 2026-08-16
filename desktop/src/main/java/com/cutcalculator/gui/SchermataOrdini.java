package com.cutcalculator.gui;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Etichette;
import com.cutcalculator.app.Unita;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Dimensione;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.persistenza.CalcoloOrdine;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Il tab <b>Ordini</b>: a sinistra le commesse, a destra i serramenti di quella scelta.
 * <p>
 * Le commesse stanno in <b>due elenchi</b>: quelle <i>da calcolare</i> e quelle <i>calcolate</i>,
 * cioè già evase da un calcolo globale che ne ha scalato il materiale. La selezione è una sola:
 * scegliendo in un elenco l'altro si deseleziona, così "il serramento di quale ordine" non è mai
 * ambiguo.
 * <p>
 * Un solo calcolo: <b>Calcola gli ordini da calcolare</b> unisce le commesse non ancora evase in un
 * piano unico (i pezzi di ordini diversi condividono le barre), <b>scarica il magazzino</b> — per
 * cui chiede conferma — e le sposta tra i calcolati. Il risultato finisce nel tab Risultati.
 */
public final class SchermataOrdini {

    @FXML private ListView<Ordine> elencoDaCalcolare;
    @FXML private ListView<Ordine> elencoCalcolati;
    @FXML private Button bottoneCalcola;
    @FXML private Button bottoneRipristina;
    @FXML private Label riepilogo;
    @FXML private TableView<Serramento> tabellaSerramenti;
    @FXML private TableColumn<Serramento, String> colonnaSistema;
    @FXML private TableColumn<Serramento, String> colonnaTipologia;
    @FXML private TableColumn<Serramento, String> colonnaColore;
    @FXML private TableColumn<Serramento, String> colonnaL;
    @FXML private TableColumn<Serramento, String> colonnaH;
    @FXML private TableColumn<Serramento, String> colonnaHF;
    @FXML private TableColumn<Serramento, String> colonnaQuantita;
    @FXML private TableColumn<Serramento, String> colonnaVarianti;

    private final ObservableList<Ordine> daCalcolare = FXCollections.observableArrayList();
    private final ObservableList<Ordine> calcolati = FXCollections.observableArrayList();
    private final ObservableList<Serramento> serramenti = FXCollections.observableArrayList();
    private Controller controller;
    private SchermataPrincipale principale;

    @FXML
    private void initialize() {
        preparaElenco(elencoDaCalcolare, daCalcolare, "Niente da calcolare.", elencoCalcolati);
        preparaElenco(elencoCalcolati, calcolati, "Nessun ordine calcolato.", elencoDaCalcolare);

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
        // Forma "breve": senza parentesi quadre, perché qui è una colonna sua.
        colonnaVarianti.setCellValueFactory(riga ->
                new ReadOnlyStringWrapper(Etichette.variantiBrevi(riga.getValue().varianti())));
        for (TableColumn<Serramento, String> colonna :
                List.of(colonnaL, colonnaH, colonnaHF, colonnaQuantita)) {
            colonna.setStyle("-fx-alignment: CENTER-RIGHT;");
        }
        tabellaSerramenti.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabellaSerramenti.setItems(serramenti);
        tabellaSerramenti.setPlaceholder(new Label("Ordine vuoto: aggiungi un serramento."));
    }

    /**
     * Uno dei due elenchi: stessa resa delle celle e selezione <b>mutuamente esclusiva</b> con
     * l'altro, così l'ordine scelto è sempre uno solo.
     */
    private void preparaElenco(ListView<Ordine> elenco, ObservableList<Ordine> contenuto,
            String seVuoto, ListView<Ordine> gemello) {
        elenco.setItems(contenuto);
        elenco.setPlaceholder(new Label(seVuoto));
        elenco.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Ordine ordine, boolean vuota) {
                super.updateItem(ordine, vuota);
                setText(vuota || ordine == null ? null
                        : ordine.nome() + "  (" + ordine.totaleSerramenti() + " serramenti)");
            }
        });
        elenco.getSelectionModel().selectedItemProperty()
                .addListener((osservabile, prima, adesso) -> {
                    if (adesso != null) {
                        gemello.getSelectionModel().clearSelection();
                        mostraSerramentiDi(adesso);
                    }
                });
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
        Ordine selezionato = selezionato();
        daCalcolare.setAll(controller.ordiniDaCalcolare());
        calcolati.setAll(controller.ordiniCalcolati());
        if (selezionato != null) {
            seleziona(selezionato);
        } else {
            elencoDaCalcolare.getSelectionModel().selectFirst();
        }
        mostraSerramentiDi(selezionato());
        int totale = daCalcolare.size() + calcolati.size();
        riepilogo.setText(totale == 0
                ? "Nessun ordine."
                : totale + " ordini: " + daCalcolare.size() + " da calcolare, "
                        + calcolati.size() + " calcolati.");
        bottoneCalcola.setDisable(daCalcolare.isEmpty());
    }

    /** Porta in primo piano l'ordine dato, nell'elenco in cui si trova adesso. */
    public void seleziona(Ordine ordine) {
        ListView<Ordine> elenco = ordine.calcolato() ? elencoCalcolati : elencoDaCalcolare;
        if (!elenco.getItems().contains(ordine)) {
            aggiorna();
            return;
        }
        elenco.getSelectionModel().select(ordine);
    }

    private void mostraSerramentiDi(Ordine ordine) {
        serramenti.setAll(ordine == null ? List.of() : ordine.serramenti());
        // "Ripristina" ha senso solo sull'ultimo calcolo: degli altri non si sa piu' da dove
        // partiva il magazzino, quindi il bottone resta spento invece di illudere.
        bottoneRipristina.setDisable(ordine == null || !controller.ripristinabile(ordine));
    }

    /** L'ordine selezionato in uno dei due elenchi, o {@code null} se non ce n'è nessuno. */
    private Ordine selezionato() {
        Ordine daFare = elencoDaCalcolare.getSelectionModel().getSelectedItem();
        return daFare != null ? daFare : elencoCalcolati.getSelectionModel().getSelectedItem();
    }

    /** L'ordine su cui agire, o vuoto (con messaggio) se non ce n'è uno selezionato. */
    private Optional<Ordine> scelto() {
        Ordine ordine = selezionato();
        if (ordine == null) {
            Dialoghi.errore("Nessun ordine scelto", "Seleziona un ordine da uno dei due elenchi.");
        }
        return Optional.ofNullable(ordine);
    }

    /** Rinfresca le celle dei due elenchi (nomi e conteggi dei serramenti). */
    private void rinfrescaElenchi() {
        elencoDaCalcolare.refresh();
        elencoCalcolati.refresh();
    }

    // --- Azioni sugli ordini -----------------------------------------------------------

    @FXML
    private void nuovoOrdine() {
        chiediNome("Nuovo ordine", controller.nomeOrdineProposto(), null)
                .ifPresent(nome -> {
                    Ordine ordine = controller.nuovoOrdine(nome);
                    aggiorna();
                    seleziona(ordine);
                });
    }

    @FXML
    private void rinominaOrdine() {
        scelto().ifPresent(ordine -> chiediNome("Rinomina ordine", ordine.nome(), ordine)
                .ifPresent(nome -> {
                    controller.rinominaOrdine(ordine, nome);
                    rinfrescaElenchi();
                }));
    }

    @FXML
    private void rimuoviOrdine() {
        scelto().ifPresent(ordine -> {
            // Un ordine calcolato ha gia' scalato il suo materiale: cancellarlo non lo rimette a
            // magazzino, e porta via anche la possibilita' di annullare quel calcolo.
            String avviso = ordine.calcolato()
                    ? "\n\nL'ordine e' gia' stato calcolato: il materiale gia' scalato dal magazzino "
                            + "non torna indietro, e il calcolo non si potra' piu' annullare."
                    : "";
            int righe = ordine.serramenti().size();
            boolean conferma = Dialoghi.conferma("Rimuovi ordine",
                    (righe == 0
                            ? "Rimuovo l'ordine vuoto \"" + ordine.nome() + "\"?"
                            : "Rimuovo l'ordine \"" + ordine.nome() + "\" con i suoi " + righe
                                    + " serramenti?")
                            + "\n\nNon si torna indietro." + avviso);
            if (conferma) {
                controller.rimuoviOrdine(ordine);
                aggiorna();
            }
        });
    }

    /**
     * Chiede un nome d'ordine valido: non vuoto, senza {@code ;} (separatore del CSV) e <b>non già
     * usato</b> — gli ordini si rileggono da disco per nome, quindi due omonimi si fonderebbero in
     * uno solo al prossimo avvio, coi serramenti mescolati.
     *
     * @param tranne l'ordine che quel nome ce l'ha già (nel rinomina), oppure {@code null}
     */
    private Optional<String> chiediNome(String titolo, String iniziale, Ordine tranne) {
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
            } else if (!controller.nomeLibero(nome, tranne)) {
                Dialoghi.errore("Nome gia' usato",
                        "C'e' gia' un ordine chiamato \"" + nome + "\".\n\nGli ordini si "
                                + "ricaricano da disco per nome: due omonimi si fonderebbero in uno "
                                + "solo al prossimo avvio.");
            } else {
                return Optional.of(nome);
            }
        }
    }

    // --- Azioni sui serramenti ---------------------------------------------------------

    /**
     * Avvisa prima di toccare i serramenti di un ordine <b>già calcolato</b>: modificarlo lo rimette
     * fra quelli da calcolare, ma il prossimo calcolo lo ritaglia <b>da capo</b> e conta una seconda
     * volta il materiale già scalato dal magazzino. La via pulita è il ripristino, che quel calcolo
     * lo annulla davvero — se è ancora l'ultimo, il messaggio lo dice.
     *
     * @return {@code false} se l'utente ha preferito non modificare
     */
    private boolean confermaModificaDiCalcolato(Ordine ordine) {
        if (!ordine.calcolato()) {
            return true;
        }
        String consiglio = controller.ripristinabile(ordine)
                ? "\n\nMeglio prima \"Ripristina ordine\": annulla quel calcolo e rimette a posto "
                        + "il magazzino."
                : "";
        return Dialoghi.conferma("Ordine gia' calcolato",
                "\"" + ordine.nome() + "\" e' gia' stato calcolato: il suo materiale e' gia' stato "
                        + "scalato dal magazzino.\n\nModificandolo tornera' fra quelli da calcolare, "
                        + "ma il prossimo calcolo lo ritagliera' da capo, contando una seconda volta "
                        + "il materiale gia' evaso." + consiglio + "\n\nModificare comunque?");
    }

    @FXML
    private void aggiungiSerramento() {
        scelto().filter(this::confermaModificaDiCalcolato).ifPresent(ordine -> DialogoSerramento
                .chiedi(controller.catalogo(), unita(), prezziProposti(ordine))
                .ifPresent(serramento -> {
                    controller.aggiungiSerramento(ordine, serramento);
                    // Toccare i serramenti rimette l'ordine tra quelli da calcolare: puo' quindi
                    // aver cambiato elenco, e va ricostruita la divisione.
                    aggiorna();
                    seleziona(ordine);
                    mostraSerramentiDi(ordine);
                }));
    }

    /**
     * I prezzi da precompilare nel dialogo: quelli dell'<b>ultimo serramento</b> dell'ordine, che
     * di solito è dello stesso colore e quindi allo stesso prezzo. Sul primo serramento i campi
     * restano vuoti: meglio farli digitare che proporre un numero preso da chissà dove.
     */
    private static Prezzi prezziProposti(Ordine ordine) {
        List<Serramento> serramenti = ordine.serramenti();
        return serramenti.isEmpty()
                ? Prezzi.NESSUNO
                : serramenti.get(serramenti.size() - 1).prezzi();
    }

    @FXML
    private void rimuoviSerramento() {
        agisciSulSerramento("Seleziona la riga da rimuovere.", (ordine, indice, serramento) -> {
            // La riga scelta si rilegge nel messaggio: la tabella si puo' ordinare per colonna, e
            // togliere il serramento sbagliato e' un errore che si scopre solo al calcolo.
            if (!Dialoghi.conferma("Rimuovi serramento",
                    "Rimuovo dall'ordine \"" + ordine.nome() + "\":\n\n" + riassunto(serramento)
                            + "\n\nNon si torna indietro.")) {
                return;
            }
            controller.rimuoviSerramento(ordine, indice);
            aggiorna();            // l'ordine torna tra quelli da calcolare
            seleziona(ordine);
            mostraSerramentiDi(ordine);
        });
    }

    /** Un serramento in una riga sola, per i messaggi di conferma. */
    private String riassunto(Serramento serramento) {
        Dimensione misure = serramento.dimensione();
        return serramento.tipologia().nome() + " [" + serramento.colore().nome() + "]  "
                + Etichette.misura(misure.L(), unita()) + " x "
                + Etichette.misuraConSimbolo(misure.H(), unita())
                + (misure.HF() > 0 ? " (HF " + Etichette.misura(misure.HF(), unita()) + ")" : "")
                + "  x" + serramento.quantita() + Etichette.varianti(serramento.varianti());
    }

    /**
     * Corregge il serramento scelto invece di toglierlo e rifarlo: il dialogo parte dai suoi valori
     * e la conferma lo <b>sostituisce</b> al suo posto, misure, colore, quantità, prezzi e varianti
     * comprese. L'ordine torna fra quelli da calcolare, come per ogni altra modifica.
     */
    @FXML
    private void modificaSerramento() {
        agisciSulSerramento("Seleziona la riga da modificare.", (ordine, indice, serramento) ->
                DialogoSerramento.modifica(controller.catalogo(), unita(), serramento)
                        .ifPresent(corretto -> {
                            controller.sostituisciSerramento(ordine, indice, corretto);
                            aggiorna();
                            seleziona(ordine);
                            mostraSerramentiDi(ordine);
                        }));
    }

    /**
     * Il preambolo comune a "rimuovi" e "modifica": ci vuole una riga scelta e un ordine scelto, e
     * se l'ordine è già calcolato ci vuole la conferma. L'indice si ricava <b>cercando l'oggetto</b>
     * e non dalla riga di tabella, che l'ordinamento per colonna scollerebbe.
     */
    private void agisciSulSerramento(String seNessuno, AzioneSuSerramento azione) {
        Serramento selezionato = tabellaSerramenti.getSelectionModel().getSelectedItem();
        if (selezionato == null) {
            Dialoghi.errore("Nessun serramento scelto", seNessuno);
            return;
        }
        scelto().filter(this::confermaModificaDiCalcolato).ifPresent(ordine -> {
            int indice = ordine.serramenti().indexOf(selezionato);
            if (indice < 0) {
                // La tabella mostrava una versione superata dell'ordine: meglio rinfrescarla che
                // passare un -1 al controller, che finirebbe in IndexOutOfBoundsException.
                Dialoghi.errore("Riga non piu' valida",
                        "Il serramento non fa piu' parte dell'ordine: la tabella e' stata aggiornata.");
                aggiorna();
                mostraSerramentiDi(ordine);
                return;
            }
            azione.esegui(ordine, indice, selezionato);
        });
    }

    /** Cosa fare al serramento scelto, una volta trovato il suo posto nell'ordine. */
    @FunctionalInterface
    private interface AzioneSuSerramento {
        void esegui(Ordine ordine, int indice, Serramento serramento);
    }

    /**
     * I documenti dell'ultimo calcolo dell'ordine scelto — distinta, preventivo, vetri e prezzo —
     * riletti da disco: si vedono anche quelli di sessioni precedenti, senza ricalcolare nulla.
     */
    @FXML
    private void vediCalcolo() {
        scelto().ifPresent(ordine -> {
            CalcoloOrdine calcolo = controller.calcoloDi(ordine);
            if (calcolo.vuoto()) {
                Dialoghi.informa("Nessun calcolo", ordine.calcolato()
                        ? "Di quest'ordine non ci sono documenti salvati."
                        : "Ordine non ancora calcolato: usa \"Calcola gli ordini da calcolare\".");
                return;
            }
            DialogoCalcolo.mostra(calcolo, ordine.nome(), ordine.calcolato(), unita());
        });
    }

    /**
     * Annulla l'ultimo calcolo: il magazzino torna esattamente com'era prima e gli ordini di quel
     * calcolo tornano fra quelli da calcolare. Riguarda <b>tutto il gruppo</b> — le barre erano
     * condivise, quindi la parte di magazzino di un ordine solo non esiste — e vale solo per il
     * calcolo più recente.
     */
    @FXML
    private void ripristinaOrdine() {
        scelto().ifPresent(ordine -> {
            if (!controller.ripristinabile(ordine)) {
                List<String> ultimo = controller.ordiniRipristinabili();
                Dialoghi.errore("Ripristino non disponibile", ultimo.isEmpty()
                        ? "Non c'e' nessun calcolo da annullare."
                        : "Si annulla solo l'ultimo calcolo, che riguardava: "
                                + String.join(", ", ultimo) + ".\n\nDegli altri non si sa piu' "
                                + "com'era il magazzino di partenza.");
                return;
            }
            String gruppo = String.join(", ", controller.ordiniRipristinabili());
            boolean conferma = Dialoghi.conferma("Ripristina ordine",
                    "Tornano fra gli ordini da calcolare: " + gruppo + ".\n\n"
                            + "Il magazzino torna esattamente com'era prima di quel calcolo: gli "
                            + "avanzi usati tornano disponibili e i ritagli rientrati spariscono.\n\n"
                            + "Si perdono le modifiche fatte a mano al magazzino dopo il calcolo, e "
                            + "i documenti (distinta, preventivo, vetri) di quegli ordini. Procedo?");
            if (!conferma) {
                return;
            }
            List<String> tornati = controller.ripristina(ordine);
            aggiorna();
            seleziona(ordine);
            principale.mostraMagazzinoAggiornato();
            Dialoghi.informa("Ripristino eseguito",
                    "Tornati da calcolare: " + String.join(", ", tornati)
                            + ".\n\nMagazzino riportato a " + controller.totaleAvanzi()
                            + " spezzoni.");
        });
    }

    // --- Calcolo -----------------------------------------------------------------------
    // Il "calcolo provvisorio" del singolo ordine e' stato tolto: mostrava un piano che il calcolo
    // globale poi rifaceva diverso, perche' li' gli ordini si uniscono e condividono le barre.

    @FXML
    private void calcolaTutti() {
        List<Ordine> daFare = controller.ordiniDaCalcolare();
        if (daFare.isEmpty()) {
            Dialoghi.errore("Niente da calcolare", controller.ordini().isEmpty()
                    ? "Crea almeno un ordine."
                    : "Gli ordini sono gia' stati calcolati tutti.");
            return;
        }
        if (daFare.stream().allMatch(ordine -> ordine.serramenti().isEmpty())) {
            Dialoghi.errore("Ordini vuoti", "Aggiungi almeno un serramento agli ordini da calcolare.");
            return;
        }
        String elenco = daFare.stream().map(Ordine::nome).collect(Collectors.joining(", "));
        boolean conferma = Dialoghi.conferma("Calcola gli ordini da calcolare",
                "Vengono calcolati insieme, in un piano di taglio unico: " + elenco + ".\n\n"
                        + "Il magazzino viene aggiornato (gli avanzi usati sono consumati e i "
                        + "ritagli sopra "
                        + Etichette.misuraConSimbolo(controller.sogliaRitaglio(), unita())
                        + " rientrano) e gli ordini passano tra i calcolati. Procedo?");
        if (!conferma) {
            return;
        }
        try {
            EvasioneOrdini evasione = controller.evadiOrdini();
            aggiorna();
            principale.mostraEvasione(evasione);
        } catch (IllegalArgumentException nonCalcolabile) {
            Dialoghi.errore("Calcolo non riuscito", nonCalcolabile);
        }
    }
}
