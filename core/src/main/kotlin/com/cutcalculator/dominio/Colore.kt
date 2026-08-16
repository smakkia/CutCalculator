package com.cutcalculator.dominio

/**
 * Il colore/finitura di un profilo: parte dell'identita fisica di ogni barra e di ogni pezzo.
 * Non e' un dettaglio cosmetico ma un **vincolo di taglio**: da una barra bianca non si ricava
 * un pezzo bronzo. Per questo il colore fa da **co-chiave** accanto al [Profilo], e la coppia
 * (profilo, colore) e' il [Materiale] con cui si raggruppa e si incastra.
 *
 * Il nome e' testo libero (un nome commerciale o un codice RAL), ma viene **normalizzato**
 * (spazi ridotti, MAIUSCOLO) cosi' `"bianco"`, `"Bianco"` e `" BIANCO "` sono lo stesso colore:
 * niente doppioni per una svista di battitura.
 *
 * Il `';'` e' **vietato**: e' il separatore del CSV del magazzino, quindi un colore che lo
 * contenesse spezzerebbe la riga e verrebbe perso al reload. Vietarlo qui, alla sorgente,
 * garantisce il round-trip su disco (vedi `ArchivioMagazzino`).
 *
 * Non e' una `data class` perche' il valore va **normalizzato prima** di diventare proprieta', e il
 * costruttore primario di una data class non puo' riscrivere i propri parametri: `equals`,
 * `hashCode` e `toString` sono quindi scritti a mano, nella stessa forma che aveva il record.
 * Il parametro e' `String?` apposta: da Java si puo' passare `null`, e il contratto e' che venga
 * rifiutato con `IllegalArgumentException`, non con la `NullPointerException` che Kotlin
 * lancerebbe da se'.
 */
class Colore(nome: String?) {

    private val nome: String = normalizza(nome)

    fun nome(): String = nome

    override fun equals(other: Any?): Boolean =
        this === other || (other is Colore && nome == other.nome)

    override fun hashCode(): Int = nome.hashCode()

    override fun toString(): String = "Colore[nome=$nome]"

    private companion object {
        private val SPAZI = Regex("\\s+")

        fun normalizza(nome: String?): String {
            require(!nome.isNullOrBlank()) { "Il colore non puo' essere vuoto" }
            val pulito = nome.trim().replace(SPAZI, " ").uppercase()
            require(!pulito.contains(';')) { "Il colore non puo' contenere ';': $pulito" }
            return pulito
        }
    }
}
