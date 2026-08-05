package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Formula;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.Tipologia;

import java.util.List;

import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_45_45;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_90;

/**
 * Dati reali del sistema <b>Twin RX 700</b> (finestre a battente, taglio termico),
 * trascritti dalle schede di taglio del Gruppo E del catalogo.
 * <p>
 * Per ora solo le <b>aste</b> con formula: l'astina {@code RX70.605} (nelle schede senza
 * quota) e il vetro sono esclusi finché non avremo i loro dati/modello.
 */
public final class CatalogoRX700 {

    private CatalogoRX700() {
    }

    // --- Anagrafica profili (Gruppo B) -------------------------------------------------
    private static final Profilo TELAIO = new Profilo("RX70.101", "Telaio", Categoria.TELAIO);
    private static final Profilo ANTA = new Profilo("RX70.203", "Anta", Categoria.ANTA);
    private static final Profilo MONTANTE = new Profilo("RX60.301", "Montante d'incontro", Categoria.MONTANTE);

    // --- Formule di comodo: lunghezza = base − offset ----------------------------------
    private static Formula perL(double offset) {
        return new Formula(1, 0, 0, -offset);
    }

    private static Formula perMezzaL(double offset) {
        return new Formula(0.5, 0, 0, -offset);
    }

    private static Formula perH(double offset) {
        return new Formula(0, 1, 0, -offset);
    }

    /** Il sistema RX 700 con le sue tipologie. */
    public static Sistema sistema() {
        return new Sistema("RX 700", FamigliaSistema.BATTENTE, List.of(
                finestraUnaAnta(),
                finestraDueAnte()));
    }

    // Telaio L,H ×2 (45°); anta L−40, H−40 ×2 (45°).
    private static Tipologia finestraUnaAnta() {
        return new Tipologia("Finestra a 1 anta", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(40), 2, TAGLIO_45_45)));
    }

    // Telaio L,H ×2 (45°); anta L/2−22 ×4, H−40 ×4 (45°); montante H−110 ×1 (90°).
    private static Tipologia finestraDueAnte() {
        return new Tipologia("Finestra a 2 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(22), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(40), 4, TAGLIO_45_45),
                new RegolaTaglio("Montante d'incontro", MONTANTE, perH(110), 1, TAGLIO_90_90)));
    }
}
