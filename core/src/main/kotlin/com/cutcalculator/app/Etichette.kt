package com.cutcalculator.app

import com.cutcalculator.dominio.Categoria
import com.cutcalculator.dominio.Materiale
import com.cutcalculator.dominio.Profilo
import com.cutcalculator.dominio.TipoTaglio
import com.cutcalculator.dominio.Varianti

/**
 * Le etichette con cui profili, materiali, misure e tagli si mostrano all'utente — le stesse per il
 * client testuale e per quello grafico, così le due UI "parlano" allo stesso modo e non si ricopiano
 * i formatter.
 *
 * Solo testo neutro: niente cornici ASCII (quelle restano nella CLI) e niente widget.
 *
 * È un `object` con i metodi `@JvmStatic`: da Java si continua a scrivere `Etichette.profilo(...)`.
 */
object Etichette {

    /** Profilo: `"RX70.101 Telaio"`. */
    @JvmStatic
    fun profilo(profilo: Profilo): String = profilo.codice + " " + profilo.descrizione

    /** Materiale (profilo + colore): `"RX70.101 Telaio [BIANCO]"`. */
    @JvmStatic
    fun materiale(materiale: Materiale): String =
        profilo(materiale.profilo) + " [" + materiale.colore.nome() + "]"

    /**
     * Una misura del modello (sempre in mm) nell'unità scelta dall'utente, senza simbolo:
     * `"6500"`, `"650"`, `"6.5"`.
     */
    @JvmStatic
    fun misura(mm: Double, unita: Unita): String = unita.formatta(mm)

    /** Come [misura], ma col simbolo dell'unità: `"6.5 m"`. */
    @JvmStatic
    fun misuraConSimbolo(mm: Double, unita: Unita): String = unita.conSimbolo(mm)

    /** Il nome di un ruolo come lo legge l'utente: `"Telaio"`, `"Montante d'incontro"`. */
    @JvmStatic
    fun ruolo(ruolo: Categoria): String = when (ruolo) {
        Categoria.TELAIO -> "Telaio"
        Categoria.ANTA -> "Anta"
        Categoria.MONTANTE -> "Montante d'incontro"
        Categoria.TRAVERSO -> "Traverso"
        Categoria.FERMAVETRO -> "Fermavetro"
        Categoria.ASTINA -> "Astina"
    }

    /**
     * Le varianti scelte in chiaro: `"Telaio: Z maggiorato, Anta: Maggiorata"`, oppure `"-"` se non
     * ce ne sono. È la forma da mettere in una colonna di tabella.
     */
    @JvmStatic
    fun variantiBrevi(varianti: Varianti): String {
        if (varianti.vuota()) {
            return "-"
        }
        return varianti.scelte().entries.joinToString(", ") { (categoria, variante) ->
            ruolo(categoria) + ": " + variante.nome
        }
    }

    /**
     * Le stesse varianti pronte da **appendere** a una riga già scritta:
     * `" [Telaio: Z maggiorato]"`. Stringa **vuota** se non ce ne sono, così le tipologie senza
     * varianti (e tutti gli scorrevoli) restano scritte esattamente come prima.
     */
    @JvmStatic
    fun varianti(varianti: Varianti): String =
        if (varianti.vuota()) "" else " [" + variantiBrevi(varianti) + "]"

    /** Tipo di taglio in forma breve: `"90/45 DX"`. */
    @JvmStatic
    fun taglio(taglio: TipoTaglio): String = when (taglio) {
        TipoTaglio.TAGLIO_45_45 -> "45/45"
        TipoTaglio.TAGLIO_90_90 -> "90/90"
        TipoTaglio.TAGLIO_90_45_DX -> "90/45 DX"
        TipoTaglio.TAGLIO_90_45_SX -> "90/45 SX"
    }
}
