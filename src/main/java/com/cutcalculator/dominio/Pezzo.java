package com.cutcalculator.dominio;

/**
 * Un singolo pezzo fisico già tagliato: il risultato di applicare una {@link RegolaTaglio}
 * a una {@link Dimensione}. Sta sul lato "output" della pipeline ed è l'unità che
 * l'ottimizzatore piazza nelle barre.
 * <p>
 * È "esploso": una riga con quantità N produce N oggetti {@code Pezzo} identici, così
 * l'ottimizzatore li può piazzare uno a uno.
 *
 * @param profilo     da quale profilo è ricavato (con il colore forma la chiave di raggruppamento)
 * @param colore      la finitura richiesta: eredita il colore dell'{@link Ordine}
 * @param lunghezza   lunghezza in mm, già calcolata dalla formula
 * @param tipoTaglio  come sono tagliate le due estremità
 * @param descrizione etichetta/ruolo, ereditata dalla riga di taglio
 */
public record Pezzo(Profilo profilo, Colore colore, double lunghezza,
        TipoTaglio tipoTaglio, String descrizione) {

    /** Il {@link Materiale} (profilo + colore): la chiave con cui l'ottimizzatore raggruppa i pezzi. */
    public Materiale materiale() {
        return new Materiale(profilo, colore);
    }
}
