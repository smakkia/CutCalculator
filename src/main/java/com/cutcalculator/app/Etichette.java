package com.cutcalculator.app;

import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.TipoTaglio;

import java.util.Locale;

/**
 * Le etichette con cui profili, materiali, misure e tagli si mostrano all'utente — le stesse per
 * il client testuale e per quello grafico, così le due UI "parlano" allo stesso modo e non si
 * ricopiano i formatter.
 * <p>
 * Solo testo neutro: niente cornici ASCII (quelle restano nella CLI) e niente widget.
 */
public final class Etichette {

    private Etichette() {
    }

    /** Profilo: {@code "RX70.101 Telaio"}. */
    public static String profilo(Profilo profilo) {
        return profilo.codice() + " " + profilo.descrizione();
    }

    /** Materiale (profilo + colore): {@code "RX70.101 Telaio [BIANCO]"}. */
    public static String materiale(Materiale materiale) {
        return profilo(materiale.profilo()) + " [" + materiale.colore().nome() + "]";
    }

    /**
     * Una misura del modello (sempre in mm) nell'unità scelta dall'utente, senza simbolo:
     * {@code "6500"}, {@code "650"}, {@code "6.5"}.
     */
    public static String misura(double mm, Unita unita) {
        return unita.formatta(mm);
    }

    /** Come {@link #misura}, ma col simbolo dell'unità: {@code "6.5 m"}. */
    public static String misuraConSimbolo(double mm, Unita unita) {
        return unita.conSimbolo(mm);
    }

    /** Un conteggio o un numero puro (non una misura): niente conversione di unità. */
    public static String numero(double valore) {
        return Math.rint(valore) == valore
                ? String.format(Locale.ROOT, "%.0f", valore)
                : String.format(Locale.ROOT, "%.1f", valore);
    }

    /** Tipo di taglio in forma breve: {@code "90/45 DX"}. */
    public static String taglio(TipoTaglio taglio) {
        return switch (taglio) {
            case TAGLIO_45_45 -> "45/45";
            case TAGLIO_90_90 -> "90/90";
            case TAGLIO_90_45_DX -> "90/45 DX";
            case TAGLIO_90_45_SX -> "90/45 SX";
        };
    }
}
