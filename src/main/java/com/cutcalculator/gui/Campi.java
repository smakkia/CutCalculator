package com.cutcalculator.gui;

import com.cutcalculator.app.Unita;
import com.cutcalculator.dominio.Colore;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 * Letture dai campi di testo con le stesse regole della CLI: misure positive in mm che accettano
 * <b>sia la virgola sia il punto</b>, e colori come testo libero normalizzato dal {@link Colore}.
 * <p>
 * Un campo non valido non lancia: torna vuoto, e la schermata decide che messaggio mostrare.
 */
final class Campi {

    private Campi() {
    }

    /**
     * La misura scritta nel campo, <b>convertita in millimetri</b>: l'utente scrive nell'unità che
     * ha scelto, il modello ragiona sempre in mm. Accetta virgola o punto come decimale.
     */
    static OptionalDouble misura(TextField campo, Unita unita) {
        try {
            double valore = Double.parseDouble(campo.getText().trim().replace(',', '.'));
            return valore > 0 ? OptionalDouble.of(unita.versoMm(valore)) : OptionalDouble.empty();
        } catch (NumberFormatException | NullPointerException nonValida) {
            return OptionalDouble.empty();
        }
    }

    /** Il messaggio d'errore per un prezzo non valido: uno solo, condiviso da chi legge i prezzi. */
    static final String PREZZO_NON_VALIDO =
            "Inserisci un prezzo non negativo (es. 6,50), oppure 0 / vuoto per \"non impostato\".";

    /**
     * Il prezzo scritto nel campo, in euro. Come le misure accetta virgola o punto, ma a differenza
     * loro ammette lo <b>zero</b> (prezzo non impostato) e il campo <b>vuoto</b>, che vale zero.
     */
    static OptionalDouble prezzo(TextField campo) {
        String testo = campo.getText() == null ? "" : campo.getText().trim().replace(',', '.');
        if (testo.isBlank()) {
            return OptionalDouble.of(0);
        }
        try {
            double valore = Double.parseDouble(testo);
            return valore >= 0 ? OptionalDouble.of(valore) : OptionalDouble.empty();
        } catch (NumberFormatException nonValido) {
            return OptionalDouble.empty();
        }
    }

    /** Il messaggio d'errore per una misura non valida, con l'unità e un esempio coerenti. */
    static String misuraNonValida(Unita unita) {
        return "Inserisci una misura positiva in " + unita.simbolo()
                + " (es. " + unita.formatta(1500) + " o " + unita.formatta(1476.5) + ").";
    }

    /** Il colore scritto nel campo, se non è vuoto e non contiene {@code ;} (separatore del CSV). */
    static Optional<Colore> colore(TextField campo) {
        String testo = campo.getText() == null ? "" : campo.getText().trim();
        if (testo.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Colore(testo));
        } catch (IllegalArgumentException nonValido) {
            return Optional.empty();
        }
    }

    /**
     * Converter di sola andata per le ComboBox su oggetti del dominio: mostra l'etichetta scelta
     * e non prova a ricostruire l'oggetto dal testo (le ComboBox qui non sono editabili).
     */
    static <T> StringConverter<T> converter(Function<T, String> etichetta) {
        return new StringConverter<>() {
            @Override
            public String toString(T valore) {
                return valore == null ? "" : etichetta.apply(valore);
            }

            @Override
            public T fromString(String testo) {
                throw new UnsupportedOperationException("ComboBox non editabile");
            }
        };
    }
}
