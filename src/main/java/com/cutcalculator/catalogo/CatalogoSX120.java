package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Formula;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.Tipologia;

import java.util.ArrayList;
import java.util.List;

import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_45_45;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_45_DX;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_45_SX;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_90;

/**
 * Dati reali del sistema <b>Twin SX 120</b> (finestre scorrevoli, taglio termico), trascritti dalle
 * "distinte di taglio" del Gruppo E (mappa config→scheda fornita dall'utente).
 * <p>
 * Si tagliano solo i profili in alluminio con sigla <b>SX</b>/<b>RC</b>; le sigle {@code ASX}
 * (guarnizioni) e {@code BX} (plastiche anti-attrito) sono omesse. Ruoli dei profili dal <b>Gruppo B</b>:
 * telaio <b>binario</b> {@code SX11.1xx} + <b>telaio esterno</b> {@code SX12.101}/{@code .153} +
 * <b>carter</b> {@code SX12.501}; ante {@code SX12.203}/{@code .204} (e {@code .205} per l'angolo);
 * <b>incontri centrali</b> {@code SX12.301} (2/3 ante), {@code SX12.303} (4 ante), {@code SX12.304/305/306}
 * (angolo).
 * <p>
 * Sono modellate le <b>varianti base</b> di ogni configurazione; le 2 ante hanno anche la variante
 * <b>fisso + mobile</b>. Le schede riportano per 3/4 ante ulteriori varianti "Per Fisso/i + Mobile/i"
 * (piccole differenze su incontro/carter) non ancora cablate. Cut type del telaio esterno e dei nodi
 * dedotto dal disegno (montanti 90/45, traverso 45/45; carter/incontri 90/90). L'angolo della "4 ante
 * con angolo" è tagliato "poi a 45°" (qui reso 90/90, la ripresa a 45° è manuale).
 */
public final class CatalogoSX120 {

    private CatalogoSX120() {
    }

    // --- Anagrafica profili (Gruppo B) -------------------------------------------------
    private static final Profilo BINARIO_SINGOLO = new Profilo("SX11.154", "Telaio (binario singolo)", Categoria.TELAIO);
    private static final Profilo BINARIO_2VIE = new Profilo("SX11.101", "Telaio (binario 2 vie)", Categoria.TELAIO);
    private static final Profilo BINARIO_3VIE = new Profilo("SX11.130", "Telaio (binario 3 vie)", Categoria.TELAIO);
    private static final Profilo BINARIO_RIBASSATO = new Profilo("SX11.176", "Telaio (binario 2 vie ribassato)", Categoria.TELAIO);
    private static final Profilo ESTERNO = new Profilo("SX12.101", "Telaio esterno", Categoria.TELAIO);
    private static final Profilo ESTERNO_SINGOLO = new Profilo("SX12.153", "Telaio esterno (via singola)", Categoria.TELAIO);
    private static final Profilo CARTER = new Profilo("SX12.501", "Carter per telaio", Categoria.TELAIO);
    private static final Profilo ANTA = new Profilo("SX12.203", "Anta (vetro infilare)", Categoria.ANTA);
    private static final Profilo ANTA_RIDOTTA = new Profilo("SX12.204", "Anta (vetro infilare ridotta)", Categoria.ANTA);
    private static final Profilo ANTA_ANGOLO = new Profilo("SX12.205", "Anta (vetro infilare per angolo)", Categoria.ANTA);
    private static final Profilo INCONTRO = new Profilo("SX12.301", "Incontro centrale", Categoria.MONTANTE);
    private static final Profilo INCONTRO_4ANTE = new Profilo("SX12.303", "Incontro (per 4 ante)", Categoria.MONTANTE);
    private static final Profilo INCONTRO_ANGOLO = new Profilo("SX12.305", "Incontro angolo (assieme .304/.305/.306)", Categoria.MONTANTE);

    // --- Formule di comodo -------------------------------------------------------------
    private static Formula perL(double offset) {
        return new Formula(1, 0, 0, -offset);
    }

    private static Formula perMezzaL(double offset) {
        return new Formula(0.5, 0, 0, -offset);
    }

    /** L/2 + add. */
    private static Formula mezzaLpiu(double add) {
        return new Formula(0.5, 0, 0, add);
    }

    /** L/3 − offset. */
    private static Formula terzoL(double offset) {
        return new Formula(1.0 / 3.0, 0, 0, -offset);
    }

    /** L/3 + add. */
    private static Formula terzoLpiu(double add) {
        return new Formula(1.0 / 3.0, 0, 0, add);
    }

    /** L/4 − offset. */
    private static Formula quartoL(double offset) {
        return new Formula(0.25, 0, 0, -offset);
    }

    /** L/4 + add. */
    private static Formula quartoLpiu(double add) {
        return new Formula(0.25, 0, 0, add);
    }

    private static Formula perH(double offset) {
        return new Formula(0, 1, 0, -offset);
    }

