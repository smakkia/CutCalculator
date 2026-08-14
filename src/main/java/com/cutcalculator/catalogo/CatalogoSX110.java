package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Formula;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.RegolaVetro;
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
    // 4° argomento = peso in kg/m, dalla voce "Peso|Weight kg/ml" delle schede.
    private static final Profilo TELAIO = new Profilo("SX11.138", "Telaio", Categoria.TELAIO, 2.431);
    // Condivisi con SX 120: un solo posto dove scriverne il peso (vedi ProfiliCondivisi).
    private static final Profilo TELAIO_2VIE = ProfiliCondivisi.BINARIO_2VIE;
    private static final Profilo TELAIO_3VIE = ProfiliCondivisi.BINARIO_3VIE;
    private static final Profilo TELAIO_MONTANTE = new Profilo("SX11.136", "Telaio (montante)", Categoria.TELAIO, 1.63);
    private static final Profilo ANTA = new Profilo("SX11.203", "Anta", Categoria.ANTA, 1.681);
    private static final Profilo ANTA_206 = new Profilo("SX11.206", "Anta (SX11.206)", Categoria.ANTA, 1.781);
    // Attenzione: la scheda di taglio usa .207 sul lato L e .208 sul lato H, cioe' al contrario di come
    // il Gruppo B li chiama (.207 = montante/jamb, .208 = traverso/crosspiece). Qui vale il Gruppo B.
    private static final Profilo ANTA_MONTANTE = new Profilo("SX11.207", "Anta (montante)", Categoria.ANTA, 1.679);
    private static final Profilo ANTA_TRAVERSO = new Profilo("SX11.208", "Anta (traverso)", Categoria.ANTA, 1.529);
    private static final Profilo MONTANTE = new Profilo("SX11.301", "Incontro centrale", Categoria.MONTANTE, 0.495);
    private static final Profilo NODO = new Profilo("SX11.305", "Incontro centrale per fisso", Categoria.MONTANTE, 0.485);
    private static final Profilo INCONTRO_4ANTE =
            new Profilo("SX11.303", "Incontro centrale (4 ante universale)", Categoria.MONTANTE, 0.485);

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

    /**
     * Altezza dell'anta: {@code Hₐ = H − 78}, uguale in tutte le tipologie. La larghezza dell'anta
     * ({@code Lₐ}) cambia invece da tipologia a tipologia, quindi resta scritta caso per caso.
     */
    private static Formula altezzaAnta() {
        return perH(78);
    }

    /**
     * La "distinta di taglio vetri" della scheda, che qui è quotata sull'<b>anta</b> e non sul
     * serramento: {@code Hₐ − 140} × {@code Lₐ − 140}. Le due formule dell'anta arrivano dalle stesse
     * costanti usate per tagliarla, così il vetro segue l'anta se un domani la si corregge.
     * <p>
     * La {@code quantita} è invece una <b>lastra per anta</b> (scelta dell'utente): le schede SX 110
     * stampano sempre "Q.tà 2" anche per le tipologie a 3 e 4 ante, dove è chiaramente il conteggio di
     * un nodo e non del serramento intero. In SX 120 le schede contano già per serramento (1/2/3/4).
     */
    private static List<RegolaVetro> vetro(int quantita, Formula altezzaAnta, Formula larghezzaAnta) {
        return List.of(new RegolaVetro("Vetro", altezzaAnta.meno(140), larghezzaAnta.meno(140), quantita));
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
    // Vetro: 2 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/2−143).
    private static Tipologia dueAnteFissoMobile() {
        Formula antaH = altezzaAnta();
        Formula antaL = perMezzaL(3);
        return new Tipologia("Finestra scorrevole a 2 ante (fisso + mobile)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, antaL, 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, antaH, 4, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 2, TAGLIO_90_90),
                new RegolaTaglio("Incontro centrale per fisso", NODO, perH(78), 2, TAGLIO_90_90)),
                vetro(2, antaH, antaL));
    }

    // 2 ante: telaio SX11.138 L,H ×2 (45°); anta SX11.203 L/2−3 ×4, H−78 ×4 (45°);
    // incontro centrale SX11.301 H−78 ×2 (90°).
    // Vetro: 2 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/2−143).
    private static Tipologia dueAnte() {
        Formula antaH = altezzaAnta();
        Formula antaL = perMezzaL(3);
        return new Tipologia("Finestra scorrevole a 2 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, antaL, 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, antaH, 4, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 2, TAGLIO_90_90)),
                vetro(2, antaH, antaL));
    }

    // 2 ante con anta SX11.206: telaio SX11.138 L,H ×2 (45°); anta SX11.206 L/2+2 ×4, H−78 ×4 (45°);
    // incontro centrale SX11.301 H−78 ×2 (90°).
    // Vetro: 2 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/2−138).
    private static Tipologia dueAnte206() {
        Formula antaH = altezzaAnta();
        Formula antaL = mezzaLpiu(2);
        return new Tipologia("Finestra scorrevole a 2 ante (anta SX11.206)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA_206, antaL, 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA_206, antaH, 4, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 2, TAGLIO_90_90)),
                vetro(2, antaH, antaL));
    }

    // 3 ante: telaio SX11.130 L,H ×2 (45°); anta SX11.203 L/3+22 ×6, H−78 ×6 (45°);
    // incontro centrale SX11.301 H−78 ×4 (90°).
    // Vetro: 3 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/3−118), una lastra per anta.
    private static Tipologia treAnte() {
        Formula antaH = altezzaAnta();
        Formula antaL = terzoLpiu(22);
        return new Tipologia("Finestra scorrevole a 3 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO_3VIE, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO_3VIE, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, antaL, 6, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, antaH, 6, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 4, TAGLIO_90_90)),
                vetro(3, antaH, antaL));
    }

    // 4 ante: telaio SX11.101 L,H ×2 (45°); anta SX11.203 L/2+2 ×8, H−78 ×8 (45°);
    // incontro centrale SX11.301 H−78 ×4 (90°); incontro centrale 4 ante SX11.303 H−126 ×1 (90°).
    // Vetro: 4 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/2−138), una lastra per anta.
    private static Tipologia quattroAnte() {
        Formula antaH = altezzaAnta();
        Formula antaL = mezzaLpiu(2);
        return new Tipologia("Finestra scorrevole a 4 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO_2VIE, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO_2VIE, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, antaL, 8, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, antaH, 8, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 4, TAGLIO_90_90),
                new RegolaTaglio("Incontro centrale (4 ante universale)", INCONTRO_4ANTE, perH(126), 1, TAGLIO_90_90)),
                vetro(4, antaH, antaL));
    }

    // 2 ante alternativa: telaio SX11.101 L ×2 + SX11.136 H ×2 (45°);
    // anta SX11.208 (traverso) L/3+22 ×6 + SX11.207 (montante) H−78 ×6 (45°);
    // incontro centrale SX11.301 H−78 ×4 (90°).
    // NB: i due codici sono invertiti rispetto alla scheda di taglio, che assegna .207 al lato L e
    // .208 al lato H; qui si segue il Gruppo B (.207 = montante, .208 = traverso). Da riverificare
    // sulla scheda originale se questa tipologia verra' davvero inclusa.
    // Vetro: la scheda dell'anta SX11.207/.208 detrae meno, (Hₐ−120) × (Lₐ−89) = (H−198) × (L/3−67).
    // Quantità 3: nonostante il nome "2 ante", le ante sono quotate su L/3 (×6 traversi) come la 3 ante.
    private static Tipologia dueAnteAlternativa() {
        Formula antaH = altezzaAnta();
        Formula antaL = terzoLpiu(22);
        return new Tipologia("Finestra scorrevole a 2 ante (alternativa)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO_2VIE, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO_MONTANTE, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (traverso)", ANTA_TRAVERSO, antaL, 6, TAGLIO_45_45),
                new RegolaTaglio("Anta (montante)", ANTA_MONTANTE, antaH, 6, TAGLIO_45_45),
                new RegolaTaglio("Incontro centrale", MONTANTE, perH(78), 4, TAGLIO_90_90)),
                List.of(new RegolaVetro("Vetro", antaH.meno(120), antaL.meno(89), 3)));
    }
}
