package com.cutcalculator.dominio

/**
 * Categoria funzionale di un profilo, usata per raggruppare i pezzi nella distinta
 * e nel preventivo (es. mostrare insieme tutti i pezzi di telaio).
 *
 * L'elenco parte dalle categorie viste nel Gruppo B dei cataloghi e si estende
 * man mano che se ne trascrivono di nuove.
 *
 * Ogni categoria porta anche il suo **livello di annidamento**: il telaio contiene tutto, dentro
 * ci stanno l'anta e il montante d'incontro, e dentro l'anta il traverso, il fermavetro e il vetro.
 * Serve alle [Variante] varianti: un profilo più largo **restringe ciò che gli sta dentro**, e il
 * livello dice cosa. È anche il motivo per cui [TRAVERSO] è separato da [MONTANTE], che pure gli
 * somiglia: il traverso di una porta sta *dentro* l'anta e si accorcia con lei, il montante
 * d'incontro sta *tra* le ante e no.
 *
 * Il livello è una property privata più una funzione `livello()`: così l'accessor visto da Java
 * resta quello di prima e i chiamanti non cambiano.
 */
enum class Categoria(private val livello: Int) {
    TELAIO(0),
    ANTA(1),
    MONTANTE(1),
    TRAVERSO(2),
    FERMAVETRO(2),
    ASTINA(2);

    /** 0 = il telaio (contiene tutto); più alto = più interno. */
    fun livello(): Int = livello

    /** `true` se un profilo di questa categoria racchiude uno dell'altra (e quindi la restringe). */
    fun contiene(altra: Categoria): Boolean = altra.livello > livello

    companion object {
        /** Il livello del vetro: sta dentro tutto, quindi si restringe con qualunque variante. */
        const val LIVELLO_VETRO: Int = 2
    }
}