    /** Il telaio esterno (2 montanti 90/45 + 1 traverso 45/45) del profilo dato. */
    private static List<RegolaTaglio> telaioEsterno(Profilo p) {
        return List.of(
                new RegolaTaglio("Telaio esterno (montante dx)", p, perH(0), 1, TAGLIO_90_45_DX),
                new RegolaTaglio("Telaio esterno (montante sx)", p, perH(0), 1, TAGLIO_90_45_SX),
                new RegolaTaglio("Telaio esterno (traverso)", p, perL(0), 1, TAGLIO_45_45));
    }

    @SafeVarargs
    private static List<RegolaTaglio> regole(List<RegolaTaglio>... blocchi) {
        List<RegolaTaglio> tutte = new ArrayList<>();
        for (List<RegolaTaglio> blocco : blocchi) {
            tutte.addAll(blocco);
        }
        return tutte;
    }

    /** Il sistema SX 120 con le sue tipologie. */
    public static Sistema sistema() {
        return new Sistema("SX 120", FamigliaSistema.SCORREVOLE, List.of(
                scomparsa(),
                dueAnteMobili(),
                dueAnteFissoMobile(),
                dueAnteRibassataMobili(),
                dueAnteRibassataFissoMobile(),
                treAnteFissoCentrale(),
                treAnte(),
                quattroAnte(),
                quattroAnteConAngolo()));
    }

