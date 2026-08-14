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
 * Dati reali del sistema <b>Twin CX 700</b> (battente, taglio termico), trascritti dalle schede di
 * taglio del Gruppo E (mappa config→scheda fornita dall'utente).
 * <p>
 * Sei tipologie, stessa struttura del {@link CatalogoRX700 RX 700} (cambiano gli offset):
 * <b>finestra 1/2 ante</b> (anta {@code CX70.201} + fermavetro {@code CX70.566}, montante d'incontro
 * {@code CX70.301}) e <b>porta 1/2 ante</b> (stessi profili + traverso {@code CX70.402}), queste
 * ultime in <b>due varianti</b>. Il fermavetro è una barra rettangolare → taglio 90/90.
 * <p>
 * <b>Cosa non c'è.</b> Le schede riportano anche un gruppo "base" con anta {@code CX70.203} e
 * fermavetro {@code CX70.605} <i>senza quota</i>, che qui è stato <b>tolto</b>: si tengono solo le
 * tipologie in cui il fermavetro è quotato, così la distinta è completa e il preventivo non
 * dimentica pezzi. Sparito il gruppo base, le tipologie rimaste non hanno più bisogno del suffisso
 * "(alternativo)" che le distingueva.
 * <p>
 * <b>Lo switch del traverso (montante centrale).</b> La porta non ha sempre il traverso
 * {@code CX70.402} che spezza il vetro in due riquadri, quindi ogni porta esiste in due varianti,
 * scelte come <i>tipologie distinte</i> (il modello è fatto di dati, non di flag a runtime):
 * <ul>
 *   <li><b>con</b> traverso — "Porta a 1/2 ante (con traverso)": il fermavetro verticale è spezzato
 *       in {@code H − HF − 92} e {@code HF − 188}, quindi serve l'<b>altezza parziale HF</b>;</li>
 *   <li><b>senza</b> traverso — "Porta a 1/2 ante", il caso normale, quindi senza suffisso: vetro
 *       unico, valgono <i>esattamente</i> le formule della finestra corrispondente, di cui infatti
 *       riusa le regole. Niente HF.</li>
 * </ul>
 * Le UI non hanno bisogno di sapere nulla di tutto ciò: chiedono l'HF solo se
 * {@link com.cutcalculator.dominio.Tipologia#usaHF()}, che è vero solo per la variante col traverso.
 * <p>
 * <b>Vetri.</b> Dalle "distinte di taglio vetri", identiche a quelle dell'RX 700; nelle schede le
 * colonne sono <b>H</b> e <b>L</b>, nell'ordine in cui le prende il record {@code Vetro}:
 * <ul>
 *   <li>finestra 1 anta: 1 × {@code (H−150) × (L−150)};</li>
 *   <li>finestra 2 ante: 2 × {@code (H−150) × (L/2−132)}, uno per anta;</li>
 *   <li>porta col traverso: due lastre, {@code (H−HF−58)} sopra e {@code (HF−154)} sotto, larghe
 *       {@code L−150} (1 anta) o {@code L/2−130} (2 ante);</li>
 *   <li>porta senza traverso: quelle della finestra, insieme alle sue regole di taglio.</li>
 * </ul>
 */
public final class CatalogoCX700 {

    private CatalogoCX700() {
    }

    // --- Anagrafica profili (Gruppo B): 4° argomento = peso in kg/m, dalla voce "Peso kg/ml." ---
    private static final Profilo TELAIO = new Profilo("CX70.101", "Telaio", Categoria.TELAIO, 1.287);
    private static final Profilo ANTA = new Profilo("CX70.201", "Anta", Categoria.ANTA, 1.528);
    private static final Profilo MONTANTE = new Profilo("CX70.301", "Montante d'incontro", Categoria.MONTANTE, 1.471);
    private static final Profilo FERMAVETRO = new Profilo("CX70.566", "Fermavetro", Categoria.FERMAVETRO, 0.396);
    private static final Profilo TRAVERSO_PORTA = new Profilo("CX70.402", "Traverso porta", Categoria.MONTANTE, 2.066);

    // --- Formule di comodo -------------------------------------------------------------
    private static Formula perL(double offset) {
        return new Formula(1, 0, 0, -offset);
    }

    private static Formula perMezzaL(double offset) {
        return new Formula(0.5, 0, 0, -offset);
    }

    private static Formula perH(double offset) {
        return new Formula(0, 1, 0, -offset);
    }

    /** H − HF − offset (montante del vetro sopra il traverso della porta). */
    private static Formula hMenoHF(double offset) {
        return new Formula(0, 1, -1, -offset);
    }

    /** HF − offset (montante del vetro sotto il traverso della porta). */
    private static Formula perHF(double offset) {
        return new Formula(0, 0, 1, -offset);
    }

    /** Il sistema CX 700 con le sue tipologie. */
    public static Sistema sistema() {
        return new Sistema("CX 700", FamigliaSistema.BATTENTE, List.of(
                finestraUnaAnta(),
                finestraDueAnte(),
                portaUnaAnta(false),
                portaUnaAnta(true),
                portaDueAnte(false),
                portaDueAnte(true)));
    }

    // finestra 1 anta: telaio L,H ×2 (45°); anta CX70.201 L−44/H−44 ×2 (45°);
    // fermavetro L−184/H−184 ×2 (90°). Vetro: 1 × (H−150) × (L−150).
    private static Tipologia finestraUnaAnta() {
        return new Tipologia("Finestra a 1 anta", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(44), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(44), 2, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perL(184), 2, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184), 2, TAGLIO_90_90)),
                List.of(new RegolaVetro("Vetro", perH(150), perL(150), 1)));
    }

    // finestra 2 ante: anta CX70.201 L/2−24.5 ×4, H−44 ×4 (45°); fermavetro L/2−164.5 ×4, H−184 ×4 (90°);
    // montante CX70.301 H−110 ×1 (90°). Vetro: 2 × (H−150) × (L/2−132), uno per anta.
    private static Tipologia finestraDueAnte() {
        return new Tipologia("Finestra a 2 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(24.5), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(44), 4, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perMezzaL(164.5), 4, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184), 4, TAGLIO_90_90),
                new RegolaTaglio("Montante d'incontro", MONTANTE, perH(110), 1, TAGLIO_90_90)),
                List.of(new RegolaVetro("Vetro", perH(150), perMezzaL(132), 2)));
    }

    // porta 1 anta: anta CX70.201 L−44/H−44 ×2 (45°); fermavetro L−184 ×4, H−HF−92 ×2, HF−188 ×2 (90°);
    // traverso porta CX70.402 L−130 ×1 (90°). Vetro spezzato dal traverso: 1 × (H−HF−58) × (L−150)
    // sopra + 1 × (HF−154) × (L−150) sotto. Senza traverso il vetro è unico → vale la finestra 1 anta.
    private static Tipologia portaUnaAnta(boolean conTraverso) {
        if (!conTraverso) {
            Tipologia base = finestraUnaAnta();
            return new Tipologia("Porta a 1 anta", base.regole(), base.regoleVetro());
        }
        return new Tipologia("Porta a 1 anta (con traverso)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(44), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(44), 2, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perL(184), 4, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(92), 2, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(188), 2, TAGLIO_90_90),
                new RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perL(130), 1, TAGLIO_90_90)),
                List.of(new RegolaVetro("Vetro (sopra traverso)", hMenoHF(58), perL(150), 1),
                        new RegolaVetro("Vetro (sotto traverso)", perHF(154), perL(150), 1)));
    }

    // porta 2 ante: anta CX70.201 L/2−24.5 ×4, H−44 ×4 (45°); fermavetro L/2−164.5 ×8, H−HF−92 ×4,
    // HF−188 ×4 (90°); montante CX70.301 H−110 ×1 (90°); traverso porta L/2−110.5 ×2 (90°).
    // Vetro spezzato dal traverso: 1 × (H−HF−58) × (L/2−130) sopra + 1 × (HF−154) × (L/2−130) sotto.
    // Senza traverso il vetro di ogni anta è unico → vale la finestra 2 ante.
    private static Tipologia portaDueAnte(boolean conTraverso) {
        if (!conTraverso) {
            Tipologia base = finestraDueAnte();
            return new Tipologia("Porta a 2 ante", base.regole(), base.regoleVetro());
        }
        return new Tipologia("Porta a 2 ante (con traverso)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(24.5), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(44), 4, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perMezzaL(164.5), 8, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(92), 4, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(188), 4, TAGLIO_90_90),
                new RegolaTaglio("Montante d'incontro", MONTANTE, perH(110), 1, TAGLIO_90_90),
                new RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perMezzaL(110.5), 2, TAGLIO_90_90)),
                List.of(new RegolaVetro("Vetro (sopra traverso)", hMenoHF(58), perMezzaL(130), 1),
                        new RegolaVetro("Vetro (sotto traverso)", perHF(154), perMezzaL(130), 1)));
    }
}
