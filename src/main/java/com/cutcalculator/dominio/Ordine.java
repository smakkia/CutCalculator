package com.cutcalculator.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Una commessa: l'insieme dei serramenti richiesti dall'utente. Verrà ottimizzata
 * tutta insieme, così pezzi di serramenti diversi possono condividere una barra.
 * <p>
 * A differenza degli altri mattoni del dominio è <b>mutabile</b>, perché è l'input che
 * l'utente compila e corregge: si può aggiungere, togliere o sostituire un singolo
 * serramento senza rifare l'ordine. I {@link Serramento} restano immutabili, quindi
 * "modificarne uno" significa sostituirlo con la versione corretta.
 * <p>
 * Un ordine può contenere serramenti di <b>colori diversi</b>: il colore è un attributo del singolo
 * {@link Serramento} (tutti i pezzi di quel serramento sono di quel colore), non dell'ordine.
 */
public class Ordine {

    private String nome;
    private final List<Serramento> serramenti = new ArrayList<>();
    private boolean calcolato;

    public Ordine(String nome) {
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }

    /**
     * {@code true} se l'ordine è <b>già stato calcolato</b>, cioè è entrato in un calcolo globale
     * che ne ha scalato il materiale dal magazzino. Serve a non ricalcolarlo (e riconsumarlo) una
     * seconda volta: il calcolo globale prende solo gli ordini <i>da calcolare</i>.
     * <p>
     * Toccare i serramenti riporta l'ordine tra quelli da calcolare: il contenuto è cambiato, quindi
     * il conto va rifatto. <b>Attenzione</b>: il ricalcolo riparte dall'ordine intero, non dal solo
     * delta, quindi il materiale già scalato verrà contato di nuovo — il modello non tiene lo
     * storico di ciò che è stato evaso.
     */
    public boolean calcolato() {
        return calcolato;
    }

    /** Segna l'ordine come calcolato: lo fa il calcolo globale dopo averlo evaso. */
    public void segnaCalcolato() {
        this.calcolato = true;
    }

    /** Rimette l'ordine tra quelli da calcolare (serve al ricaricamento da file). */
    public void segnaDaCalcolare() {
        this.calcolato = false;
    }

    /** Rinomina l'ordine: correggerne il nome è una delle modifiche che l'utente può fare. */
    public void rinomina(String nome) {
        this.nome = nome;   // il nome non cambia i pezzi: lo stato di calcolo resta com'è
    }

    /** Aggiunge un serramento in coda; ritorna {@code this} per l'uso a catena. */
    public Ordine aggiungi(Serramento serramento) {
        serramenti.add(serramento);
        calcolato = false;
        return this;
    }

    /** Corregge un singolo serramento sostituendolo con una nuova versione. */
    public void sostituisci(int indice, Serramento serramento) {
        serramenti.set(indice, serramento);
        calcolato = false;
    }

    /** Rimuove il serramento alla posizione data. */
    public void rimuovi(int indice) {
        serramenti.remove(indice);
        calcolato = false;
    }

    /** Vista in sola lettura dei serramenti; le modifiche passano dai metodi sopra. */
    public List<Serramento> serramenti() {
        return List.copyOf(serramenti);
    }

    /** Numero totale di serramenti richiesti: la somma delle quantità di ogni riga. */
    public int totaleSerramenti() {
        return serramenti.stream().mapToInt(Serramento::quantita).sum();
    }
}
