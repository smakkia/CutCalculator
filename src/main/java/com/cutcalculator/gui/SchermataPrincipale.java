package com.cutcalculator.gui;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Unita;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Tipologia;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.ottimizzatore.Strategia;
import com.cutcalculator.pianificazione.DistintaOrdine;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.preventivo.Preventivo;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Il controller della finestra ({@code principale.fxml}): tiene insieme la barra dei menu e i tre
 * tab — <b>Magazzino</b>, <b>Ordini</b>, <b>Risultati</b> — e smista verso di loro le richieste
 * che arrivano da {@link GuiFx}.
 * <p>
 * Ogni tab è un FXML a sé ({@code fx:include}), così in SceneBuilder si apre e si modifica una
 * schermata alla volta; JavaFX inietta qui il controller di ciascuno col nome
 * {@code <fx:id>Controller}. Lo stato resta nel {@link Controller}: le schermate glielo chiedono
 * e gli delegano le modifiche, esattamente come fa la CLI.
 */
public final class SchermataPrincipale {

    @FXML private TabPane schede;
    @FXML private Tab schedaMagazzino;
    @FXML private Tab schedaOrdini;
    @FXML private Tab schedaRisultati;
    @FXML private Menu menuUnita;
    @FXML private Menu menuStrategia;

    // Iniettati da fx:include: <fx:id>Controller
    @FXML private SchermataMagazzino magazzinoController;
    @FXML private SchermataOrdini ordiniController;
    @FXML private SchermataRisultati risultatiController;

    private Controller controller;

    /** Collega lo stato dell'app e propaga il controller ai tre tab. */
    public void inizializza(Controller controller) {
        this.controller = controller;
        magazzinoController.inizializza(controller);
        ordiniController.inizializza(controller, this);
        risultatiController.inizializza(controller);
        costruisciMenuUnita();
        costruisciMenuStrategia();
        schede.getSelectionModel().select(schedaMagazzino);   // tab d'apertura, esplicito
        avvisaRigheScartate();
    }

    /**
     * Avvisa se il file ordini conteneva righe illeggibili. Da quando gli ordini si risalvano da
     * soli, tacere non basta più: la prima modifica riscriverebbe il file senza quelle righe.
     */
    private void avvisaRigheScartate() {
        int scartate = controller.righeOrdiniScartate();
        if (scartate > 0) {
            Dialoghi.errore("File ordini incompleto",
                    scartate + " righe del file ordini non sono state riconosciute (tipologia non "
                            + "più nel catalogo, o dati malformati).\n\nVerranno perse alla prima "
                            + "modifica, perché gli ordini si risalvano da soli: correggi "
                            + "dati/ordini.csv prima di continuare.");
        }
    }

    /**
     * Il menu con cui si sceglie l'unità di misura: una voce a scelta singola per ciascuna
     * ({@code mm}, {@code cm}, {@code m}), con quella corrente già spuntata.
     */
    private void costruisciMenuUnita() {
        ToggleGroup gruppo = new ToggleGroup();
        for (Unita unita : Unita.values()) {
            RadioMenuItem voce = new RadioMenuItem(unita.descrizione());
            voce.setToggleGroup(gruppo);
            voce.setSelected(unita == controller.unita());
            voce.setOnAction(evento -> cambiaUnita(unita));
            menuUnita.getItems().add(voce);
        }
    }

    /**
     * Cambia l'unità e rinfresca tutte le viste: i dati non si toccano (il modello resta in
     * millimetri), cambiano solo le intestazioni e i numeri mostrati.
     */
    private void cambiaUnita(Unita unita) {
        controller.impostaUnita(unita);
        magazzinoController.aggiornaUnita();
        ordiniController.aggiornaUnita();
        risultatiController.aggiornaUnita();
    }

