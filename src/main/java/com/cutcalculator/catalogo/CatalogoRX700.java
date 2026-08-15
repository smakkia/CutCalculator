package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.RegolaVetro;
import com.cutcalculator.dominio.Tipologia;
import com.cutcalculator.dominio.Variante;

import java.util.List;
import java.util.Map;

import static com.cutcalculator.catalogo.Formule.hMenoHF;
import static com.cutcalculator.catalogo.Formule.perH;
import static com.cutcalculator.catalogo.Formule.perHF;
import static com.cutcalculator.catalogo.Formule.perL;
import static com.cutcalculator.catalogo.Formule.perMezzaL;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_45_45;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_90;

/**
 * Dati reali del sistema <b>Twin RX 700</b> (battente, taglio termico), trascritti dalle schede di
 * taglio del Gruppo E (mappa config→scheda fornita dall'utente).
 * <p>
 * Sei tipologie: <b>finestra 1/2 ante</b> (anta {@code RX70.201} + fermavetro {@code RX70.511},
 * montante d'incontro {@code RX70.301}) e <b>porta 1/2 ante</b> (stessi profili + traverso
 * {@code RX70.402}), queste ultime in <b>due varianti</b>. Il fermavetro è una barra rettangolare
 * → taglio 90/90.
 * <p>
 * <b>Cosa non c'è.</b> Le schede riportano anche un gruppo "base" con anta {@code RX70.203} e
 * montante {@code RX60.301}, che qui è stato <b>tolto</b>: quel montante è della serie RX 600 e il
 * suo peso non compare nel Gruppo B di questo catalogo, quindi le sue barre non si potrebbero
 * valorizzare. Sparito il gruppo base, le tipologie rimaste non hanno più bisogno del suffisso
 * "(alternativo)" che le distingueva.
 * <p>
 * <b>Lo switch del traverso (montante centrale).</b> La porta non ha sempre il traverso
 * {@code RX70.402} che spezza il vetro in due riquadri, quindi ogni porta esiste in due varianti,
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
 * <b>Vetri.</b> Dalle "distinte di taglio vetri"; nelle schede le colonne sono <b>H</b> e <b>L</b>,
 * nell'ordine in cui le prende il record {@code Vetro}:
 * <ul>
 *   <li>finestra 1 anta: 1 × {@code (H−150) × (L−150)};</li>
 *   <li>finestra 2 ante: 2 × {@code (H−150) × (L/2−132)}, uno per anta;</li>
 *   <li>porta col traverso: due lastre, {@code (H−HF−58)} sopra e {@code (HF−154)} sotto, larghe
 *       {@code L−150} (1 anta) o {@code L/2−130} (2 ante);</li>
 *   <li>porta senza traverso: quelle della finestra, insieme alle sue regole di taglio.</li>
 * </ul>
 */
public final class CatalogoRX700 {

    private CatalogoRX700() {
    }

    // --- Anagrafica profili (Gruppo B): 4° argomento = peso in kg/m, dalla voce "Peso kg/ml." ---
    private static final Profilo TELAIO = new Profilo("RX70.101", "Telaio ad L piccolo", Categoria.TELAIO, 1.280);
    private static final Profilo ANTA = new Profilo("RX70.201", "Anta tonda piccola", Categoria.ANTA, 1.503);
    private static final Profilo MONTANTE = new Profilo("RX70.301", "Montante d'incontro", Categoria.MONTANTE, 1.483);
    private static final Profilo FERMAVETRO = new Profilo("RX70.511", "Fermavetro", Categoria.FERMAVETRO, 0.340);
    private static final Profilo TRAVERSO_PORTA = new Profilo("RX70.402", "Traverso porta", Categoria.TRAVERSO, 2.001);

    // --- Varianti (6° argomento del Profilo = extra kerf per estremità a 45°) -----------
    // Stessi numeri di CX 700: 24 mm di larghezza in vista in più sul maggiorato, 22 sulla forma a Z,
    // e i due assi si sommano. Il numero si paga come barra sulle diagonali e come restringimento di
    // ciò che il profilo racchiude (3° argomento della Variante).
    private static final Profilo TELAIO_L_MAGG = new Profilo("RX70.105", "Telaio ad L grande", Categoria.TELAIO, 1.669, 0, 24);
    private static final Profilo TELAIO_Z = new Profilo("RX70.102", "Telaio a Z piccolo", Categoria.TELAIO, 1.392, 0, 22);
    private static final Profilo TELAIO_Z_MAGG = new Profilo("RX70.106", "Telaio a Z grande", Categoria.TELAIO, 1.781, 0, 46);
    private static final Profilo ANTA_MAGG = new Profilo("RX70.202", "Anta tonda grande", Categoria.ANTA, 1.906, 0, 24);

