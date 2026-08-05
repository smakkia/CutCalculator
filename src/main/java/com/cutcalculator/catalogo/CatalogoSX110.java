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
 * Dati reali del sistema <b>Twin SX 110</b> (finestre scorrevoli, taglio termico),
 * trascritti dalle schede di taglio del Gruppo E del catalogo.
 * <p>
 * Per ora solo le aste con formula (niente vetro/accessori). Da confermare: il ruolo
 * esatto del profilo verticale {@code SX11.305} del nodo d'incontro (qui trattato come
 * secondo montante).
 */
public final class CatalogoSX110 {

    private CatalogoSX110() {
    }

    // --- Anagrafica profili (Gruppo B) -------------------------------------------------
    private static final Profilo TELAIO = new Profilo("SX11.138", "Telaio", Categoria.TELAIO);
    private static final Profilo ANTA = new Profilo("SX11.203", "Anta", Categoria.ANTA);
    private static final Profilo MONTANTE = new Profilo("SX11.301", "Montante d'incontro", Categoria.MONTANTE);
    private static final Profilo NODO = new Profilo("SX11.305", "Nodo d'incontro", Categoria.MONTANTE);

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

    /** Il sistema SX 110 con le sue tipologie. */
    public static Sistema sistema() {
        return new Sistema("SX 110", FamigliaSistema.SCORREVOLE, List.of(
                finestraScorrevoleDueAnte()));
    }

    // Telaio L,H ×2 (45°); anta L/2−3 ×4, H−78 ×4 (45°); montante e nodo d'incontro H−78 ×2 (90°).
    private static Tipologia finestraScorrevoleDueAnte() {
        return new Tipologia("Finestra scorrevole a 2 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(3), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(78), 4, TAGLIO_45_45),
                new RegolaTaglio("Montante d'incontro", MONTANTE, perH(78), 2, TAGLIO_90_90),
                new RegolaTaglio("Nodo d'incontro", NODO, perH(78), 2, TAGLIO_90_90)));
    }
}
