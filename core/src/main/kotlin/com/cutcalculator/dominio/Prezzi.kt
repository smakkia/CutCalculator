package com.cutcalculator.dominio

/**
 * Il **listino** dell'utente: quanto costa il materiale, nelle due unità con cui lo si compra
 * davvero — l'alluminio **a peso** (€/kg) e il vetro **a superficie** (€/mq).
 *
 * Non è un dato di catalogo ma un'impostazione: cambia col fornitore e col mercato, quindi la
 * inserisce l'utente e viene persistita insieme alle altre preferenze. Il [Profilo] può però
 * portare un proprio `prezzoAlChilo`: se c'è, vince su questo listino generale (vedi [alChiloDi]),
 * così un profilo particolarmente caro può avere il suo prezzo senza costringere a riscrivere tutto
 * il resto.
 *
 * Un prezzo a **zero** significa "non impostato": i costi verranno zero, non sbagliati.
 *
 * @param alChiloBarre prezzo dell'alluminio in €/kg, valido per i profili che non ne hanno uno proprio
 * @param alMqVetro    prezzo del vetro in €/mq
 */
@JvmRecord
data class Prezzi(val alChiloBarre: Double, val alMqVetro: Double) {

    init {
        require(alChiloBarre >= 0 && alMqVetro >= 0) {
            "I prezzi non possono essere negativi: $alChiloBarre EUR/kg, $alMqVetro EUR/mq"
        }
    }

    /** `true` se c'è almeno un prezzo impostato: sotto questa condizione i costi hanno senso. */
    fun impostati(): Boolean = alChiloBarre > 0 || alMqVetro > 0

    /** Il €/kg da applicare a un profilo: il suo, se ce l'ha, altrimenti quello generale. */
    fun alChiloDi(profilo: Profilo): Double =
        if (profilo.prezzoAlChilo > 0) profilo.prezzoAlChilo else alChiloBarre

    /** Il costo (€) di `mq` metri quadri di vetro. */
    fun costoVetro(mq: Double): Double = mq * alMqVetro

    companion object {
        /** Listino vuoto: nessun prezzo impostato, tutti i costi vengono zero. */
        @JvmField
        val NESSUNO: Prezzi = Prezzi(0.0, 0.0)
    }
}
