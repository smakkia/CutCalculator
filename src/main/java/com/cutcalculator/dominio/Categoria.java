package com.cutcalculator.dominio;

/**
 * Categoria funzionale di un profilo, usata per raggruppare i pezzi nella distinta
 * e nel preventivo (es. mostrare insieme tutti i pezzi di telaio).
 * <p>
 * L'elenco parte dalle categorie viste nel Gruppo B dei cataloghi e si estende
 * man mano che se ne trascrivono di nuove.
 */
public enum Categoria {
    TELAIO,
    ANTA,
    FERMAVETRO,
    ALTRO
}
