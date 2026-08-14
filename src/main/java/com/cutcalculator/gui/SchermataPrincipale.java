package com.cutcalculator.gui;

import com.cutcalculator.app.Controller;
import com.cutcalculator.app.Unita;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Tipologia;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.preventivo.Preventivo;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleGroup;

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
    @FXML private MenuItem voceSalvaOrdini;
    @FXML private MenuItem voceCaricaOrdini;
    @FXML private Menu menuUnita;

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
        boolean persistenza = controller.persistenzaOrdiniAttiva();
        voceSalvaOrdini.setDisable(!persistenza);
        voceCaricaOrdini.setDisable(!persistenza);
        costruisciMenuUnita();
        schede.getSelectionModel().select(schedaMagazzino);   // tab d'apertura, esplicito
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

    // --- Voci di menu ------------------------------------------------------------------

    @FXML
    private void esci() {
        javafx.application.Platform.exit();
    }

    @FXML
    private void salvaOrdini() {
        controller.salvaOrdini();
        Dialoghi.informa("Ordini salvati",
                controller.ordini().size() + " ordini salvati su file.");
    }

    @FXML
    private void caricaOrdini() {
        boolean conferma = controller.ordini().isEmpty() || Dialoghi.conferma("Carica ordini da file",
                "Gli ordini in memoria verranno sostituiti da quelli su file. Procedo?");
        if (!conferma) {
            return;
        }
        int caricati = controller.caricaOrdini();
        ordiniController.aggiorna();
        Dialoghi.informa("Ordini caricati", caricati + " ordini caricati da file.");
    }

    @FXML
    private void apriCatalogo() {
        mostraCatalogo(controller.catalogo());
    }

    /**
     * Il listino (€/kg dell'alluminio, €/mq del vetro). Cambiandolo si rifà solo la vista dei
     * risultati: i costi sono derivati, quindi basta ricalcolarli sull'ultimo preventivo mostrato.
     */
    @FXML
    private void apriPrezzi() {
        DialogoPrezzi.chiedi(controller.prezzi()).ifPresent(prezzi -> {
            controller.impostaPrezzi(prezzi);
            risultatiController.aggiornaPrezzi();
        });
    }

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

    public void mostraDistinta(Distinta distinta) {
        risultatiController.mostraDistinta(distinta);
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
