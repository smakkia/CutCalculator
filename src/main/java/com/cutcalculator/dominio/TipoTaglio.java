package com.cutcalculator.dominio;

/**
 * Combinazione dei due tagli alle estremità di un pezzo, tenuta in un solo valore.
 * I tagli sono quasi sempre a 45° o 90°, quindi le combinazioni utili sono poche.
 * Il caso misto (un'estremità a 90°, l'altra a 45°) esiste in due versioni speculari,
 * {@link #TAGLIO_90_45_DX} e {@link #TAGLIO_90_45_SX}, perché il pezzo destro e quello
 * sinistro non sono intercambiabili. Il tipo si intuisce dalla forma del pezzo nella scheda.
 */
public enum TipoTaglio {
    TAGLIO_45_45,
    TAGLIO_90_90,
    TAGLIO_90_45_DX,
    TAGLIO_90_45_SX
}
