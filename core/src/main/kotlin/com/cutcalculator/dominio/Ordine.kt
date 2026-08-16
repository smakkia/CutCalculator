package com.cutcalculator.dominio

import java.util.Collections

/**
 * Una commessa: l'insieme dei serramenti richiesti dall'utente. Verrà ottimizzata tutta insieme,
 * così pezzi di serramenti diversi possono condividere una barra.
 *
 * A differenza degli altri mattoni del dominio è **mutabile**, perché è l'input che l'utente compila
 * e corregge: si può aggiungere, togliere o sostituire un singolo serramento senza rifare l'ordine.
 * I [Serramento] restano immutabili, quindi "modificarne uno" significa sostituirlo con la versione
 * corretta.
 *
 * Un ordine può contenere serramenti di **colori diversi**: il colore è un attributo del singolo
 * [Serramento] (tutti i pezzi di quel serramento sono di quel colore), non dell'ordine.
 */
class Ordine(nome: String) {

    private var nome: String = nome
    private val serramenti: MutableList<Serramento> = ArrayList()
    private var calcolato: Boolean = false

    fun nome(): String = nome

    /**
     * `true` se l'ordine è **già stato calcolato**, cioè è entrato in un calcolo globale che ne ha
     * scalato il materiale dal magazzino. Serve a non ricalcolarlo (e riconsumarlo) una seconda
     * volta: il calcolo globale prende solo gli ordini *da calcolare*.
     *
     * Toccare i serramenti riporta l'ordine tra quelli da calcolare: il contenuto è cambiato, quindi
     * il conto va rifatto. **Attenzione**: il ricalcolo riparte dall'ordine intero, non dal solo
     * delta, quindi il materiale già scalato verrà contato di nuovo — il modello non tiene lo
     * storico di ciò che è stato evaso.
     */
    fun calcolato(): Boolean = calcolato

    /** Segna l'ordine come calcolato: lo fa il calcolo globale dopo averlo evaso. */
    fun segnaCalcolato() {
        calcolato = true
    }

    /** Rimette l'ordine tra quelli da calcolare (serve al ricaricamento da file). */
    fun segnaDaCalcolare() {
        calcolato = false
    }

    /** Rinomina l'ordine: correggerne il nome è una delle modifiche che l'utente può fare. */
    fun rinomina(nome: String) {
        this.nome = nome   // il nome non cambia i pezzi: lo stato di calcolo resta com'è
    }

    /** Aggiunge un serramento in coda; ritorna `this` per l'uso a catena. */
    fun aggiungi(serramento: Serramento): Ordine {
        serramenti.add(serramento)
        calcolato = false
        return this
    }

    /** Corregge un singolo serramento sostituendolo con una nuova versione. */
    fun sostituisci(indice: Int, serramento: Serramento) {
        serramenti[indice] = serramento
        calcolato = false
    }

    /** Rimuove il serramento alla posizione data. */
    fun rimuovi(indice: Int) {
        serramenti.removeAt(indice)
        calcolato = false
    }

    /** Vista in sola lettura dei serramenti; le modifiche passano dai metodi sopra. */
    fun serramenti(): List<Serramento> = Collections.unmodifiableList(ArrayList(serramenti))

    /** Numero totale di serramenti richiesti: la somma delle quantità di ogni riga. */
    fun totaleSerramenti(): Int = serramenti.sumOf { it.quantita }
}