    // #1 - Scomparsa: 1 anta a scomparsa (binario singolo). Ante e incontro in singola copia.
    private static Tipologia scomparsa() {
        return new Tipologia("Finestra scorrevole a scomparsa", regole(
                List.of(new RegolaTaglio("Telaio (binario)", BINARIO_SINGOLO, perL(42), 1, TAGLIO_90_90)),
                telaioEsterno(ESTERNO_SINGOLO),
                List.of(
                        new RegolaTaglio("Anta (lato orizzontale)", ANTA, mezzaLpiu(10), 1, TAGLIO_45_45),
                        new RegolaTaglio("Anta (lato verticale)", ANTA, perH(63), 1, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, perMezzaL(26), 1, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99), 1, TAGLIO_45_45),
                        new RegolaTaglio("Incontro centrale", INCONTRO, perH(76), 1, TAGLIO_90_90))));
    }

    // Aste comuni alle 2 ante "mm.110" (binario SX11.101): telaio + carter + ante.
    private static List<RegolaTaglio> comuniDueAnte() {
        return regole(
                List.of(new RegolaTaglio("Telaio (binario)", BINARIO_2VIE, perL(42), 1, TAGLIO_90_90)),
                telaioEsterno(ESTERNO),
                List.of(
                        new RegolaTaglio("Carter per telaio", CARTER, perH(94), 2, TAGLIO_90_90),
                        new RegolaTaglio("Anta (lato orizzontale)", ANTA, mezzaLpiu(10), 2, TAGLIO_45_45),
                        new RegolaTaglio("Anta (lato verticale)", ANTA, perH(63), 2, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, perMezzaL(26), 2, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99), 2, TAGLIO_45_45)));
    }

    // #2 - 2 ante mobile+mobile: comuni + incontro H−121 ×2.
    private static Tipologia dueAnteMobili() {
        return new Tipologia("Finestra scorrevole a 2 ante (mobile + mobile)", regole(comuniDueAnte(),
                List.of(new RegolaTaglio("Incontro centrale", INCONTRO, perH(121), 2, TAGLIO_90_90))));
    }

    // #2 - 2 ante fisso+mobile: comuni + incontro H−121 ×1 e H−115 ×1 + carter L/2−94 ×1.
    private static Tipologia dueAnteFissoMobile() {
        return new Tipologia("Finestra scorrevole a 2 ante (fisso + mobile)", regole(comuniDueAnte(),
                List.of(
                        new RegolaTaglio("Incontro centrale", INCONTRO, perH(121), 1, TAGLIO_90_90),
                        new RegolaTaglio("Incontro centrale", INCONTRO, perH(115), 1, TAGLIO_90_90),
                        new RegolaTaglio("Carter per telaio (fisso)", CARTER, perMezzaL(94), 1, TAGLIO_90_90))));
    }

    // Aste comuni alle 2 ante "soglia ribassata" (binario SX11.176).
    private static List<RegolaTaglio> comuniRibassata() {
        return regole(
                List.of(new RegolaTaglio("Telaio (binario)", BINARIO_RIBASSATO, perL(42), 1, TAGLIO_90_90)),
                telaioEsterno(ESTERNO),
                List.of(
                        new RegolaTaglio("Carter per telaio", CARTER, perH(67), 2, TAGLIO_90_90),
                        new RegolaTaglio("Anta (lato orizzontale)", ANTA, mezzaLpiu(10), 2, TAGLIO_45_45),
                        new RegolaTaglio("Anta (lato verticale)", ANTA, perH(36), 2, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, perMezzaL(26), 2, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(72), 2, TAGLIO_45_45)));
    }

    // #3 - 2 ante soglia ribassata mobile+mobile: comuni + incontro H−94 ×2.
    private static Tipologia dueAnteRibassataMobili() {
        return new Tipologia("Finestra scorrevole a 2 ante, soglia ribassata (mobile + mobile)", regole(comuniRibassata(),
                List.of(new RegolaTaglio("Incontro centrale", INCONTRO, perH(94), 2, TAGLIO_90_90))));
    }

    // #3 - 2 ante soglia ribassata fisso+mobile: comuni + incontro H−94 ×1 e H−88 ×1 + carter L/2−94 ×1.
    private static Tipologia dueAnteRibassataFissoMobile() {
        return new Tipologia("Finestra scorrevole a 2 ante, soglia ribassata (fisso + mobile)", regole(comuniRibassata(),
                List.of(
                        new RegolaTaglio("Incontro centrale", INCONTRO, perH(94), 1, TAGLIO_90_90),
                        new RegolaTaglio("Incontro centrale", INCONTRO, perH(88), 1, TAGLIO_90_90),
                        new RegolaTaglio("Carter per telaio (fisso)", CARTER, perMezzaL(94), 1, TAGLIO_90_90))));
    }

    // Ante + incontro comuni alle 3 ante: ante L/3+29.35 / L/3−6.65, incontro H−121 ×4.
    private static List<RegolaTaglio> anteTreAnte() {
        return List.of(
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, terzoLpiu(29.35), 3, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(63), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, terzoL(6.65), 3, TAGLIO_45_45),
                new RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99), 4, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", INCONTRO, perH(121), 4, TAGLIO_90_90));
    }

    // #4 - 3 ante fisso centrale: binario SX11.101 (2 vie); carter H−94 ×2.
    private static Tipologia treAnteFissoCentrale() {
        return new Tipologia("Finestra scorrevole a 3 ante, fisso centrale", regole(
                List.of(new RegolaTaglio("Telaio (binario)", BINARIO_2VIE, perL(42), 1, TAGLIO_90_90)),
                telaioEsterno(ESTERNO),
                List.of(new RegolaTaglio("Carter per telaio", CARTER, perH(94), 2, TAGLIO_90_90)),
                anteTreAnte()));
    }

    // #5 - 3 ante: binario SX11.130 (3 vie); carter H−94 ×4.
    private static Tipologia treAnte() {
        return new Tipologia("Finestra scorrevole a 3 ante", regole(
                List.of(new RegolaTaglio("Telaio (binario)", BINARIO_3VIE, perL(42), 1, TAGLIO_90_90)),
                telaioEsterno(ESTERNO),
                List.of(new RegolaTaglio("Carter per telaio", CARTER, perH(94), 4, TAGLIO_90_90)),
                anteTreAnte()));
    }

    // #6 - 4 ante: binario SX11.101; ante L/4+24 / L/4−12; incontro SX12.301 ×4 + SX12.303 ×2.
    private static Tipologia quattroAnte() {
        return new Tipologia("Finestra scorrevole a 4 ante", regole(
                List.of(new RegolaTaglio("Telaio (binario)", BINARIO_2VIE, perL(42), 1, TAGLIO_90_90)),
                telaioEsterno(ESTERNO),
                List.of(
                        new RegolaTaglio("Carter per telaio", CARTER, perH(94), 2, TAGLIO_90_90),
                        new RegolaTaglio("Anta (lato orizzontale)", ANTA, quartoLpiu(24), 4, TAGLIO_45_45),
                        new RegolaTaglio("Anta (lato verticale)", ANTA, perH(63), 3, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, quartoL(12), 4, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99), 5, TAGLIO_45_45),
                        new RegolaTaglio("Incontro centrale", INCONTRO, perH(121), 4, TAGLIO_90_90),
                        new RegolaTaglio("Incontro (per 4 ante)", INCONTRO_4ANTE, perH(121), 2, TAGLIO_90_90))));
    }

    // #7 - 4 ante con angolo: binario SX11.101 L−21 ×2 (poi a 45°); ante L/2−33 / L/2−69 + anta angolo
    // SX12.205; nodo angolo SX12.304/305/306. Cut angolo reso 90/90 (ripresa a 45° manuale).
    private static Tipologia quattroAnteConAngolo() {
        return new Tipologia("Finestra scorrevole a 4 ante con angolo", regole(
                List.of(new RegolaTaglio("Telaio (binario, poi a 45°)", BINARIO_2VIE, perL(21), 2, TAGLIO_90_90)),
                telaioEsterno(ESTERNO),
                List.of(
                        new RegolaTaglio("Carter per telaio", CARTER, perH(94), 2, TAGLIO_90_90),
                        new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(33), 4, TAGLIO_45_45),
                        new RegolaTaglio("Anta (lato verticale)", ANTA, perH(63), 2, TAGLIO_45_45),
                        new RegolaTaglio("Anta angolo (lato verticale)", ANTA_ANGOLO, perH(63), 2, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, perMezzaL(69), 4, TAGLIO_45_45),
                        new RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99), 4, TAGLIO_45_45),
                        new RegolaTaglio("Incontro centrale", INCONTRO, perH(121), 4, TAGLIO_90_90),
                        new RegolaTaglio("Incontro angolo", INCONTRO_ANGOLO, perH(121), 1, TAGLIO_90_90))));
    }
}
