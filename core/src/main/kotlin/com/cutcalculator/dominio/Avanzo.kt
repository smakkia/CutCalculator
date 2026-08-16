package com.cutcalculator.dominio

/**
 * Un avanzo (spezzone) già in magazzino: una barra corta, di proprietà, da cui l'ottimizzatore può
 * ricavare pezzi **prima** di aprire una barra nuova.
 *
 * È legato a un [Materiale] (profilo + [Colore]): da un avanzo bianco del profilo X si tagliano solo
 * pezzi bianchi del profilo X. La `quantita` permette di dichiarare più avanzi identici in un colpo
 * solo (es. "3 spezzoni del telaio bianco da 1200 mm").
 */
@JvmRecord
data class Avanzo(
    val profilo: Profilo,
    val colore: Colore,
    val lunghezza: Double,
    val quantita: Int
) {
    /** Il [Materiale] (profilo + colore): la chiave con cui l'avanzo viene raggruppato. */
    fun materiale(): Materiale = Materiale(profilo, colore)

    /** Peso (kg) di **uno** di questi spezzoni. */
    fun peso(): Double = profilo.peso(lunghezza)

    /** Peso (kg) di tutti gli spezzoni della riga (`peso × quantita`). */
    fun pesoTotale(): Double = peso() * quantita

    /** Valore (€) di **uno** di questi spezzoni: materiale già di proprietà, non da comprare. */
    fun prezzo(): Double = profilo.prezzo(lunghezza)

    /** Valore (€) di tutti gli spezzoni della riga. */
    fun prezzoTotale(): Double = prezzo() * quantita

    companion object {
        /**
         * Lunghezza minima (mm) perché un residuo di taglio valga la pena di essere tenuto: sotto
         * questa misura lo spezzone è scarto, sopra rientra in magazzino come avanzo riusabile.
         * È una regola di dominio, quindi sta qui: la usano sia chi aggiorna il magazzino dopo
         * un'evasione sia il preventivo, che dichiara in anticipo quanti ritagli si recupereranno.
         */
        const val LUNGHEZZA_MINIMA_RIUSO: Double = 500.0
    }
}
