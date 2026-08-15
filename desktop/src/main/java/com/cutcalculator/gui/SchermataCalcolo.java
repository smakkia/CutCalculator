package com.cutcalculator.gui;

import com.cutcalculator.app.Etichette;
import com.cutcalculator.app.Unita;
import com.cutcalculator.persistenza.CalcoloOrdine;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * I documenti di un ordine <b>già calcolato</b> ({@code calcolo.fxml}), riletti da disco e mostrati
 * dentro un dialogo da {@link DialogoCalcolo}: distinta, preventivo e vetri in tre tab, col prezzo
 * dell'ordine sempre in vista sotto.
 * <p>
 * Sono tabelle come nel tab Risultati, non testo: le stesse colonne allineate, gli stessi formati.
 * <p>
 * I dati arrivano "piatti" da {@link CalcoloOrdine} — codici e numeri, non oggetti del dominio —
 * perché sono la fotografia di un calcolo passato. Le misure si mostrano comunque nell'unità scelta
 * dall'utente: sul disco sono in millimetri come tutto il resto.
 */
public final class SchermataCalcolo {

    @FXML private Label avviso;
    @FXML private Label riepilogo;
    @FXML private Label prezzoTotale;

    @FXML private TableView<CalcoloOrdine.VoceDistinta> tabellaDistinta;
    @FXML private TableColumn<CalcoloOrdine.VoceDistinta, String> distintaProfilo;
    @FXML private TableColumn<CalcoloOrdine.VoceDistinta, String> distintaColore;
    @FXML private TableColumn<CalcoloOrdine.VoceDistinta, String> distintaLunghezza;
    @FXML private TableColumn<CalcoloOrdine.VoceDistinta, String> distintaTaglio;
    @FXML private TableColumn<CalcoloOrdine.VoceDistinta, String> distintaQuantita;

    @FXML private TableView<CalcoloOrdine.VocePreventivo> tabellaPreventivo;
    @FXML private TableColumn<CalcoloOrdine.VocePreventivo, String> prevProfilo;
    @FXML private TableColumn<CalcoloOrdine.VocePreventivo, String> prevColore;
    @FXML private TableColumn<CalcoloOrdine.VocePreventivo, String> prevLunghezza;
    @FXML private TableColumn<CalcoloOrdine.VocePreventivo, String> prevPeso;
    @FXML private TableColumn<CalcoloOrdine.VocePreventivo, String> prevCosto;

    @FXML private Tab schedaVetri;
    @FXML private TableView<CalcoloOrdine.VoceVetro> tabellaVetri;
    @FXML private TableColumn<CalcoloOrdine.VoceVetro, String> vetroAltezza;
    @FXML private TableColumn<CalcoloOrdine.VoceVetro, String> vetroLarghezza;
    @FXML private TableColumn<CalcoloOrdine.VoceVetro, String> vetroQuantita;
    @FXML private TableColumn<CalcoloOrdine.VoceVetro, String> vetroArea;
    @FXML private TableColumn<CalcoloOrdine.VoceVetro, String> vetroCosto;

    private final ObservableList<CalcoloOrdine.VoceDistinta> righeDistinta =
            FXCollections.observableArrayList();
    private final ObservableList<CalcoloOrdine.VocePreventivo> righePreventivo =
            FXCollections.observableArrayList();
    private final ObservableList<CalcoloOrdine.VoceVetro> righeVetri =
            FXCollections.observableArrayList();

    private Unita unita = Unita.PREDEFINITA;

