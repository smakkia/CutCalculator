package com.cutcalculator.catalogo

import com.cutcalculator.catalogo.Formule.hMenoHF
import com.cutcalculator.catalogo.Formule.perH
import com.cutcalculator.catalogo.Formule.perHF
import com.cutcalculator.catalogo.Formule.perL
import com.cutcalculator.catalogo.Formule.perMezzaL
import com.cutcalculator.dominio.Categoria
import com.cutcalculator.dominio.Profilo
import com.cutcalculator.dominio.RegolaTaglio
import com.cutcalculator.dominio.RegolaVetro
import com.cutcalculator.dominio.TipoTaglio.TAGLIO_45_45
import com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_90
import com.cutcalculator.dominio.Tipologia
import com.cutcalculator.dominio.Variante

/**
 * Dati reali del sistema **Twin RX 700** (battente, taglio termico), trascritti dalle schede di
 * taglio del Gruppo E (mappa config→scheda fornita dall'utente).
 *
 * Sei tipologie: **finestra 1/2 ante** (anta `RX70.201` + fermavetro `RX70.511`, montante d'incontro
 * `RX70.301`) e **porta 1/2 ante** (stessi profili + traverso `RX70.402`), queste ultime in **due
 * varianti**. Il fermavetro è una barra rettangolare → taglio 90/90.
 *
 * **Cosa non c'è.** Le schede riportano anche un gruppo "base" con anta `RX70.203` e montante
 * `RX60.301`, che qui è stato **tolto**: quel montante è della serie RX 600 e il suo peso non compare
 * nel Gruppo B di questo catalogo, quindi le sue barre non si potrebbero valorizzare. Sparito il
 * gruppo base, le tipologie rimaste non hanno più bisogno del suffisso "(alternativo)" che le
 * distingueva.
 *
 * **Lo switch del traverso (montante centrale).** La porta non ha sempre il traverso `RX70.402` che
 * spezza il vetro in due riquadri, quindi ogni porta esiste in due varianti, scelte come *tipologie
 * distinte* (il modello è fatto di dati, non di flag a runtime):
 * - **con** traverso — "Porta a 1/2 ante (con traverso)": il fermavetro verticale è spezzato in
 *   `H − HF − 92` e `HF − 188`, quindi serve l'**altezza parziale HF**;
 * - **senza** traverso — "Porta a 1/2 ante", il caso normale, quindi senza suffisso: vetro unico,
 *   valgono *esattamente* le formule della finestra corrispondente, di cui infatti riusa le regole.
 *   Niente HF.
 *
 * Le UI non hanno bisogno di sapere nulla di tutto ciò: chiedono l'HF solo se [Tipologia.usaHF], che
 * è vero solo per la variante col traverso.
 *
 * **Vetri.** Dalle "distinte di taglio vetri"; nelle schede le colonne sono **H** e **L**, nell'ordine
 * in cui le prende il record `Vetro`:
 * - finestra 1 anta: 1 × `(H−150) × (L−150)`;
 * - finestra 2 ante: 2 × `(H−150) × (L/2−132)`, uno per anta;
 * - porta col traverso: due lastre, `(H−HF−58)` sopra e `(HF−154)` sotto, larghe `L−150` (1 anta) o
 *   `L/2−130` (2 ante);
 * - porta senza traverso: quelle della finestra, insieme alle sue regole di taglio.
 */
object CatalogoRX700 {

    // --- Anagrafica profili (Gruppo B): 4° argomento = peso in kg/m, dalla voce "Peso kg/ml." ---
    private val TELAIO = Profilo("RX70.101", "Telaio ad L piccolo", Categoria.TELAIO, 1.280)
    private val ANTA = Profilo("RX70.201", "Anta tonda piccola", Categoria.ANTA, 1.503)
    private val MONTANTE = Profilo("RX70.301", "Montante d'incontro", Categoria.MONTANTE, 1.483)
    private val FERMAVETRO = Profilo("RX70.511", "Fermavetro", Categoria.FERMAVETRO, 0.340)
    private val TRAVERSO_PORTA = Profilo("RX70.402", "Traverso porta", Categoria.TRAVERSO, 2.001)

