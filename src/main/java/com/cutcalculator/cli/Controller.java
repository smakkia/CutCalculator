package com.cutcalculator.cli;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.formule.GeneratoreDistinta;
import com.cutcalculator.ottimizzatore.BestFitDecreasing;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.persistenza.ArchivioMagazzino;
import com.cutcalculator.pianificazione.EvasioneOrdini;
import com.cutcalculator.pianificazione.PianificatoreOrdini;
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
    private final ArchivioMagazzino archivio;
    private final List<Avanzo> magazzino = new ArrayList<>();
    private final List<Ordine> ordini = new ArrayList<>();

    /** Solo in memoria, senza persistenza: comodo per test o usi effimeri. */
    public Controller(Catalogo catalogo) {
        this(catalogo, null);
    }

    /**
     * Collega un {@link ArchivioMagazzino}: il magazzino viene <b>caricato</b> da disco
     * all'avvio e <b>risalvato</b> a ogni modifica. Con {@code archivio} null resta in memoria.
     */
    public Controller(Catalogo catalogo, ArchivioMagazzino archivio) {
        this.catalogo = catalogo;
        this.archivio = archivio;
        if (archivio != null) {
            magazzino.addAll(archivio.carica());
        }
    }

    // --- Lettura dello stato -----------------------------------------------------------

    public Catalogo catalogo() {
        return catalogo;
    }

    /** Vista in sola lettura del magazzino (l'ordine è quello di inserimento). */
    public List<Avanzo> magazzino() {
        return List.copyOf(magazzino);
    }

    /** Numero totale di spezzoni a magazzino: la somma delle quantità di ogni {@link Avanzo}. */
    public int totaleAvanzi() {
        return magazzino.stream().mapToInt(Avanzo::quantita).sum();
    }

    /** Vista in sola lettura degli ordini (l'ordine è quello di inserimento). */
    public List<Ordine> ordini() {
        return List.copyOf(ordini);
    }

    // --- Operazioni sul magazzino ------------------------------------------------------

    public void aggiungiAvanzo(Avanzo avanzo) {
        magazzino.add(avanzo);
        salva();
    }

    /**
     * Toglie {@code quantita} spezzoni dall'avanzo alla posizione data. Se sono tutti (o più),
     * rimuove l'intera riga; altrimenti la sostituisce con una copia dalla quantità ridotta
     * (l'{@link Avanzo} è immutabile).
     *
     * @return l'avanzo aggiornato che resta a magazzino, oppure {@code null} se la riga è sparita
     */
    public Avanzo rimuoviAvanzo(int indice, int quantita) {
        Avanzo avanzo = magazzino.get(indice);
        Avanzo rimasto;
        if (quantita >= avanzo.quantita()) {
            magazzino.remove(indice);
            rimasto = null;
        } else {
            rimasto = new Avanzo(avanzo.profilo(), avanzo.colore(),
                    avanzo.lunghezza(), avanzo.quantita() - quantita);
            magazzino.set(indice, rimasto);
        }
        salva();
        return rimasto;
    }

    /**
     * Svuota il magazzino: rimuove <b>tutti</b> gli spezzoni presenti e persiste.
     *
     * @return quanti spezzoni sono stati rimossi in totale
     */
    public int svuotaMagazzino() {
        int totale = totaleAvanzi();
        magazzino.clear();
        salva();
        return totale;
    }

    /** Persiste il magazzino se c'è un archivio collegato (altrimenti resta solo in memoria). */
    private void salva() {
        if (archivio != null) {
            archivio.salva(magazzino);
        }
    }

    // --- Operazioni sugli ordini -------------------------------------------------------

    /**
     * Crea un ordine con questo nome, lo aggiunge e lo restituisce (l'{@link Ordine} è modificabile).
     * Il colore si sceglie sul singolo {@link com.cutcalculator.dominio.Serramento}, non sull'ordine.
     */
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

    /** La soglia (mm) sopra cui un ritaglio rientra in magazzino come nuovo avanzo. */
    public double sogliaRitaglio() {
        return PianificatoreOrdini.SOGLIA_RITAGLIO_DEFAULT;
    }

    /**
     * Calcolo <b>globale</b>: pianifica <b>tutti</b> gli ordini insieme sul magazzino condiviso
     * (ogni avanzo va a un solo ordine, vedi {@link PianificatoreOrdini}) e poi <b>applica</b> il
     * risultato — sostituisce il magazzino con quello aggiornato (avanzi usati consumati, ritagli
     * sopra soglia rientrati) e lo persiste. A differenza di {@link #calcola}, questo <b>consuma</b>.
     *
     * @throws IllegalArgumentException se un pezzo è più lungo della barra standard
     */
    public EvasioneOrdini evadiOrdini() {
        EvasioneOrdini evasione = new PianificatoreOrdini().pianifica(ordini, magazzino);
        magazzino.clear();
        magazzino.addAll(evasione.magazzinoAggiornato());
        salva();
        return evasione;
    }

    /** I tre output della pipeline per un ordine. */
    public record Risultato(Distinta distinta, PianoDiTaglio piano, Preventivo preventivo) {
    }
}