    @FXML
    private void initialize() {
        distintaProfilo.setCellValueFactory(r -> testo(r.getValue().profilo()));
        distintaColore.setCellValueFactory(r -> testo(r.getValue().colore()));
        distintaLunghezza.setCellValueFactory(r -> testo(misura(r.getValue().lunghezza())));
        distintaTaglio.setCellValueFactory(r -> testo(taglio(r.getValue().taglio())));
        distintaQuantita.setCellValueFactory(r -> testo("x" + r.getValue().quantita()));
        allineaDestra(distintaLunghezza, distintaQuantita);
        tabellaDistinta.setItems(righeDistinta);
        tabellaDistinta.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        prevProfilo.setCellValueFactory(r -> testo(r.getValue().profilo()));
        prevColore.setCellValueFactory(r -> testo(r.getValue().colore()));
        prevLunghezza.setCellValueFactory(r -> testo(misura(r.getValue().lunghezza())));
        prevPeso.setCellValueFactory(r -> testo(numero(r.getValue().peso())));
        prevCosto.setCellValueFactory(r -> testo(euro(r.getValue().costo())));
        allineaDestra(prevLunghezza, prevPeso, prevCosto);
        tabellaPreventivo.setItems(righePreventivo);
        tabellaPreventivo.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        vetroAltezza.setCellValueFactory(r -> testo(misura(r.getValue().altezza())));
        vetroLarghezza.setCellValueFactory(r -> testo(misura(r.getValue().larghezza())));
        vetroQuantita.setCellValueFactory(r -> testo(String.valueOf(r.getValue().quantita())));
        vetroArea.setCellValueFactory(r -> testo(numero(r.getValue().areaMq())));
        vetroCosto.setCellValueFactory(r -> testo(euro(r.getValue().costo())));
        allineaDestra(vetroAltezza, vetroLarghezza, vetroQuantita, vetroArea, vetroCosto);
        tabellaVetri.setItems(righeVetri);
        tabellaVetri.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    /**
     * Riempie le tabelle.
     *
     * @param ancoraValido {@code false} se l'ordine è stato modificato dopo questo calcolo: i
     *                     documenti restano leggibili, ma non descrivono più l'ordine di adesso
     */
    public void mostra(CalcoloOrdine calcolo, boolean ancoraValido, Unita unita) {
        this.unita = unita;
        String simbolo = " (" + unita.simbolo() + ")";
        distintaLunghezza.setText("Lunghezza" + simbolo);
        prevLunghezza.setText("Profili" + simbolo);
        vetroAltezza.setText("Altezza H" + simbolo);
        vetroLarghezza.setText("Larghezza L" + simbolo);

        righeDistinta.setAll(calcolo.distinta());
        righePreventivo.setAll(calcolo.preventivo());
        righeVetri.setAll(calcolo.vetri());
        schedaVetri.setDisable(calcolo.vetri().isEmpty());

        avviso.setText(ancoraValido ? ""
                : "L'ordine e' stato modificato dopo questo calcolo: ricalcolalo per avere i "
                        + "documenti aggiornati.");
        avviso.setManaged(!ancoraValido);
        avviso.setVisible(!ancoraValido);

        riepilogo.setText(calcolo.totalePezzi() + " pezzi da tagliare  |  "
                + numero(calcolo.pesoProfili()) + " kg di alluminio " + euro(calcolo.costoProfili())
                + "  |  " + calcolo.totaleLastre() + " lastre, " + numero(calcolo.areaVetroMq())
                + " mq " + euro(calcolo.costoVetro()));
        prezzoTotale.setText("PREZZO DELL'ORDINE: " + euro(calcolo.costoTotale()));
    }

    private String misura(double mm) {
        return Etichette.misura(mm, unita);
    }

    /** {@code TAGLIO_90_45_DX} → {@code 90/45 DX}, come nelle altre viste. */
    private static String taglio(String salvato) {
        String senzaPrefisso = salvato.replace("TAGLIO_", "");
        return senzaPrefisso.replaceFirst("_", "/").replace('_', ' ');
    }

    private static String numero(double valore) {
        return String.format("%.2f", valore);
    }

    private static String euro(double importo) {
        return String.format("%.2f €", importo);
    }

    private static ReadOnlyStringWrapper testo(String valore) {
        return new ReadOnlyStringWrapper(valore);
    }

    @SafeVarargs
    private static <T> void allineaDestra(TableColumn<T, String>... colonne) {
        for (TableColumn<T, String> colonna : colonne) {
            colonna.setStyle("-fx-alignment: CENTER-RIGHT;");
        }
    }
}