    // --- Varianti (6° argomento del Profilo = extra kerf per estremità a 45°) -----------
    // Stessi numeri di CX 700: 24 mm di larghezza in vista in più sul maggiorato, 22 sulla forma a Z,
    // e i due assi si sommano. Il numero si paga come barra sulle diagonali e come restringimento di
    // ciò che il profilo racchiude (3° argomento della Variante).
    private val TELAIO_L_MAGG =
        Profilo("RX70.105", "Telaio ad L grande", Categoria.TELAIO, 1.669, 0.0, 24.0)
    private val TELAIO_Z =
        Profilo("RX70.102", "Telaio a Z piccolo", Categoria.TELAIO, 1.392, 0.0, 22.0)
    private val TELAIO_Z_MAGG =
        Profilo("RX70.106", "Telaio a Z grande", Categoria.TELAIO, 1.781, 0.0, 46.0)
    private val ANTA_MAGG =
        Profilo("RX70.202", "Anta tonda grande", Categoria.ANTA, 1.906, 0.0, 24.0)

    private fun varianti(): Map<Categoria, List<Variante>> = mapOf(
        Categoria.TELAIO to listOf(
            Variante("L piccolo", TELAIO),
            Variante("L maggiorato", TELAIO_L_MAGG, 24.0),
            Variante("Z piccolo", TELAIO_Z),
            Variante("Z maggiorato", TELAIO_Z_MAGG, 24.0)
        ),
        Categoria.ANTA to listOf(
            Variante("Piccola", ANTA),
            Variante("Maggiorata", ANTA_MAGG, 24.0)
        )
    )

    /** Il sistema RX 700 con le sue tipologie. */
    fun sistema(): Sistema = Sistema(
        "RX 700", FamigliaSistema.BATTENTE,
        listOf(
            finestraUnaAnta(),
            finestraDueAnte(),
            portaUnaAnta(false),
            portaUnaAnta(true),
            portaDueAnte(false),
            portaDueAnte(true)
        ),
        varianti()
    )

