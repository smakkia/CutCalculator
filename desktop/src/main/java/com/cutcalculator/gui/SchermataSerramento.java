package com.cutcalculator.gui;

import com.cutcalculator.app.Etichette;
import com.cutcalculator.app.Unita;
import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.catalogo.Sistema;
import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Dimensione;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Serramento;
import com.cutcalculator.dominio.Tipologia;
import com.cutcalculator.dominio.Variante;
import com.cutcalculator.dominio.Varianti;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Il modulo "aggiungi serramento" ({@code serramento.fxml}), mostrato dentro un dialogo da
 * {@link DialogoSerramento}: scelta del sistema, della tipologia, colore, misure e quantità.
 * <p>
 * Lo stesso modulo serve anche a <b>correggere</b> un serramento già inserito: {@link #precompila}
 * riporta nei campi quello di partenza, e il dialogo restituisce la versione corretta. Le regole
 * sono le stesse — una sola schermata, così non possono divergere.
 * <p>
 * Le due ComboBox sono <b>a cascata</b> (il sistema decide le tipologie) e il campo <b>HF</b>
 * (altezza parziale) si abilita solo per le tipologie che la usano — le porte, dove il traverso
 * spezza il vetro: {@link Tipologia#usaHF()}.
 * <p>
 * Le righe delle <b>varianti</b> (telaio a Z, anta maggiorata...) non stanno nell'FXML ma si
 * costruiscono a runtime: quali ruoli abbiano alternative dipende dal sistema scelto, e chi non ne
 * ha non deve vedere niente.
 */
public final class SchermataSerramento {

    @FXML private ComboBox<Sistema> sceltaSistema;
    @FXML private ComboBox<Tipologia> sceltaTipologia;
    @FXML private TextField campoColore;
    @FXML private TextField campoL;
    @FXML private TextField campoH;
    @FXML private TextField campoHF;
    @FXML private TextField campoPrezzoKg;
    @FXML private TextField campoPrezzoMq;
    @FXML private Label etichettaL;
    @FXML private Label etichettaH;
    @FXML private Label etichettaHF;
    @FXML private Spinner<Integer> campoQuantita;
    @FXML private GridPane grigliaVarianti;

    private Unita unita = Unita.PREDEFINITA;
    /** Serve a risalire dal serramento da correggere al suo sistema, che il serramento non porta. */
    private Catalogo catalogo;
    /** Una ComboBox per ruolo con alternative, ricostruite a ogni cambio di sistema. */
    private final Map<Categoria, ComboBox<Variante>> sceltaVarianti = new EnumMap<>(Categoria.class);

    @FXML
    private void initialize() {
        campoQuantita.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        sceltaSistema.setConverter(Campi.converter(s -> s.nome() + " (" + s.famiglia() + ")"));
        sceltaTipologia.setConverter(Campi.converter(Tipologia::nome));
        sceltaSistema.valueProperty().addListener((osservabile, prima, adesso) -> tipologieDi(adesso));
        // Le varianti dipendono anche dalla tipologia (non tutte usano tutti i ruoli), e cambiando
        // sistema la tipologia si riseleziona da sola: basta stare in ascolto qui.
        sceltaTipologia.valueProperty().addListener((osservabile, prima, adesso) -> {
            abilitaHF(adesso);
            variantiDi(sceltaSistema.getValue(), adesso);
        });
    }

    /**
     * Riempie le scelte col catalogo dell'app, adotta l'unità di misura scelta dall'utente e
     * propone i prezzi dell'impostazione generale — che restano modificabili, perché il €/kg e il
     * €/mq cambiano con la finitura scelta qui accanto.
     */
    public void inizializza(Catalogo catalogo, Unita unita, Prezzi predefiniti) {
        this.unita = unita;
        this.catalogo = catalogo;
        campoPrezzoKg.setText(predefiniti.alChiloBarre() > 0
                ? String.valueOf(predefiniti.alChiloBarre()) : "");
        campoPrezzoMq.setText(predefiniti.alMqVetro() > 0
                ? String.valueOf(predefiniti.alMqVetro()) : "");
        sceltaSistema.getItems().setAll(catalogo.sistemi());
        sceltaSistema.getSelectionModel().selectFirst();
        campoL.setPromptText(unita.simbolo());
        campoH.setPromptText(unita.simbolo());
        campoHF.setPromptText("solo per le porte, in " + unita.simbolo());
        etichettaL.setText("Larghezza L (" + unita.simbolo() + ")");
        etichettaH.setText("Altezza H (" + unita.simbolo() + ")");
        etichettaHF.setText("Altezza parziale HF (" + unita.simbolo() + ")");
    }

    /**
     * Riporta nei campi un serramento già inserito, per correggerlo invece di toglierlo e rifarlo.
     * Va chiamata <b>dopo</b> {@link #inizializza}.
     * <p>
     * L'ordine conta: il sistema si sceglie per primo (il serramento non lo porta con sé, lo si
     * chiede al catalogo dalla tipologia), poi la tipologia — è il suo listener a ricostruire le
     * righe delle varianti — e solo allora si possono selezionare le varianti scelte. L'HF si scrive
     * per ultimo perché il cambio di tipologia, se non la usa, pulisce il campo.
     */
    public void precompila(Serramento serramento) {
        catalogo.sistemaDi(serramento.tipologia())
                .ifPresent(sistema -> sceltaSistema.getSelectionModel().select(sistema));
        sceltaTipologia.getSelectionModel().select(serramento.tipologia());
        serramento.varianti().scelte().forEach((ruolo, variante) -> {
            ComboBox<Variante> scelta = sceltaVarianti.get(ruolo);
            if (scelta != null) {
                scelta.getSelectionModel().select(variante);
            }
        });
        campoColore.setText(serramento.colore().nome());
        Dimensione misure = serramento.dimensione();
        campoL.setText(Etichette.misura(misure.L(), unita));
        campoH.setText(Etichette.misura(misure.H(), unita));
        campoHF.setText(misure.HF() > 0 ? Etichette.misura(misure.HF(), unita) : "");
        campoQuantita.getValueFactory().setValue(serramento.quantita());
        campoPrezzoKg.setText(serramento.prezzi().alChiloBarre() > 0
                ? String.valueOf(serramento.prezzi().alChiloBarre()) : "");
        campoPrezzoMq.setText(serramento.prezzi().alMqVetro() > 0
                ? String.valueOf(serramento.prezzi().alMqVetro()) : "");
    }

    private void tipologieDi(Sistema sistema) {
        sceltaTipologia.getItems().setAll(sistema == null ? List.of() : sistema.tipologie());
        sceltaTipologia.getSelectionModel().selectFirst();
    }

    /**
     * Ricostruisce le righe delle varianti per il sistema e la tipologia scelti: una ComboBox per
     * ogni ruolo che ha davvero più di un profilo <b>ed è usato dalla tipologia</b>, preselezionata
     * sul primo (quello base). Un elemento fisso non ha ante, e sceglierne una li accorcerebbe
     * fermavetro e vetro per un profilo che non viene mai tagliato. I sistemi che non dichiarano
     * varianti — oggi gli scorrevoli — lasciano la griglia vuota e <b>nascosta</b>
     * ({@code visible} <i>e</i> {@code managed}, altrimenti resterebbe lo spazio bianco).
     *
     * <p>Le scelte già fatte si conservano quando restano possibili: cambiare tipologia non deve far
     * ripartire dal telaio base chi lo aveva appena cambiato.
     */
    private void variantiDi(Sistema sistema, Tipologia tipologia) {
        Map<Categoria, Variante> precedenti = varianti().scelte();
        sceltaVarianti.clear();
        grigliaVarianti.getChildren().clear();
        List<Categoria> ruoli = sistema == null || tipologia == null
                ? List.of() : sistema.ruoliConScelta(tipologia);
        int riga = 0;
        for (Categoria ruolo : ruoli) {
            ComboBox<Variante> scelta = new ComboBox<>();
            scelta.setConverter(Campi.converter(Variante::nome));
            scelta.getItems().setAll(sistema.variantiDi(ruolo));
            Variante gia = precedenti.get(ruolo);
            if (gia != null && scelta.getItems().contains(gia)) {
                scelta.getSelectionModel().select(gia);
            } else {
                scelta.getSelectionModel().selectFirst();
            }
            scelta.setPrefWidth(320.0);
            grigliaVarianti.add(new Label(Etichette.ruolo(ruolo)), 0, riga);
            grigliaVarianti.add(scelta, 1, riga);
            sceltaVarianti.put(ruolo, scelta);
            riga++;
        }
        grigliaVarianti.setVisible(!ruoli.isEmpty());
        grigliaVarianti.setManaged(!ruoli.isEmpty());
    }

    /** Le varianti scelte nelle ComboBox costruite sopra. */
    private Varianti varianti() {
        Varianti scelte = Varianti.NESSUNA;
        for (ComboBox<Variante> combo : sceltaVarianti.values()) {
            Variante variante = combo.getValue();
            if (variante != null) {
                scelte = scelte.con(variante);
            }
        }
        return scelte;
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
        OptionalDouble alKg = Campi.prezzo(campoPrezzoKg);
        OptionalDouble alMq = Campi.prezzo(campoPrezzoMq);
        if (alKg.isEmpty() || alMq.isEmpty()) {
            Dialoghi.errore("Prezzi non validi", Campi.PREZZO_NON_VALIDO);
            return Optional.empty();
        }
        return Optional.of(new Serramento(tipologia, colore.get(),
                new Dimensione(l.getAsDouble(), h.getAsDouble(), hf), campoQuantita.getValue(),
                new Prezzi(alKg.getAsDouble(), alMq.getAsDouble()), varianti()));
    }
}
