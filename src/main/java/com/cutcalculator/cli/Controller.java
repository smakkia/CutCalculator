package com.cutcalculator.cli;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.formule.GeneratoreDistinta;
import com.cutcalculator.ottimizzatore.BestFitDecreasing;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.preventivo.GeneratorePreventivo;
import com.cutcalculator.preventivo.Preventivo;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo <b>stato</b> dell'applicazione e le operazioni su di esso, del tutto indipendenti dalla
 * UI (non stampa e non legge nulla). Tiene il {@link Catalogo}, il magazzino degli
 * {@link Avanzo avanzi} e la lista degli {@link Ordine ordini}, ed espone le azioni per
 * modificarli e per far scorrere la pipeline ({@link #calcola}).
 * <p>
 * Una view — oggi {@link CliView}, domani una GUI — ci si appoggia sopra: legge lo stato e
 * invoca le operazioni, senza duplicarne la gestione. Il magazzino è condiviso da tutti gli
 * ordini; {@link #calcola} riusa gli avanzi correnti senza consumarli.
 */
public final class Controller {

    private final Catalogo catalogo;
    private final List<Avanzo> magazzino = new ArrayList<>();
    private final List<Ordine> ordini = new ArrayList<>();

    public Controller(Catalogo catalogo) {
        this.catalogo = catalogo;
    }

    // --- Lettura dello stato -----------------------------------------------------------

    public Catalogo catalogo() {
        return catalogo;
    }

    /** Vista in sola lettura del magazzino (l'ordine è quello di inserimento). */
    public List<Avanzo> magazzino() {
        return List.copyOf(magazzino);
    }

    /** Vista in sola lettura degli ordini (l'ordine è quello di inserimento). */
    public List<Ordine> ordini() {
        return List.copyOf(ordini);
    }

    // --- Operazioni sul magazzino ------------------------------------------------------

    public void aggiungiAvanzo(Avanzo avanzo) {
        magazzino.add(avanzo);
    }

    /** Rimuove l'avanzo alla posizione data e lo restituisce. */
    public Avanzo rimuoviAvanzo(int indice) {
        return magazzino.remove(indice);
    }

    // --- Operazioni sugli ordini -------------------------------------------------------

    /** Crea un ordine con questo nome, lo aggiunge e lo restituisce (l'{@link Ordine} è modificabile). */
    public Ordine nuovoOrdine(String nome) {
        Ordine ordine = new Ordine(nome);
        ordini.add(ordine);
        return ordine;
    }

    public void rimuoviOrdine(Ordine ordine) {
        ordini.remove(ordine);
    }

    // --- Pipeline ----------------------------------------------------------------------

    /**
     * Fa scorrere l'ordine lungo le tre fasi (distinta → piano → preventivo) riusando gli
     * avanzi correnti del magazzino, e restituisce i tre risultati insieme.
     *
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    public Risultato calcola(Ordine ordine) {
        Distinta distinta = new GeneratoreDistinta().genera(ordine);
        PianoDiTaglio piano = new BestFitDecreasing().ottimizza(distinta, magazzino);
        Preventivo preventivo = new GeneratorePreventivo().genera(piano);
        return new Risultato(distinta, piano, preventivo);
    }

    /** I tre output della pipeline per un ordine. */
    public record Risultato(Distinta distinta, PianoDiTaglio piano, Preventivo preventivo) {
    }
}