    // finestra 1 anta: telaio L,H ×2 (45°); anta RX70.201 L−40/H−40 ×2 (45°);
    // fermavetro L−184/H−184 ×2 (90°). Vetro: 1 × (H−150) × (L−150).
    private fun finestraUnaAnta(): Tipologia = Tipologia(
        "Finestra a 1 anta",
        listOf(
            RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(40.0), 2, TAGLIO_45_45),
            RegolaTaglio("Anta (lato verticale)", ANTA, perH(40.0), 2, TAGLIO_45_45),
            RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perL(184.0), 2, TAGLIO_90_90),
            RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184.0), 2, TAGLIO_90_90)
        ),
        listOf(RegolaVetro("Vetro", perH(150.0), perL(150.0), 1))
    )

    // finestra 2 ante: anta RX70.201 L/2−22 ×4, H−40 ×4 (45°); fermavetro L/2−162 ×4, H−184 ×4 (90°);
    // montante RX70.301 H−110 ×1 (90°). Vetro: 2 × (H−150) × (L/2−132), uno per anta.
    private fun finestraDueAnte(): Tipologia = Tipologia(
        "Finestra a 2 ante",
        listOf(
            RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(22.0), 4, TAGLIO_45_45),
            RegolaTaglio("Anta (lato verticale)", ANTA, perH(40.0), 4, TAGLIO_45_45),
            RegolaTaglio(
                "Fermavetro (lato orizzontale)", FERMAVETRO, perMezzaL(162.0), 4, TAGLIO_90_90
            ),
            RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184.0), 4, TAGLIO_90_90),
            RegolaTaglio("Montante d'incontro", MONTANTE, perH(110.0), 1, TAGLIO_90_90)
        ),
        listOf(RegolaVetro("Vetro", perH(150.0), perMezzaL(132.0), 2))
    )

    // porta 1 anta: anta RX70.201 L−40/H−40 ×2 (45°); fermavetro L−184 ×4, H−HF−92 ×2, HF−188 ×2 (90°);
    // traverso porta RX70.402 L−130 ×1 (90°). Vetro spezzato dal traverso: 1 × (H−HF−58) × (L−150)
    // sopra + 1 × (HF−154) × (L−150) sotto. Senza traverso il vetro è unico → vale la finestra 1 anta.
    private fun portaUnaAnta(conTraverso: Boolean): Tipologia {
        if (!conTraverso) {
            val base = finestraUnaAnta()
            return Tipologia("Porta a 1 anta", base.regole(), base.regoleVetro())
        }
        return Tipologia(
            "Porta a 1 anta (con traverso)",
            listOf(
                RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(40.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, perH(40.0), 2, TAGLIO_45_45),
                RegolaTaglio(
                    "Fermavetro (lato orizzontale)", FERMAVETRO, perL(184.0), 4, TAGLIO_90_90
                ),
                // Un'estremità sola sul perimetro: dall'altra parte c'è il traverso, che non ingrossa.
                RegolaTaglio(
                    "Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(92.0), 2,
                    TAGLIO_90_90, 1
                ),
                RegolaTaglio(
                    "Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(188.0), 2,
                    TAGLIO_90_90, 1
                ),
                RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perL(130.0), 1, TAGLIO_90_90)
            ),
            // Idem per le lastre: in verticale un bordo va sul traverso, in orizzontale no.
            listOf(
                RegolaVetro("Vetro (sopra traverso)", hMenoHF(58.0), perL(150.0), 1, 1, 2),
                RegolaVetro("Vetro (sotto traverso)", perHF(154.0), perL(150.0), 1, 1, 2)
            )
        )
    }

    // porta 2 ante: anta RX70.201 L/2−22 ×4, H−40 ×4 (45°); fermavetro L/2−162 ×8, H−HF−92 ×4,
    // HF−188 ×4 (90°); montante RX70.301 H−110 ×1 (90°); traverso porta L/2−112 ×2 (90°).
    // Vetro spezzato dal traverso: 1 × (H−HF−58) × (L/2−130) sopra + 1 × (HF−154) × (L/2−130) sotto.
    // Senza traverso il vetro di ogni anta è unico → vale la finestra 2 ante.
    private fun portaDueAnte(conTraverso: Boolean): Tipologia {
        if (!conTraverso) {
            val base = finestraDueAnte()
            return Tipologia("Porta a 2 ante", base.regole(), base.regoleVetro())
        }
        return Tipologia(
            "Porta a 2 ante (con traverso)",
            listOf(
                RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(22.0), 4, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, perH(40.0), 4, TAGLIO_45_45),
                RegolaTaglio(
                    "Fermavetro (lato orizzontale)", FERMAVETRO, perMezzaL(162.0), 8, TAGLIO_90_90
                ),
                RegolaTaglio(
                    "Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(92.0), 4,
                    TAGLIO_90_90, 1
                ),
                RegolaTaglio(
                    "Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(188.0), 4,
                    TAGLIO_90_90, 1
                ),
                RegolaTaglio("Montante d'incontro", MONTANTE, perH(110.0), 1, TAGLIO_90_90),
                RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perMezzaL(112.0), 2, TAGLIO_90_90)
            ),
            listOf(
                RegolaVetro("Vetro (sopra traverso)", hMenoHF(58.0), perMezzaL(130.0), 1, 1, 2),
                RegolaVetro("Vetro (sotto traverso)", perHF(154.0), perMezzaL(130.0), 1, 1, 2)
            )
        )
    }
}
