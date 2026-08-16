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
 * Dati reali del sistema **Twin CX 700** (battente, taglio termico), trascritti dalle schede di
 * taglio del Gruppo E (mappa config→scheda fornita dall'utente).
 *
 * Otto tipologie, stessa struttura del [CatalogoRX700] (cambiano gli offset): **finestra 1/2 ante**
 * (anta `CX70.201` + fermavetro `CX70.566`, montante d'incontro `CX70.301`), **porta 1/2 ante**
 * (stessi profili + traverso `CX70.402`) e l'**elemento fisso**, le ultime due con e senza
 * **traverso**. Il fermavetro è una barra rettangolare → taglio 90/90.
 *
 * **L'elemento fisso** è lo stesso telaio di finestre e porte, senza ante: il vetro appoggia
 * direttamente sul telaio e lo tiene il fermavetro di sempre. Dal nodo in sezione del catalogo
 * ("Elemento fisso") la vista del telaio è **50 mm** = 28 di `CX70.101` + 22 di `CX70.566`, da cui
 * `fermavetro = L−96` e `vetro = L−66` — la stessa coppia della finestra, dove però la differenza fra
 * i due è 34 mm perché lì il vetro è tenuto dall'**anta**, non dal telaio (qui sono 30). Le varianti
 * di telaio funzionano come sulle altre tipologie: il fermavetro batte sul perimetro con entrambe le
 * estremità, quindi un telaio maggiorato accorcia lui e il vetro di 24 mm per lato senza che il
 * catalogo dica nulla.
 * ⚠️ La quota del **traverso nel fisso** (`L−42`) è l'unica **dedotta e non confermata da una
 * scheda**: è quella della porta (`L−130`) più gli 88 mm che l'anta occupava e qui non ci sono — la
 * differenza fra i fermavetri delle due tipologie (92 mm per lato contro 48). Regge se il traverso si
 * incastra nel telaio quanto si incastra nell'anta: con quel numero si infila 27 mm oltre la testa
 * del fermavetro, esattamente come nella porta. Tutte le altre quote della tipologia sono derivate
 * per somma e tornano.
 *
 * **Cosa non c'è.** Le schede riportano anche un gruppo "base" con anta `CX70.203` e fermavetro
 * `CX70.605` *senza quota*, che qui è stato **tolto**: si tengono solo le tipologie in cui il
 * fermavetro è quotato, così la distinta è completa e il preventivo non dimentica pezzi. Sparito il
 * gruppo base, le tipologie rimaste non hanno più bisogno del suffisso "(alternativo)" che le
 * distingueva.
 *
 * **Lo switch del traverso (montante centrale).** La porta non ha sempre il traverso `CX70.402` che
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
 * **Vetri.** Dalle "distinte di taglio vetri", identiche a quelle dell'RX 700; nelle schede le
 * colonne sono **H** e **L**, nell'ordine in cui le prende il record `Vetro`:
 * - finestra 1 anta: 1 × `(H−150) × (L−150)`;
 * - finestra 2 ante: 2 × `(H−150) × (L/2−132)`, uno per anta;
 * - porta col traverso: due lastre, `(H−HF−58)` sopra e `(HF−154)` sotto, larghe `L−150` (1 anta) o
 *   `L/2−130` (2 ante);
 * - porta senza traverso: quelle della finestra, insieme alle sue regole di taglio.
 */
object CatalogoCX700 {

    /**
     * Le due **teste dei fermavetri perpendicolari**, 20 mm per lato. Vedi
     * [CatalogoRX700] per il perché: le schede quotano tutti e quattro i fermavetri di un riquadro
     * alla luce piena, ma una delle due coppie deve entrare *fra* l'altra, e qui si accorciano
     * sempre gli **orizzontali**.
     */
    private const val TESTE_FERMAVETRO = 40.0

    // --- Anagrafica profili (Gruppo B): 4° argomento = peso in kg/m, dalla voce "Peso kg/ml." ---
    private val TELAIO = Profilo("CX70.101", "Telaio ad L piccolo", Categoria.TELAIO, 1.287)
    private val ANTA = Profilo("CX70.201", "Anta tonda piccola", Categoria.ANTA, 1.528)
    private val MONTANTE = Profilo("CX70.301", "Montante d'incontro", Categoria.MONTANTE, 1.471)
    private val FERMAVETRO = Profilo("CX70.566", "Fermavetro", Categoria.FERMAVETRO, 0.396)
    private val TRAVERSO_PORTA = Profilo("CX70.402", "Traverso porta", Categoria.TRAVERSO, 2.066)

    // --- Varianti (6° argomento del Profilo = extra kerf per estremità a 45°) -----------
    // Il numero è la maggiore larghezza in vista della sezione: si paga come barra in più sulle
    // diagonali e come restringimento di ciò che il profilo racchiude (3° argomento della Variante).
    private val TELAIO_L_MAGG =
        Profilo("CX70.105", "Telaio ad L grande", Categoria.TELAIO, 1.681, 0.0, 24.0)
    private val TELAIO_Z =
        Profilo("CX70.102", "Telaio a Z piccolo", Categoria.TELAIO, 1.366, 0.0, 22.0)
    private val TELAIO_Z_MAGG =
        Profilo("CX70.106", "Telaio a Z grande", Categoria.TELAIO, 1.791, 0.0, 46.0)
    private val ANTA_MAGG =
        Profilo("CX70.202", "Anta tonda grande", Categoria.ANTA, 1.956, 0.0, 24.0)

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

    /** Il sistema CX 700 con le sue tipologie. */
    fun sistema(): Sistema = Sistema(
        "CX 700", FamigliaSistema.BATTENTE,
        listOf(
            finestraUnaAnta(),
            finestraDueAnte(),
            portaUnaAnta(false),
            portaUnaAnta(true),
            portaDueAnte(false),
            portaDueAnte(true),
            elementoFisso(false),
            elementoFisso(true)
        ),
        varianti()
    )

    // elemento fisso: telaio L,H ×2 (45°); fermavetro L−96/H−96 ×2 (90°, orizzontali meno le teste).
    // Vetro: 1 × (H−66) × (L−66). Niente anta: il vetro sta sul telaio.
    //
    // Col traverso CX70.402 il riquadro si spezza in due, come nella porta e con lo stesso HF (dal
    // filo esterno inferiore del telaio al filo superiore del traverso). Le quote si derivano da
    // quelle della porta aggiungendo i millimetri che l'anta occupava e qui non ci sono; le due parti
    // più i 96 mm di vista del traverso ridanno il pezzo intero. Nei vetri le due prese sono diverse:
    // 15 mm per lato dove il bordo va sul telaio, 17 dove va sul traverso (che è lo stesso profilo
    // della porta, quindi con la geometria dell'anta).
    private fun elementoFisso(conTraverso: Boolean): Tipologia {
        val telaioEFermavetri = listOf(
            RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
            RegolaTaglio(
                "Fermavetro (lato orizzontale)", FERMAVETRO, perL(96.0).meno(TESTE_FERMAVETRO),
                if (conTraverso) 4 else 2, TAGLIO_90_90
            )
        )
        if (!conTraverso) {
            return Tipologia(
                "Elemento fisso",
                telaioEFermavetri + RegolaTaglio(
                    "Fermavetro (lato verticale)", FERMAVETRO, perH(96.0), 2, TAGLIO_90_90
                ),
                listOf(RegolaVetro("Vetro", perH(66.0), perL(66.0), 1))
            )
        }
        return Tipologia(
            "Elemento fisso (con traverso)",
            telaioEFermavetri + listOf(
                // Un'estremità sola sul perimetro: dall'altra c'è il traverso, che non ingrossa.
                RegolaTaglio(
                    "Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(48.0), 2,
                    TAGLIO_90_90, 1
                ),
                RegolaTaglio(
                    "Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(144.0), 2,
                    TAGLIO_90_90, 1
                ),
                // ⚠️ Quota dedotta, non letta da una scheda: è quella della porta (L−130) più gli
                // 88 mm che l'anta occupava e qui non ci sono, cioè la differenza fra i fermavetri
                // delle due tipologie (92 − 48 per lato). Vedi CLAUDE.md, "Da confermare".
                RegolaTaglio("Traverso", TRAVERSO_PORTA, perL(42.0), 1, TAGLIO_90_90)
            ),
            listOf(
                RegolaVetro("Vetro (sopra traverso)", hMenoHF(16.0), perL(66.0), 1, 1, 2),
                RegolaVetro("Vetro (sotto traverso)", perHF(112.0), perL(66.0), 1, 1, 2)
            )
        )
    }

    // finestra 1 anta: telaio L,H ×2 (45°); anta CX70.201 L−44/H−44 ×2 (45°);
    // fermavetro L−184/H−184 ×2 (90°). Vetro: 1 × (H−150) × (L−150).
    private fun finestraUnaAnta(): Tipologia = Tipologia(
        "Finestra a 1 anta",
        listOf(
            RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(44.0), 2, TAGLIO_45_45),
            RegolaTaglio("Anta (lato verticale)", ANTA, perH(44.0), 2, TAGLIO_45_45),
            RegolaTaglio(
                "Fermavetro (lato orizzontale)", FERMAVETRO, perL(184.0).meno(TESTE_FERMAVETRO), 2,
                TAGLIO_90_90
            ),
            RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184.0), 2, TAGLIO_90_90)
        ),
        listOf(RegolaVetro("Vetro", perH(150.0), perL(150.0), 1))
    )

    // finestra 2 ante: anta CX70.201 L/2−24.5 ×4, H−44 ×4 (45°); fermavetro L/2−164.5 ×4, H−184 ×4 (90°);
    // montante CX70.301 H−110 ×1 (90°). Vetro: 2 × (H−150) × (L/2−132), uno per anta.
    private fun finestraDueAnte(): Tipologia = Tipologia(
        "Finestra a 2 ante",
        listOf(
            RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
            RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(24.5), 4, TAGLIO_45_45),
            RegolaTaglio("Anta (lato verticale)", ANTA, perH(44.0), 4, TAGLIO_45_45),
            RegolaTaglio(
                "Fermavetro (lato orizzontale)", FERMAVETRO,
                perMezzaL(164.5).meno(TESTE_FERMAVETRO), 4, TAGLIO_90_90
            ),
            RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184.0), 4, TAGLIO_90_90),
            RegolaTaglio("Montante d'incontro", MONTANTE, perH(110.0), 1, TAGLIO_90_90)
        ),
        listOf(RegolaVetro("Vetro", perH(150.0), perMezzaL(132.0), 2))
    )

    // porta 1 anta: anta CX70.201 L−44/H−44 ×2 (45°); fermavetro L−184 ×4, H−HF−92 ×2, HF−188 ×2 (90°);
    // traverso porta CX70.402 L−130 ×1 (90°). Vetro spezzato dal traverso: 1 × (H−HF−58) × (L−150)
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
                RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(44.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, perH(44.0), 2, TAGLIO_45_45),
                RegolaTaglio(
                    "Fermavetro (lato orizzontale)", FERMAVETRO,
                    perL(184.0).meno(TESTE_FERMAVETRO), 4, TAGLIO_90_90
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

    // porta 2 ante: anta CX70.201 L/2−24.5 ×4, H−44 ×4 (45°); fermavetro L/2−164.5 ×8, H−HF−92 ×4,
    // HF−188 ×4 (90°); montante CX70.301 H−110 ×1 (90°); traverso porta L/2−110.5 ×2 (90°).
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
                RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(24.5), 4, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, perH(44.0), 4, TAGLIO_45_45),
                RegolaTaglio(
                    "Fermavetro (lato orizzontale)", FERMAVETRO,
                    perMezzaL(164.5).meno(TESTE_FERMAVETRO), 8, TAGLIO_90_90
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
                RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perMezzaL(110.5), 2, TAGLIO_90_90)
            ),
            listOf(
                RegolaVetro("Vetro (sopra traverso)", hMenoHF(58.0), perMezzaL(130.0), 1, 1, 2),
                RegolaVetro("Vetro (sotto traverso)", perHF(154.0), perMezzaL(130.0), 1, 1, 2)
            )
        )
    }
}
