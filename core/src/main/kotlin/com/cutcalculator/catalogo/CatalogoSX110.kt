package com.cutcalculator.catalogo

import com.cutcalculator.catalogo.Formule.mezzaLpiu
import com.cutcalculator.catalogo.Formule.perH
import com.cutcalculator.catalogo.Formule.perL
import com.cutcalculator.catalogo.Formule.perMezzaL
import com.cutcalculator.catalogo.Formule.terzoLpiu
import com.cutcalculator.dominio.Categoria
import com.cutcalculator.dominio.Formula
import com.cutcalculator.dominio.Profilo
import com.cutcalculator.dominio.RegolaTaglio
import com.cutcalculator.dominio.RegolaVetro
import com.cutcalculator.dominio.TipoTaglio.TAGLIO_45_45
import com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_90
import com.cutcalculator.dominio.Tipologia

/**
 * Dati reali del sistema **Twin SX 110** (finestre scorrevoli, taglio termico), trascritti dalle
 * schede di taglio del Gruppo E del catalogo.
 *
 * Sei tipologie (solo aste con formula; niente vetro/accessori):
 * - **2 ante** (telaio `SX11.138`, anta `SX11.203`);
 * - **2 ante fisso + mobile** (come sopra + incontro centrale per fisso `SX11.305`);
 * - **2 ante** con anta `SX11.206`;
 * - **3 ante** (telaio `SX11.130`);
 * - **4 ante** (telaio `SX11.101`, incontro centrale universale `SX11.303`);
 * - **2 ante alternativa** (telaio `SX11.101`+`SX11.136`, ante `SX11.207`/`.208`).
 *
 * I profili `SX11.301`/`SX11.303`/`SX11.305` sono **incontri centrali** (rispettivamente 2 ante,
 * 4 ante universale, per fisso).
 */
object CatalogoSX110 {

    // --- Anagrafica profili (Gruppo B) -------------------------------------------------
    // 4° argomento = peso in kg/m, dalla voce "Peso|Weight kg/ml" delle schede.
    private val TELAIO = Profilo("SX11.138", "Telaio", Categoria.TELAIO, 2.431)

    // Condivisi con SX 120: un solo posto dove scriverne il peso (vedi ProfiliCondivisi).
    private val TELAIO_2VIE = ProfiliCondivisi.BINARIO_2VIE
    private val TELAIO_3VIE = ProfiliCondivisi.BINARIO_3VIE
    private val TELAIO_MONTANTE = Profilo("SX11.136", "Telaio (montante)", Categoria.TELAIO, 1.63)
    private val ANTA = Profilo("SX11.203", "Anta", Categoria.ANTA, 1.681)
    private val ANTA_206 = Profilo("SX11.206", "Anta (SX11.206)", Categoria.ANTA, 1.781)

    // Attenzione: la scheda di taglio usa .207 sul lato L e .208 sul lato H, cioe' al contrario di come
    // il Gruppo B li chiama (.207 = montante/jamb, .208 = traverso/crosspiece). Qui vale il Gruppo B.
    private val ANTA_MONTANTE = Profilo("SX11.207", "Anta (montante)", Categoria.ANTA, 1.679)
    private val ANTA_TRAVERSO = Profilo("SX11.208", "Anta (traverso)", Categoria.ANTA, 1.529)
    private val MONTANTE = Profilo("SX11.301", "Incontro centrale", Categoria.MONTANTE, 0.495)
    private val NODO = Profilo("SX11.305", "Incontro centrale per fisso", Categoria.MONTANTE, 0.485)
    private val INCONTRO_4ANTE =
        Profilo("SX11.303", "Incontro centrale (4 ante universale)", Categoria.MONTANTE, 0.485)

    /**
     * Altezza dell'anta: `Hₐ = H − 78`, uguale in tutte le tipologie. La larghezza dell'anta (`Lₐ`)
     * cambia invece da tipologia a tipologia, quindi resta scritta caso per caso.
     */
    private fun altezzaAnta(): Formula = perH(78.0)