    /**
     * Il menu con cui si sceglie l'<b>euristica di taglio</b>: una voce per {@link Strategia}, con la
     * corrente spuntata. Il predefinito è il multi-start, che prova più piani e tiene il migliore.
     */
    private void costruisciMenuStrategia() {
        ToggleGroup gruppo = new ToggleGroup();
        for (Strategia strategia : Strategia.values()) {
            RadioMenuItem voce = new RadioMenuItem(strategia.nome());
            voce.setToggleGroup(gruppo);
            voce.setSelected(strategia == controller.strategia());
            voce.setOnAction(evento -> cambiaStrategia(strategia));
            menuStrategia.getItems().add(voce);
        }
    }

    /**
     * Cambia l'euristica. Le viste <b>non</b> si rinfrescano: un piano già calcolato resta quello che
     * è: la scelta vale dal calcolo successivo, e riscrivere i numeri sotto gli occhi dell'utente
     * gli farebbe credere che sia stato rifatto.
     */
    private void cambiaStrategia(Strategia strategia) {
        controller.impostaStrategia(strategia);
        Dialoghi.informa("Algoritmo di ottimizzazione",
                strategia.nome() + "\n\n" + strategia.spiegazione()
                        + "\n\nVale dal prossimo calcolo: i risultati gia' mostrati restano quelli.");
    }

    // --- Voci di menu ------------------------------------------------------------------

    @FXML
    private void esci() {
        javafx.application.Platform.exit();
    }

    // Salva/carica ordini a comando non ci sono piu': gli ordini si salvano da soli a ogni
    // modifica, come il magazzino, e si ricaricano all'avvio.

    @FXML
    private void apriCatalogo() {
        mostraCatalogo(controller.catalogo());
    }

    // I prezzi non hanno più una voce di menu: si dichiarano su ogni serramento, dove si sceglie
    // anche il colore da cui dipendono.

    // --- Viste (chiamate da GuiFx) -----------------------------------------------------

    /** Elenca sistemi e tipologie disponibili: in GUI il catalogo serve da consultazione. */
    public void mostraCatalogo(Catalogo catalogo) {
        String testo = catalogo.perFamiglia().entrySet().stream()
                .map(voce -> voce.getKey() + "\n" + voce.getValue().stream()
                        .map(this::descrivi)
                        .collect(Collectors.joining("\n")))
                .collect(Collectors.joining("\n\n"));
        Dialoghi.informa("Catalogo", testo);
    }

    private String descrivi(Sistema sistema) {
        return "  " + sistema.nome() + "\n" + sistema.tipologie().stream()
                .map(Tipologia::nome)
                .map(nome -> "    - " + nome)
                .collect(Collectors.joining("\n"));
    }

    public void mostraMagazzino() {
        magazzinoController.aggiorna();
        schede.getSelectionModel().select(schedaMagazzino);
    }

    public void mostraOrdine(Ordine ordine) {
        ordiniController.seleziona(ordine);
        schede.getSelectionModel().select(schedaOrdini);
    }

    public void mostraDistinta(List<DistintaOrdine> distinte) {
        risultatiController.mostraDistinta(distinte);
        schede.getSelectionModel().select(schedaRisultati);
    }

    public void mostraPiano(PianoDiTaglio piano) {
        risultatiController.mostraPiano(piano);
        schede.getSelectionModel().select(schedaRisultati);
    }

    public void mostraSfridi(PianoDiTaglio piano) {
        risultatiController.mostraSfridi(piano);
        schede.getSelectionModel().select(schedaRisultati);
    }

    public void mostraPreventivo(Preventivo preventivo) {
        risultatiController.mostraPreventivo(preventivo);
        schede.getSelectionModel().select(schedaRisultati);
    }

    /**
     * Mostra l'esito del calcolo globale e rinfresca il magazzino: {@code evadiOrdini} lo
     * <b>consuma</b>, quindi la tabella degli avanzi non è più quella di prima.
     */
    public void mostraEvasione(EvasioneOrdini evasione) {
        risultatiController.mostraEvasione(evasione);
        magazzinoController.aggiorna();
        schede.getSelectionModel().select(schedaRisultati);
    }
}
