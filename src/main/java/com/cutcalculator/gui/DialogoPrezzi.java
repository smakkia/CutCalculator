package com.cutcalculator.gui;

import com.cutcalculator.dominio.Prezzi;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Il dialogo del {@link Prezzi listino}: due campi, il €/kg dell'alluminio e il €/mq del vetro.
 * <p>
 * A differenza del dialogo del serramento non ha un FXML suo: sono due caselle di testo, e un
 * sesto file da tenere allineato in SceneBuilder costerebbe più di quanto renda. Il filtro sull'OK
 * segue però la stessa regola — input non valido, dialogo che <b>resta aperto</b>.
 */
final class DialogoPrezzi {

    private DialogoPrezzi() {
    }

    /** Il listino compilato, o vuoto se l'utente annulla. */
    static Optional<Prezzi> chiedi(Prezzi attuali) {
        TextField barre = new TextField(numero(attuali.alChiloBarre()));
        TextField vetro = new TextField(numero(attuali.alMqVetro()));

        GridPane griglia = new GridPane();
        griglia.setHgap(10);
        griglia.setVgap(10);
        griglia.setPadding(new Insets(16));
        griglia.addRow(0, new Label("Alluminio (€/kg):"), barre);
        griglia.addRow(1, new Label("Vetro (€/mq):"), vetro);
        Label nota = new Label("""
                Sono prezzi tuoi, non del catalogo: cambiali quando cambia il fornitore.
                A zero valgono "non impostato" e il preventivo non mostra costi.""");
        griglia.add(nota, 0, 2, 2, 1);

        Dialog<Prezzi> dialogo = new Dialog<>();
        dialogo.setTitle("Prezzi del materiale");
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().setContent(griglia);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Prezzi[] compilato = new Prezzi[1];
        Node ok = dialogo.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, evento -> {
            OptionalDouble alChilo = Campi.prezzo(barre);
            OptionalDouble alMq = Campi.prezzo(vetro);
            if (alChilo.isEmpty() || alMq.isEmpty()) {
                Dialoghi.errore("Prezzi non validi", Campi.PREZZO_NON_VALIDO);
                evento.consume();
                return;
            }
            compilato[0] = new Prezzi(alChilo.getAsDouble(), alMq.getAsDouble());
        });
        dialogo.setResultConverter(bottone -> bottone == ButtonType.OK ? compilato[0] : null);
        return dialogo.showAndWait();
    }

    /** Il valore di partenza nel campo: senza decimali inutili, e vuoto se non impostato. */
    private static String numero(double prezzo) {
        return prezzo > 0 ? String.valueOf(prezzo) : "";
    }
}
