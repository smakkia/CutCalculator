package com.cutcalculator.dominio;

/**
 * Categoria funzionale di un profilo, usata per raggruppare i pezzi nella distinta
 * e nel preventivo (es. mostrare insieme tutti i pezzi di telaio).
 * <p>
 * L'elenco parte dalle categorie viste nel Gruppo B dei cataloghi e si estende
 * man mano che se ne trascrivono di nuove.
 * <p>
 * Ogni categoria porta anche il suo <b>livello di annidamento</b>: il telaio contiene tutto, dentro
 * ci stanno l'anta e il montante d'incontro, e dentro l'anta il traverso, il fermavetro e il vetro.
 * Serve alle {@link Variante varianti}: un profilo più largo <b>restringe ciò che gli sta dentro</b>,
 * e il livello dice cosa. È anche il motivo per cui {@link #TRAVERSO} è separato da {@link #MONTANTE},
 * che pure gli somiglia: il traverso di una porta sta <i>dentro</i> l'anta e si accorcia con lei, il
 * montante d'incontro sta <i>tra</i> le ante e no.
 */
public enum Categoria {
    TELAIO(0),
    ANTA(1),
    MONTANTE(1),
    TRAVERSO(2),
    FERMAVETRO(2),
    ASTINA(2);

    /** Il livello del vetro: sta dentro tutto, quindi si restringe con qualunque variante. */
    public static final int LIVELLO_VETRO = 2;

    private final int livello;

    Categoria(int livello) {
        this.livello = livello;
    }

    /** 0 = il telaio (contiene tutto); più alto = più interno. */
    public int livello() {
        return livello;
    }

    /** {@code true} se un profilo di questa categoria racchiude uno dell'altra (e quindi la restringe). */
    public boolean contiene(Categoria altra) {
        return altra.livello > livello;
    }
}