    /**
     * La "distinta di taglio vetri" della scheda, che qui è quotata sull'**anta** e non sul
     * serramento: `Hₐ − 140` × `Lₐ − 140`. Le due formule dell'anta arrivano dalle stesse costanti
     * usate per tagliarla, così il vetro segue l'anta se un domani la si corregge.
     *
     * La `quantita` è invece una **lastra per anta** (scelta dell'utente): le schede SX 110 stampano
     * sempre "Q.tà 2" anche per le tipologie a 3 e 4 ante, dove è chiaramente il conteggio di un nodo
     * e non del serramento intero. In SX 120 le schede contano già per serramento (1/2/3/4).
     */
    private fun vetro(
        quantita: Int, altezzaAnta: Formula, larghezzaAnta: Formula
    ): List<RegolaVetro> =
        listOf(RegolaVetro("Vetro", altezzaAnta.meno(140.0), larghezzaAnta.meno(140.0), quantita))

    /** Il sistema SX 110 con le sue tipologie. */
    fun sistema(): Sistema = Sistema(
        "SX 110", FamigliaSistema.SCORREVOLE,
        listOf(
            dueAnteFissoMobile(),
            dueAnte(),
            dueAnte206(),
            treAnte(),
            quattroAnte(),
            dueAnteAlternativa()
        )
    )

