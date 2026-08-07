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
 * Dati reali del sistema <b>Twin RX 700</b> (battente, taglio termico), trascritti dalle schede di
 * taglio del Gruppo E (mappa config→scheda fornita dall'utente).
 * <p>
 * Sei configurazioni (stessa struttura del {@link CatalogoCX700 CX 700}, cambiano gli offset):
 * <b>finestra 1/2 ante</b> (anta {@code RX70.203}, montante {@code RX60.301}, fermavetro {@code RX70.605}
 * senza quota → omesso); <b>finestra 1/2 ante alternativo</b> (anta {@code RX70.201} + fermavetro
 * {@code RX70.511}, montante {@code RX70.301}); <b>porta 1/2 ante</b> (anta {@code RX70.201} + fermavetro
 * {@code RX70.511} col vetro spezzato dal traverso {@code RX70.402}, quindi con l'altezza parziale
 * <b>HF</b>). Il fermavetro è una barra rettangolare → taglio 90/90.
 */
public final class CatalogoRX700 {

    private CatalogoRX700() {
    }

    // --- Anagrafica profili (Gruppo B) -------------------------------------------------
    private static final Profilo TELAIO = new Profilo("RX70.101", "Telaio", Categoria.TELAIO);
    private static final Profilo ANTA = new Profilo("RX70.203", "Anta", Categoria.ANTA);
    private static final Profilo ANTA_ALT = new Profilo("RX70.201", "Anta (alternativa/porta)", Categoria.ANTA);
    private static final Profilo MONTANTE = new Profilo("RX60.301", "Montante d'incontro", Categoria.MONTANTE);
    private static final Profilo MONTANTE_ALT = new Profilo("RX70.301", "Montante d'incontro (alternativo/porta)", Categoria.MONTANTE);
    private static final Profilo FERMAVETRO = new Profilo("RX70.511", "Fermavetro", Categoria.FERMAVETRO);
    private static final Profilo TRAVERSO_PORTA = new Profilo("RX70.402", "Traverso porta", Categoria.MONTANTE);

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

    /** Il sistema RX 700 con le sue tipologie. */
    public static Sistema sistema() {
        return new Sistema("RX 700", FamigliaSistema.BATTENTE, List.of(
                finestraUnaAnta(),
                finestraDueAnte(),
                finestraUnaAntaAlt(),
                finestraDueAnteAlt(),
                portaUnaAnta(),
                portaDueAnte()));
    }

    // #1 finestra 1 anta: telaio L,H ×2 (45°); anta RX70.203 L−40, H−40 ×2 (45°). Fermavetro senza quota.
    private static Tipologia finestraUnaAnta() {
        return new Tipologia("Finestra a 1 anta", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perL(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(40), 2, TAGLIO_45_45)));
    }

    // #2 finestra 2 ante: anta RX70.203 L/2−22 ×4, H−40 ×4 (45°); montante RX60.301 H−110 ×1 (90°).
    private static Tipologia finestraDueAnte() {
        return new Tipologia("Finestra a 2 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA, perMezzaL(22), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA, perH(40), 4, TAGLIO_45_45),
                new RegolaTaglio("Montante d'incontro", MONTANTE, perH(110), 1, TAGLIO_90_90)));
    }

    // #3 finestra 1 anta alternativo: anta RX70.201 L−40/H−40 ×2 (45°); fermavetro L−184/H−184 ×2 (90°).
    private static Tipologia finestraUnaAntaAlt() {
        return new Tipologia("Finestra a 1 anta (alternativo)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA_ALT, perL(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA_ALT, perH(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perL(184), 2, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184), 2, TAGLIO_90_90)));
    }

    // #4 finestra 2 ante alternativo: anta RX70.201 L/2−22 ×4, H−40 ×4 (45°);
    // fermavetro L/2−162 ×4, H−184 ×4 (90°); montante RX70.301 H−110 ×1 (90°).
    private static Tipologia finestraDueAnteAlt() {
        return new Tipologia("Finestra a 2 ante (alternativo)", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA_ALT, perMezzaL(22), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA_ALT, perH(40), 4, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perMezzaL(162), 4, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (lato verticale)", FERMAVETRO, perH(184), 4, TAGLIO_90_90),
                new RegolaTaglio("Montante d'incontro", MONTANTE_ALT, perH(110), 1, TAGLIO_90_90)));
    }

    // #5 porta 1 anta: anta RX70.201 L−40/H−40 ×2 (45°); fermavetro L−184 ×4, H−HF−92 ×2, HF−188 ×2 (90°);
    // traverso porta RX70.402 L−130 ×1 (90°).
    private static Tipologia portaUnaAnta() {
        return new Tipologia("Porta a 1 anta", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA_ALT, perL(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA_ALT, perH(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perL(184), 4, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(92), 2, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(188), 2, TAGLIO_90_90),
                new RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perL(130), 1, TAGLIO_90_90)));
    }

    // #6 porta 2 ante: anta RX70.201 L/2−22 ×4, H−40 ×2 (45°); fermavetro L/2−162 ×8, H−HF−92 ×4,
    // HF−188 ×4 (90°); montante RX70.301 H−110 ×1 (90°); traverso porta L/2−112 ×2 (90°).
    private static Tipologia portaDueAnte() {
        return new Tipologia("Porta a 2 ante", List.of(
                new RegolaTaglio("Telaio (lato orizzontale)", TELAIO, perL(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Telaio (lato verticale)", TELAIO, perH(0), 2, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato orizzontale)", ANTA_ALT, perMezzaL(22), 4, TAGLIO_45_45),
                new RegolaTaglio("Anta (lato verticale)", ANTA_ALT, perH(40), 2, TAGLIO_45_45),
                new RegolaTaglio("Fermavetro (lato orizzontale)", FERMAVETRO, perMezzaL(162), 8, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sopra traverso)", FERMAVETRO, hMenoHF(92), 4, TAGLIO_90_90),
                new RegolaTaglio("Fermavetro (verticale, sotto traverso)", FERMAVETRO, perHF(188), 4, TAGLIO_90_90),
                new RegolaTaglio("Montante d'incontro", MONTANTE_ALT, perH(110), 1, TAGLIO_90_90),
                new RegolaTaglio("Traverso porta", TRAVERSO_PORTA, perMezzaL(112), 2, TAGLIO_90_90)));
    }
}
