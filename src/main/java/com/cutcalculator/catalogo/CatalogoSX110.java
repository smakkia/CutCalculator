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
 * Sei tipologie (solo aste con formula; niente vetro/accessori):
 * <ul>
 *   <li><b>2 ante</b> (telaio {@code SX11.138}, anta {@code SX11.203});</li>
 *   <li><b>2 ante fisso + mobile</b> (come sopra + incontro centrale per fisso {@code SX11.305});</li>
 *   <li><b>2 ante</b> con anta {@code SX11.206};</li>
 *   <li><b>3 ante</b> (telaio {@code SX11.130});</li>
 *   <li><b>4 ante</b> (telaio {@code SX11.101}, incontro centrale universale {@code SX11.303});</li>
 *   <li><b>2 ante alternativa</b> (telaio {@code SX11.101}+{@code SX11.136}, ante {@code SX11.207}/{@code .208}).</li>
 * </ul>
 * I profili {@code SX11.301}/{@code SX11.303}/{@code SX11.305} sono <b>incontri centrali</b>
 * (rispettivamente 2 ante, 4 ante universale, per fisso).
 */
public final class CatalogoSX110 {

    private CatalogoSX110() {
    }

    // --- Anagrafica profili (Gruppo B) -------------------------------------------------
    private static final Profilo TELAIO = new Profilo("SX11.138", "Telaio", Categoria.TELAIO);
    private static final Profilo TELAIO_2VIE = new Profilo("SX11.101", "Telaio (binario 2 vie)", Categoria.TELAIO);
    private static final Profilo TELAIO_3VIE = new Profilo("SX11.130", "Telaio (binario 3 vie)", Categoria.TELAIO);
    private static final Profilo TELAIO_MONTANTE = new Profilo("SX11.136", "Telaio (montante)", Categoria.TELAIO);
    private static final Profilo ANTA = new Profilo("SX11.203", "Anta", Categoria.ANTA);
    private static final Profilo ANTA_206 = new Profilo("SX11.206", "Anta (SX11.206)", Categoria.ANTA);
    private static final Profilo ANTA_TRAVERSO = new Profilo("SX11.207", "Anta (traverso)", Categoria.ANTA);
    private static final Profilo ANTA_MONTANTE = new Profilo("SX11.208", "Anta (montante)", Categoria.ANTA);
    private static final Profilo MONTANTE = new Profilo("SX11.301", "Incontro centrale", Categoria.MONTANTE);
    private static final Profilo NODO = new Profilo("SX11.305", "Incontro centrale per fisso", Categoria.MONTANTE);
    private static final Profilo INCONTRO_4ANTE =
            new Profilo("SX11.303", "Incontro centrale (4 ante universale)", Categoria.MONTANTE);

    // --- Formule di comodo: lunghezza = base − offset ----------------------------------
    private static Formula perL(double offset) {
        return new Formula(1, 0, 0, -offset);
    }

    private static Formula perMezzaL(double offset) {
        return new Formula(0.5, 0, 0, -offset);
    }

    /** L/2 + add (offset positivo). */
    private static Formula mezzaLpiu(double add) {
        return new Formula(0.5, 0, 0, add);
    }

    /** L/3 + add. */
    private static Formula terzoLpiu(double add) {
        return new Formula(1.0 / 3.0, 0, 0, add);
    }

    private static Formula perH(double offset) {
        return new Formula(0, 1, 0, -offset);
    }

    /** Il sistema SX 110 con le sue tipologie. */
    public static Sistema sistema() {
        return new Sistema("SX 110", FamigliaSistema.SCORREVOLE, List.of(
                dueAnteFissoMobile(),
                dueAnte(),
                dueAnte206(),
                treAnte(),
                quattroAnte(),
                dueAnteAlternativa()));
    }

    // 2 ante fisso+mobile: telaio L,H ×2 (45°); anta L/2−3 ×4, H−78 ×4 (45°);
    // incontro centrale SX11.301 H−78 ×2 e per fisso SX11.305 H−78 ×2 (90°).
    private static Tipologia dueAnteFissoMobile() {
        return new Tipologia("Finestra scorrevole a 2 ante (fisso + mobile)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(3), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(78), 4, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 2, TAGLIO_90_90),
                new RegolaTaglio("Incontro centrale per fisso", NODO, perH(78), 2, TAGLIO_90_90)));
    }

    // 2 ante: telaio SX11.138 L,H ×2 (45°); anta SX11.203 L/2−3 ×4, H−78 ×4 (45°);
    // incontro centrale SX11.301 H−78 ×2 (90°).
    private static Tipologia dueAnte() {
        return new Tipologia("Finestra scorrevole a 2 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(3), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(78), 4, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 2, TAGLIO_90_90)));
    }

    // 2 ante con anta SX11.206: telaio SX11.138 L,H ×2 (45°); anta SX11.206 L/2+2 ×4, H−78 ×4 (45°);
    // incontro centrale SX11.301 H−78 ×2 (90°).
    private static Tipologia dueAnte206() {
        return new Tipologia("Finestra scorrevole a 2 ante (anta SX11.206)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA_206, mezzaLpiu(2), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA_206, perH(78), 4, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 2, TAGLIO_90_90)));
    }

    // 3 ante: telaio SX11.130 L,H ×2 (45°); anta SX11.203 L/3+22 ×6, H−78 ×6 (45°);
    // incontro centrale SX11.301 H−78 ×4 (90°).
    private static Tipologia treAnte() {
        return new Tipologia("Finestra scorrevole a 3 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO_3VIE, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO_3VIE, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, terzoLpiu(22), 6, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(78), 6, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 4, TAGLIO_90_90)));
    }

    // 4 ante: telaio SX11.101 L,H ×2 (45°); anta SX11.203 L/2+2 ×8, H−78 ×8 (45°);
    // incontro centrale SX11.301 H−78 ×4 (90°); incontro centrale 4 ante SX11.303 H−126 ×1 (90°).
    private static Tipologia quattroAnte() {
        return new Tipologia("Finestra scorrevole a 4 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO_2VIE, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO_2VIE, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, mezzaLpiu(2), 8, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(78), 8, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 4, TAGLIO_90_90),
                new RegolaTaglio("Incontro centrale (4 ante universale)", INCONTRO_4ANTE, perH(126), 1, TAGLIO_90_90)));
    }

    // 2 ante alternativa: telaio SX11.101 L ×2 + SX11.136 H ×2 (45°);
    // anta SX11.207 L/3+22 ×6 + SX11.208 H−78 ×6 (45°); incontro centrale SX11.301 H−78 ×4 (90°).
    private static Tipologia dueAnteAlternativa() {
        return new Tipologia("Finestra scorrevole a 2 ante (alternativa)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO_2VIE, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO_MONTANTE, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (traverso)", ANTA_TRAVERSO, terzoLpiu(22), 6, TAGLIO_45_45),
                new RegolaTaglio("Anta (montante)", ANTA_MONTANTE, perH(78), 6, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 4, TAGLIO_90_90)));
    }
}
