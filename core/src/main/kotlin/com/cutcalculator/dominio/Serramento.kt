package com.cutcalculator.dominio

/**
 * Un serramento richiesto dall'utente: una [Tipologia] (la ricetta) applicata a misure concrete
 * ([Dimensione]), in un certo [Colore] e quantità.
 *
 * È l'input della pipeline: "voglio `quantita` serramenti di questa tipologia, grandi L×H, di questo
 * colore". Il `GeneratoreDistinta` lo trasformerà nella lista dei pezzi, tutti del colore del
 * serramento (un serramento = un colore; un ordine può però contenere serramenti di colori diversi).
 *
 * Porta anche il **listino** con cui valorizzarlo: i prezzi del materiale cambiano con la finitura,
 * e la finitura si sceglie qui — quindi €/kg e €/mq stanno accanto al colore, non in
 * un'impostazione globale. I pezzi e le lastre che ne nascono se lo portano dietro.
 *
 * Porta infine le [Varianti] scelte: la ricetta è la stessa, ma il telaio può essere a Z o
 * maggiorato e l'anta maggiorata, e allora si taglia un altro profilo e le quote di ciò che gli sta
 * dentro si accorciano. Anche questo è per serramento, come il colore e i prezzi: nello stesso
 * ordine possono convivere un telaio piccolo e uno maggiorato.
 *
 * @param tipologia  quale scheda di taglio applicare
 * @param colore     la finitura: tutti i pezzi del serramento sono di questo colore
 * @param dimensione misure finite (L×H, più HF per le porte)
 * @param quantita   quanti serramenti identici
 * @param prezzi     €/kg dell'alluminio e €/mq del vetro **per questo serramento**
 * @param varianti   quali profili sostituire ai base, ruolo per ruolo; vuoto = tutto base
 */
@JvmRecord
data class Serramento(
    val tipologia: Tipologia,
    val colore: Colore,
    val dimensione: Dimensione,
    val quantita: Int,
    val prezzi: Prezzi,
    val varianti: Varianti
) {
    /** Con i profili base della tipologia: nessuna variante scelta. */
    constructor(
        tipologia: Tipologia, colore: Colore, dimensione: Dimensione, quantita: Int, prezzi: Prezzi
    ) : this(tipologia, colore, dimensione, quantita, prezzi, Varianti.NESSUNA)

    /** Senza listino: i costi di questo serramento verranno zero. */
    constructor(tipologia: Tipologia, colore: Colore, dimensione: Dimensione, quantita: Int) :
            this(tipologia, colore, dimensione, quantita, Prezzi.NESSUNO)

    /** Comodo per le finestre: misure L×H senza altezza parziale (HF = 0). */
    constructor(tipologia: Tipologia, colore: Colore, L: Double, H: Double, quantita: Int) :
            this(tipologia, colore, Dimensione(L, H), quantita)
}
