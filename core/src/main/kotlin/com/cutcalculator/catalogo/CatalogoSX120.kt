package com.cutcalculator.catalogo

import com.cutcalculator.catalogo.Formule.mezzaLpiu
import com.cutcalculator.catalogo.Formule.perH
import com.cutcalculator.catalogo.Formule.perL
import com.cutcalculator.catalogo.Formule.perMezzaL
import com.cutcalculator.catalogo.Formule.quartoL
import com.cutcalculator.catalogo.Formule.quartoLpiu
import com.cutcalculator.catalogo.Formule.terzoL
import com.cutcalculator.catalogo.Formule.terzoLpiu
import com.cutcalculator.dominio.Categoria
import com.cutcalculator.dominio.Formula
import com.cutcalculator.dominio.Profilo
import com.cutcalculator.dominio.RegolaTaglio
import com.cutcalculator.dominio.RegolaVetro
import com.cutcalculator.dominio.TipoTaglio.TAGLIO_45_45
import com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_45_DX
import com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_45_SX
import com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_90
import com.cutcalculator.dominio.Tipologia

/**
 * Dati reali del sistema **Twin SX 120** (finestre scorrevoli, taglio termico), trascritti dalle
 * "distinte di taglio" del Gruppo E (mappa config→scheda fornita dall'utente).
 *
 * Si tagliano solo i profili in alluminio con sigla `SX`/`RC`; le sigle `ASX` (guarnizioni) e
 * `BX` (plastiche anti-attrito) sono omesse. Ruoli dei profili dal **Gruppo B**: telaio **binario**
 * `SX11.1xx` + **telaio esterno** `SX12.101`/`.153` + **carter** `SX12.501`; ante
 * `SX12.203`/`.204` (e `.205` per l'angolo); **incontri centrali** `SX12.301` (2/3 ante),
 * `SX12.303` (4 ante), `SX12.304/305/306` (angolo).
 *
 * Sono modellate le **varianti base** di ogni configurazione; le 2 ante hanno anche la variante
 * **fisso + mobile**. Le schede riportano per 3/4 ante ulteriori varianti "Per Fisso/i + Mobile/i"
 * (piccole differenze su incontro/carter) non ancora cablate. Cut type del telaio esterno e dei nodi
 * dedotto dal disegno (montanti 90/45, traverso 45/45; carter/incontri 90/90). L'angolo della "4 ante
 * con angolo" è tagliato "poi a 45°" (qui reso 90/90, la ripresa a 45° è manuale).
 */
object CatalogoSX120 {

    // --- Anagrafica profili (Gruppo B) -------------------------------------------------
    // 4° argomento = peso in kg/m, dalla voce "Peso kg/ml." delle schede.
    private val BINARIO_SINGOLO =
        Profilo("SX11.154", "Telaio (binario singolo)", Categoria.TELAIO, 1.226)

    // Condivisi con SX 110: un solo posto dove scriverne il peso (vedi ProfiliCondivisi).
    private val BINARIO_2VIE = ProfiliCondivisi.BINARIO_2VIE
    private val BINARIO_3VIE = ProfiliCondivisi.BINARIO_3VIE

    // La scheda SX 110 dà 1.565 per questo codice ("Soglia ribassata"): qui vale il dato del
    // catalogo in cui il profilo è usato, cioè SX 120 ("Telaio binario 2 vie ribassato").
    private val BINARIO_RIBASSATO =
        Profilo("SX11.176", "Telaio (binario 2 vie ribassato)", Categoria.TELAIO, 1.501)
    private val ESTERNO = Profilo("SX12.101", "Telaio esterno", Categoria.TELAIO, 0.912)
    private val ESTERNO_SINGOLO =
        Profilo("SX12.153", "Telaio esterno (via singola)", Categoria.TELAIO, 1.604)
    private val CARTER = Profilo("SX12.501", "Carter per telaio", Categoria.TELAIO, 0.283)
    private val ANTA = Profilo("SX12.203", "Anta (vetro infilare)", Categoria.ANTA, 1.142)
    private val ANTA_RIDOTTA =
        Profilo("SX12.204", "Anta (vetro infilare ridotta)", Categoria.ANTA, 0.832)
    private val ANTA_ANGOLO =
        Profilo("SX12.205", "Anta (vetro infilare per angolo)", Categoria.ANTA, 0.987)
    private val INCONTRO = Profilo("SX12.301", "Incontro centrale", Categoria.MONTANTE, 1.204)
    private val INCONTRO_4ANTE =
        Profilo("SX12.303", "Incontro (per 4 ante)", Categoria.MONTANTE, 0.766)