    private static Map<Categoria, List<Variante>> varianti() {
        return Map.of(
                Categoria.TELAIO, List.of(
                        new Variante("L piccolo", TELAIO),
                        new Variante("L maggiorato", TELAIO_L_MAGG, 24),
                        new Variante("Z piccolo", TELAIO_Z),
                        new Variante("Z maggiorato", TELAIO_Z_MAGG, 24)),
                Categoria.ANTA, List.of(
                        new Variante("Piccola", ANTA),
                        new Variante("Maggiorata", ANTA_MAGG, 24)));
    }

    /** Il sistema RX 700 con le sue tipologie. */
    public static Sistema sistema() {
        return new Sistema("RX 700", FamigliaSistema.BATTENTE, List.of(
                finestraUnaAnta(),
                finestraDueAnte(),
                portaUnaAnta(false),
                portaUnaAnta(true),
                portaDueAnte(false),
                portaDueAnte(true)),
                varianti());
    }

    // finestra 1 anta: telaio L,H ×2 (45°); anta RX70.201 L−40/H−40 ×2 (45°);
    // fermavetro L−184/H−184 ×2 (90°). Vetro: 1 × (H−150) × (L−150).
    private static Tipologia finestraUnaAnta() {
        return new Tipologia("Finestra a 1 anta", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perL(184), 2, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184), 2, TAGLIO_90_90)),
                List.of(new RegolaVetro("Vetro", perH(150), perL(150), 1)));
    }

    // finestra 2 ante: anta RX70.201 L/2−22 ×4, H−40 ×4 (45°); fermavetro L/2−162 ×4, H−184 ×4 (90°);
    // montante RX70.301 H−110 ×1 (90°). Vetro: 2 × (H−150) × (L/2−132), uno per anta.
    private static Tipologia finestraDueAnte() {
        return new Tipologia("Finestra a 2 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(22), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(40), 4, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perMezzaL(162), 4, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184), 4, TAGLIO_90_90),
                new RegolaTaglio("Montante d'incontro", MONTANTE, perH(110), 1, TAGLIO_90_90)),
                List.of(new RegolaVetro("Vetro", perH(150), perMezzaL(132), 2)));
    }

    // porta 1 anta: anta RX70.201 L−40/H−40 ×2 (45°); fermavetro L−184 ×4, H−HF−92 ×2, HF−188 ×2 (90°);
    // traverso porta RX70.402 L−130 ×1 (90°). Vetro spezzato dal traverso: 1 × (H−HF−58) × (L−150)
    // sopra + 1 × (HF−154) × (L−150) sotto. Senza traverso il vetro è unico → vale la finestra 1 anta.
    private static Tipologia portaUnaAnta(boolean conTraverso) {
        if (!conTraverso) {
            Tipologia base = finestraUnaAnta();
            return new Tipologia("Porta a 1 anta", base.regole(), base.regoleVetro());
        }
        return new Tipologia("Porta a 1 anta (con traverso)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perL(184), 4, TAGLIO_90_90),
                // Un'estremità sola sul perimetro: dall'altra parte c'è il traverso, che non ingrossa.
                new RegolaTaglio("Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(92), 2, TAGLIO_90_90, 1),
                new RegolaTaglio("Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(188), 2, TAGLIO_90_90, 1),
                new RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perL(130), 1, TAGLIO_90_90)),
                // Idem per le lastre: in verticale un bordo va sul traverso, in orizzontale no.
                List.of(new RegolaVetro("Vetro (sopra traverso)", hMenoHF(58), perL(150), 1, 1, 2),
                        new RegolaVetro("Vetro (sotto traverso)", perHF(154), perL(150), 1, 1, 2)));
    }

    // porta 2 ante: anta RX70.201 L/2−22 ×4, H−40 ×4 (45°); fermavetro L/2−162 ×8, H−HF−92 ×4,
    // HF−188 ×4 (90°); montante RX70.301 H−110 ×1 (90°); traverso porta L/2−112 ×2 (90°).
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
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(22), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(40), 4, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perMezzaL(162), 8, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(92), 4, TAGLIO_90_90, 1),
                new RegolaTaglio("Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(188), 4, TAGLIO_90_90, 1),
                new RegolaTaglio("Montante d'incontro", MONTANTE, perH(110), 1, TAGLIO_90_90),
                new RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perMezzaL(112), 2, TAGLIO_90_90)),
                List.of(new RegolaVetro("Vetro (sopra traverso)", hMenoHF(58), perMezzaL(130), 1, 1, 2),
                        new RegolaVetro("Vetro (sotto traverso)", perHF(154), perMezzaL(130), 1, 1, 2)));
    }
}