    // 2 ante fisso+mobile: telaio L,H ×2 (45°); anta L/2−3 ×4, H−78 ×4 (45°);
    // incontro centrale SX11.301 H−78 ×2 e per fisso SX11.305 H−78 ×2 (90°).
    // Vetro: 2 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/2−143).
    private fun dueAnteFissoMobile(): Tipologia {
        val antaH = altezzaAnta()
        val antaL = perMezzaL(3.0)
        return Tipologia(
            "Finestra scorrevole a 2 ante (fisso + mobile)",
            listOf(
                RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato orizzontale)", ANTA, antaL, 4, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, antaH, 4, TAGLIO_45_45),
                RegolaTaglio("Incontro centrale", MONTANTE, perH(78.0), 2, TAGLIO_90_90),
                RegolaTaglio("Incontro centrale per fisso", NODO, perH(78.0), 2, TAGLIO_90_90)
            ),
            vetro(2, antaH, antaL)
        )
    }

    // 2 ante: telaio SX11.138 L,H ×2 (45°); anta SX11.203 L/2−3 ×4, H−78 ×4 (45°);
    // incontro centrale SX11.301 H−78 ×2 (90°).
    // Vetro: 2 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/2−143).
    private fun dueAnte(): Tipologia {
        val antaH = altezzaAnta()
        val antaL = perMezzaL(3.0)
        return Tipologia(
            "Finestra scorrevole a 2 ante",
            listOf(
                RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato orizzontale)", ANTA, antaL, 4, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, antaH, 4, TAGLIO_45_45),
                RegolaTaglio("Incontro centrale", MONTANTE, perH(78.0), 2, TAGLIO_90_90)
            ),
            vetro(2, antaH, antaL)
        )
    }

    // 2 ante con anta SX11.206: telaio SX11.138 L,H ×2 (45°); anta SX11.206 L/2+2 ×4, H−78 ×4 (45°);
    // incontro centrale SX11.301 H−78 ×2 (90°).
    // Vetro: 2 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/2−138).
    private fun dueAnte206(): Tipologia {
        val antaH = altezzaAnta()
        val antaL = mezzaLpiu(2.0)
        return Tipologia(
            "Finestra scorrevole a 2 ante (anta SX11.206)",
            listOf(
                RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato orizzontale)", ANTA_206, antaL, 4, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA_206, antaH, 4, TAGLIO_45_45),
                RegolaTaglio("Incontro centrale", MONTANTE, perH(78.0), 2, TAGLIO_90_90)
            ),
            vetro(2, antaH, antaL)
        )
    }

    // 3 ante: telaio SX11.130 L,H ×2 (45°); anta SX11.203 L/3+22 ×6, H−78 ×6 (45°);
    // incontro centrale SX11.301 H−78 ×4 (90°).
    // Vetro: 3 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/3−118), una lastra per anta.
    private fun treAnte(): Tipologia {
        val antaH = altezzaAnta()
        val antaL = terzoLpiu(22.0)
        return Tipologia(
            "Finestra scorrevole a 3 ante",
            listOf(
                RegolaTaglio("Telaio (lato orizzontale)", TELAIO_3VIE, perL(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Telaio (lato verticale)", TELAIO_3VIE, perH(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato orizzontale)", ANTA, antaL, 6, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, antaH, 6, TAGLIO_45_45),
                RegolaTaglio("Incontro centrale", MONTANTE, perH(78.0), 4, TAGLIO_90_90)
            ),
            vetro(3, antaH, antaL)
        )
    }

    // 4 ante: telaio SX11.101 L,H ×2 (45°); anta SX11.203 L/2+2 ×8, H−78 ×8 (45°);
    // incontro centrale SX11.301 H−78 ×4 (90°); incontro centrale 4 ante SX11.303 H−126 ×1 (90°).
    // Vetro: 4 × (Hₐ−140) × (Lₐ−140) = (H−218) × (L/2−138), una lastra per anta.
    private fun quattroAnte(): Tipologia {
        val antaH = altezzaAnta()
        val antaL = mezzaLpiu(2.0)
        return Tipologia(
            "Finestra scorrevole a 4 ante",
            listOf(
                RegolaTaglio("Telaio (lato orizzontale)", TELAIO_2VIE, perL(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Telaio (lato verticale)", TELAIO_2VIE, perH(0.0), 2, TAGLIO_45_45),
                RegolaTaglio("Anta (lato orizzontale)", ANTA, antaL, 8, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, antaH, 8, TAGLIO_45_45),
                RegolaTaglio("Incontro centrale", MONTANTE, perH(78.0), 4, TAGLIO_90_90),
                RegolaTaglio(
                    "Incontro centrale (4 ante universale)", INCONTRO_4ANTE, perH(126.0), 1,
                    TAGLIO_90_90
                )
            ),
            vetro(4, antaH, antaL)
        )
    }

    // 2 ante alternativa: telaio SX11.101 L ×2 + SX11.136 H ×2 (45°);
    // anta SX11.208 (traverso) L/3+22 ×6 + SX11.207 (montante) H−78 ×6 (45°);
    // incontro centrale SX11.301 H−78 ×4 (90°).
    // NB: i due codici sono invertiti rispetto alla scheda di taglio, che assegna .207 al lato L e
    // .208 al lato H; qui si segue il Gruppo B (.207 = montante, .208 = traverso). Da riverificare
    // sulla scheda originale se questa tipologia verra' davvero inclusa.
    // Vetro: la scheda dell'anta SX11.207/.208 detrae meno, (Hₐ−120) × (Lₐ−89) = (H−198) × (L/3−67).
    // Quantità 3: nonostante il nome "2 ante", le ante sono quotate su L/3 (×6 traversi) come la 3 ante.
    private fun dueAnteAlternativa(): Tipologia {
        val antaH = altezzaAnta()
        val antaL = terzoLpiu(22.0)
        return Tipologia(
            "Finestra scorrevole a 2 ante (alternativa)",
            listOf(
                RegolaTaglio("Telaio (lato orizzontale)", TELAIO_2VIE, perL(0.0), 2, TAGLIO_45_45),
                RegolaTaglio(
                    "Telaio (lato verticale)", TELAIO_MONTANTE, perH(0.0), 2, TAGLIO_45_45
                ),
                RegolaTaglio("Anta (traverso)", ANTA_TRAVERSO, antaL, 6, TAGLIO_45_45),
                RegolaTaglio("Anta (montante)", ANTA_MONTANTE, antaH, 6, TAGLIO_45_45),
                RegolaTaglio("Incontro centrale", MONTANTE, perH(78.0), 4, TAGLIO_90_90)
            ),
            listOf(RegolaVetro("Vetro", antaH.meno(120.0), antaL.meno(89.0), 3))
        )
    }
}