    // L'angolo è un assieme di tre profili: la scheda li stampa in una riga sola (stessa quota, 1 pz),
    // ma sono tre barre distinte da tagliare e da pesare.
    private val ANGOLO_INTERNO =
        Profilo("SX12.304", "Incontro anta per angolo interno", Categoria.MONTANTE, 0.418)
    private val ANGOLO_PRINCIPALE =
        Profilo("SX12.305", "Incontro angolo esterno (principale)", Categoria.MONTANTE, 1.385)
    private val ANGOLO_SECONDARIO =
        Profilo("SX12.306", "Incontro angolo esterno (secondario)", Categoria.MONTANTE, 0.717)

    /** Il telaio esterno (2 montanti 90/45 + 1 traverso 45/45) del profilo dato. */
    private fun telaioEsterno(p: Profilo): List<RegolaTaglio> = listOf(
        RegolaTaglio("Telaio esterno (montante dx)", p, perH(0.0), 1, TAGLIO_90_45_DX),
        RegolaTaglio("Telaio esterno (montante sx)", p, perH(0.0), 1, TAGLIO_90_45_SX),
        RegolaTaglio("Telaio esterno (traverso)", p, perL(0.0), 1, TAGLIO_45_45)
    )

    /**
     * La "distinta di taglio vetri" della scheda: una lastra per anta, sempre `anta − 78` su entrambi
     * i lati (verifica: anta `H−63` → vetro `H−141`; soglia ribassata anta `H−36` → vetro `H−114`).
     * Qui le quote restano in H e L come le stampa il catalogo.
     */
    private fun vetro(quantita: Int, h: Formula, l: Formula): List<RegolaVetro> =
        listOf(RegolaVetro("Vetro", h, l, quantita))

    private fun regole(vararg blocchi: List<RegolaTaglio>): List<RegolaTaglio> = blocchi.flatMap { it }

    /** Il sistema SX 120 con le sue tipologie. */
    fun sistema(): Sistema = Sistema(
        "SX 120", FamigliaSistema.SCORREVOLE,
        listOf(
            scomparsa(),
            dueAnteMobili(),
            dueAnteFissoMobile(),
            dueAnteRibassataMobili(),
            dueAnteRibassataFissoMobile(),
            treAnteFissoCentrale(),
            treAnte(),
            quattroAnte(),
            quattroAnteConAngolo()
        )
    )

    // #1 - Scomparsa: 1 anta a scomparsa (binario singolo). Ante e incontro in singola copia.
    // Vetro: 1 × (H−141) × (L/2−68).
    private fun scomparsa(): Tipologia = Tipologia(
        "Finestra scorrevole a scomparsa",
        regole(
            listOf(
                RegolaTaglio("Telaio (binario)", BINARIO_SINGOLO, perL(42.0), 1, TAGLIO_90_90)
            ),
            telaioEsterno(ESTERNO_SINGOLO),
            listOf(
                RegolaTaglio("Anta (lato orizzontale)", ANTA, mezzaLpiu(10.0), 1, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, perH(63.0), 1, TAGLIO_45_45),
                RegolaTaglio(
                    "Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, perMezzaL(26.0), 1, TAGLIO_45_45
                ),
                RegolaTaglio(
                    "Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99.0), 1, TAGLIO_45_45
                ),
                RegolaTaglio("Incontro centrale", INCONTRO, perH(76.0), 1, TAGLIO_90_90)
            )
        ),
        vetro(1, perH(141.0), perMezzaL(68.0))
    )

