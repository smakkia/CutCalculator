package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Formula;
import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.dominio.RegolaTaglio;
import com.cutcalculator.dominio.Tipologia;

import java.util.List;

import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_45_45;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_45_DX;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_45_SX;
import static com.cutcalculator.dominio.TipoTaglio.TAGLIO_90_90;

/**
 * Dati reali del sistema <b>Twin SX 120</b> (finestre scorrevoli, taglio termico).
 * <p>
 * La scheda SX 120 è a configurazione <b>fisso + mobile</b> e mescola profili di alluminio,
 * guarnizioni e plastiche anti-attrito. Regole di trascrizione applicate:
 * <ul>
 *   <li>si tagliano solo i profili con sigla <b>SX</b> o <b>RC</b> (le sigle {@code ASX}/{@code BX}
 *       sono guarnizioni/plastiche → omesse);</li>
 *   <li>le sezioni "per fisso + mobile" generano <b>due tipologie</b>: una con quelle parti
 *       (senza l'alternativa) e una col comportamento opposto.</li>
 * </ul>
 * Config trascritta: telaio {@code SX11.176}, "2 ante". Da validare: completezza del telaio
 * (qui 1 pezzo, come da scheda), le categorie dei profili {@code SX12.*} e l'orientamento dx/sx
 * dei due montanti del nodo {@code SX12.101} (assegnato arbitrariamente: non cambia il piano).
 */
public final class CatalogoSX120 {

    private CatalogoSX120() {
    }

    // --- Anagrafica profili (Gruppo B) -------------------------------------------------
    private static final Profilo TELAIO = new Profilo("SX11.176", "Telaio", Categoria.TELAIO);
    private static final Profilo NODO = new Profilo("SX12.101", "Nodo d'incontro", Categoria.MONTANTE);
    private static final Profilo ANTA_301 = new Profilo("SX12.301", "Anta (montante)", Categoria.ANTA);
    private static final Profilo ANTA_501 = new Profilo("SX12.501", "Anta (montante)", Categoria.ANTA);
    private static final Profilo ANTA_203 = new Profilo("SX12.203", "Anta (traverso)", Categoria.ANTA);

    // --- Formule di comodo -------------------------------------------------------------
    private static Formula perL(double offset) {
        return new Formula(1, 0, 0, -offset);
    }

    private static Formula perH(double offset) {
        return new Formula(0, 1, 0, -offset);
    }

    /** L/2 + add (offset positivo). */
    private static Formula mezzaLpiu(double add) {
        return new Formula(0.5, 0, 0, add);
    }

    /** Il sistema SX 120 con le due varianti della 2 ante. */
    public static Sistema sistema() {
        return new Sistema("SX 120", FamigliaSistema.SCORREVOLE, List.of(
                fissoMobile(),
                alternativa()));
    }

    // Aste comuni a entrambe le configurazioni.
    private static List<RegolaTaglio> comuni() {
        return List.of(
                new RegolaTaglio("Telaio", TELAIO, perL(42), 1, TAGLIO_90_90),
                new RegolaTaglio("Anta (montante)", ANTA_501, perH(94), 2, TAGLIO_90_90),
                new RegolaTaglio("Anta (traverso)", ANTA_203, mezzaLpiu(10), 2, TAGLIO_45_45));
    }

    // Fisso + mobile: comuni + tutto il nodo SX12.101 + SX12.301 come H−94 ×1 e H−88 ×1
    // (anta fissa e mobile hanno montanti di lunghezza diversa).
    private static Tipologia fissoMobile() {
        List<RegolaTaglio> regole = new java.util.ArrayList<>(comuni());
        regole.add(new RegolaTaglio("Nodo d'incontro (montante dx)", NODO, perH(0), 1, TAGLIO_90_45_DX));
        regole.add(new RegolaTaglio("Nodo d'incontro (montante sx)", NODO, perH(0), 1, TAGLIO_90_45_SX));
        regole.add(new RegolaTaglio("Nodo d'incontro (traverso)", NODO, perL(0), 1, TAGLIO_45_45));
        regole.add(new RegolaTaglio("Anta (montante)", ANTA_301, perH(94), 1, TAGLIO_90_90));
        regole.add(new RegolaTaglio("Anta (montante)", ANTA_301, perH(88), 1, TAGLIO_90_90));
        return new Tipologia("Finestra scorrevole a 2 ante (fisso + mobile)", regole);
    }

    // Alternativa: comuni + niente nodo SX12.101 + SX12.301 come H−94 ×2 (due ante mobili uguali).
    private static Tipologia alternativa() {
        List<RegolaTaglio> regole = new java.util.ArrayList<>(comuni());
        regole.add(new RegolaTaglio("Anta (montante)", ANTA_301, perH(94), 2, TAGLIO_90_90));
        return new Tipologia("Finestra scorrevole a 2 ante (mobile + mobile)", regole);
    }
}
