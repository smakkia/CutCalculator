package com.cutcalculator.dominio;

/**
 * Combinazione dei due tagli alle estremità di un pezzo, tenuta in un solo valore.
 * I tagli sono quasi sempre a 45° o 90°, quindi le combinazioni utili sono poche.
 * Il caso misto è {@link #TAGLIO_90_45}.
 */
public enum TipoTaglio {
    TAGLIO_45_45,
    TAGLIO_90_90,
    TAGLIO_90_45
}