    // Aste comuni alle 2 ante "mm.110" (binario SX11.101): telaio + carter + ante.
    private fun comuniDueAnte(): List<RegolaTaglio> = regole(
        listOf(RegolaTaglio("Telaio (binario)", BINARIO_2VIE, perL(42.0), 1, TAGLIO_90_90)),
        telaioEsterno(ESTERNO),
        listOf(
            RegolaTaglio("Carter per telaio", CARTER, perH(94.0), 2, TAGLIO_90_90),
            RegolaTaglio("Anta (lato orizzontale)", ANTA, mezzaLpiu(10.0), 2, TAGLIO_45_45),
            RegolaTaglio("Anta (lato verticale)", ANTA, perH(63.0), 2, TAGLIO_45_45),
            RegolaTaglio(
                "Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, perMezzaL(26.0), 2, TAGLIO_45_45
            ),
            RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99.0), 2, TAGLIO_45_45)
        )
    )

    // #2 - 2 ante mobile+mobile: comuni + incontro H−121 ×2. Vetro: 2 × (H−141) × (L/2−68).
    private fun dueAnteMobili(): Tipologia = Tipologia(
        "Finestra scorrevole a 2 ante (mobile + mobile)",
        regole(
            comuniDueAnte(),
            listOf(RegolaTaglio("Incontro centrale", INCONTRO, perH(121.0), 2, TAGLIO_90_90))
        ),
        vetro(2, perH(141.0), perMezzaL(68.0))
    )

    // #2 - 2 ante fisso+mobile: comuni + incontro H−121 ×1 e H−115 ×1 + carter L/2−94 ×1.
    // Stessa scheda vetri della mobile+mobile.
    private fun dueAnteFissoMobile(): Tipologia = Tipologia(
        "Finestra scorrevole a 2 ante (fisso + mobile)",
        regole(
            comuniDueAnte(),
            listOf(
                RegolaTaglio("Incontro centrale", INCONTRO, perH(121.0), 1, TAGLIO_90_90),
                RegolaTaglio("Incontro centrale", INCONTRO, perH(115.0), 1, TAGLIO_90_90),
                RegolaTaglio(
                    "Carter per telaio (fisso)", CARTER, perMezzaL(94.0), 1, TAGLIO_90_90
                )
            )
        ),
        vetro(2, perH(141.0), perMezzaL(68.0))
    )

    // Aste comuni alle 2 ante "soglia ribassata" (binario SX11.176).
    private fun comuniRibassata(): List<RegolaTaglio> = regole(
        listOf(RegolaTaglio("Telaio (binario)", BINARIO_RIBASSATO, perL(42.0), 1, TAGLIO_90_90)),
        telaioEsterno(ESTERNO),
        listOf(
            RegolaTaglio("Carter per telaio", CARTER, perH(67.0), 2, TAGLIO_90_90),
            RegolaTaglio("Anta (lato orizzontale)", ANTA, mezzaLpiu(10.0), 2, TAGLIO_45_45),
            RegolaTaglio("Anta (lato verticale)", ANTA, perH(36.0), 2, TAGLIO_45_45),
            RegolaTaglio(
                "Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, perMezzaL(26.0), 2, TAGLIO_45_45
            ),
            RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(72.0), 2, TAGLIO_45_45)
        )
    )

    // #3 - 2 ante soglia ribassata mobile+mobile: comuni + incontro H−94 ×2.
    // Vetro: 2 × (H−114) × (L/2−68): l'anta ribassata è 27 mm più alta, e il vetro con lei.
    private fun dueAnteRibassataMobili(): Tipologia = Tipologia(
        "Finestra scorrevole a 2 ante, soglia ribassata (mobile + mobile)",
        regole(
            comuniRibassata(),
            listOf(RegolaTaglio("Incontro centrale", INCONTRO, perH(94.0), 2, TAGLIO_90_90))
        ),
        vetro(2, perH(114.0), perMezzaL(68.0))
    )

    // #3 - 2 ante soglia ribassata fisso+mobile: comuni + incontro H−94 ×1 e H−88 ×1 + carter L/2−94 ×1.
    // Stessa scheda vetri della mobile+mobile ribassata.
    private fun dueAnteRibassataFissoMobile(): Tipologia = Tipologia(
        "Finestra scorrevole a 2 ante, soglia ribassata (fisso + mobile)",
        regole(
            comuniRibassata(),
            listOf(
                RegolaTaglio("Incontro centrale", INCONTRO, perH(94.0), 1, TAGLIO_90_90),
                RegolaTaglio("Incontro centrale", INCONTRO, perH(88.0), 1, TAGLIO_90_90),
                RegolaTaglio(
                    "Carter per telaio (fisso)", CARTER, perMezzaL(94.0), 1, TAGLIO_90_90
                )
            )
        ),
        vetro(2, perH(114.0), perMezzaL(68.0))
    )

    // Ante + incontro comuni alle 3 ante: ante L/3+29.35 / L/3−6.65, incontro H−121 ×4.
    private fun anteTreAnte(): List<RegolaTaglio> = listOf(
        RegolaTaglio("Anta (lato orizzontale)", ANTA, terzoLpiu(29.35), 3, TAGLIO_45_45),
        RegolaTaglio("Anta (lato verticale)", ANTA, perH(63.0), 2, TAGLIO_45_45),
        RegolaTaglio(
            "Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, terzoL(6.65), 3, TAGLIO_45_45
        ),
        RegolaTaglio("Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99.0), 4, TAGLIO_45_45),
        RegolaTaglio("Incontro centrale", INCONTRO, perH(121.0), 4, TAGLIO_90_90)
    )

    // #4 - 3 ante fisso centrale: binario SX11.101 (2 vie); carter H−94 ×2.
    // Vetro: 3 × (H−141) × (L/3−48.65).
    private fun treAnteFissoCentrale(): Tipologia = Tipologia(
        "Finestra scorrevole a 3 ante, fisso centrale",
        regole(
            listOf(RegolaTaglio("Telaio (binario)", BINARIO_2VIE, perL(42.0), 1, TAGLIO_90_90)),
            telaioEsterno(ESTERNO),
            listOf(RegolaTaglio("Carter per telaio", CARTER, perH(94.0), 2, TAGLIO_90_90)),
            anteTreAnte()
        ),
        vetro(3, perH(141.0), terzoL(48.65))
    )

    // #5 - 3 ante: binario SX11.130 (3 vie); carter H−94 ×4. Stessa scheda vetri della #4.
    private fun treAnte(): Tipologia = Tipologia(
        "Finestra scorrevole a 3 ante",
        regole(
            listOf(RegolaTaglio("Telaio (binario)", BINARIO_3VIE, perL(42.0), 1, TAGLIO_90_90)),
            telaioEsterno(ESTERNO),
            listOf(RegolaTaglio("Carter per telaio", CARTER, perH(94.0), 4, TAGLIO_90_90)),
            anteTreAnte()
        ),
        vetro(3, perH(141.0), terzoL(48.65))
    )

    // #6 - 4 ante: binario SX11.101; ante L/4+24 / L/4−12; incontro SX12.301 ×4 + SX12.303 ×2.
    // Vetro: 4 × (H−141) × (L/4−54).
    private fun quattroAnte(): Tipologia = Tipologia(
        "Finestra scorrevole a 4 ante",
        regole(
            listOf(RegolaTaglio("Telaio (binario)", BINARIO_2VIE, perL(42.0), 1, TAGLIO_90_90)),
            telaioEsterno(ESTERNO),
            listOf(
                RegolaTaglio("Carter per telaio", CARTER, perH(94.0), 2, TAGLIO_90_90),
                RegolaTaglio("Anta (lato orizzontale)", ANTA, quartoLpiu(24.0), 4, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, perH(63.0), 3, TAGLIO_45_45),
                RegolaTaglio(
                    "Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, quartoL(12.0), 4, TAGLIO_45_45
                ),
                RegolaTaglio(
                    "Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99.0), 5, TAGLIO_45_45
                ),
                RegolaTaglio("Incontro centrale", INCONTRO, perH(121.0), 4, TAGLIO_90_90),
                RegolaTaglio("Incontro (per 4 ante)", INCONTRO_4ANTE, perH(121.0), 2, TAGLIO_90_90)
            )
        ),
        vetro(4, perH(141.0), quartoL(54.0))
    )

    // #7 - 4 ante con angolo: binario SX11.101 L−21 ×2 (poi a 45°); ante L/2−33 / L/2−69 + anta angolo
    // SX12.205; nodo angolo = assieme SX12.304 + .305 + .306, tutti H−121 ×1 (la scheda li stampa in
    // una riga sola). Cut angolo reso 90/90 (ripresa a 45° manuale).
    // Vetro: 4 × (H−141) × (L/2−111): qui L è il lato dell'angolo, non la luce totale.
    private fun quattroAnteConAngolo(): Tipologia = Tipologia(
        "Finestra scorrevole a 4 ante con angolo",
        regole(
            listOf(
                RegolaTaglio(
                    "Telaio (binario, poi a 45°)", BINARIO_2VIE, perL(21.0), 2, TAGLIO_90_90
                )
            ),
            telaioEsterno(ESTERNO),
            listOf(
                RegolaTaglio("Carter per telaio", CARTER, perH(94.0), 2, TAGLIO_90_90),
                RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(33.0), 4, TAGLIO_45_45),
                RegolaTaglio("Anta (lato verticale)", ANTA, perH(63.0), 2, TAGLIO_45_45),
                RegolaTaglio(
                    "Anta angolo (lato verticale)", ANTA_ANGOLO, perH(63.0), 2, TAGLIO_45_45
                ),
                RegolaTaglio(
                    "Anta ridotta (lato orizzontale)", ANTA_RIDOTTA, perMezzaL(69.0), 4, TAGLIO_45_45
                ),
                RegolaTaglio(
                    "Anta ridotta (lato verticale)", ANTA_RIDOTTA, perH(99.0), 4, TAGLIO_45_45
                ),
                RegolaTaglio("Incontro centrale", INCONTRO, perH(121.0), 4, TAGLIO_90_90),
                RegolaTaglio(
                    "Incontro angolo (anta interna)", ANGOLO_INTERNO, perH(121.0), 1, TAGLIO_90_90
                ),
                RegolaTaglio(
                    "Incontro angolo (esterno principale)", ANGOLO_PRINCIPALE, perH(121.0), 1,
                    TAGLIO_90_90
                ),
                RegolaTaglio(
                    "Incontro angolo (esterno secondario)", ANGOLO_SECONDARIO, perH(121.0), 1,
                    TAGLIO_90_90
                )
            )
        ),
        vetro(4, perH(141.0), perMezzaL(111.0))
    )
}
