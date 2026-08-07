package com.cutcalculator.dominio;

/**
 * Un avanzo (spezzone) già in magazzino: una barra corta, di proprietà, da cui
 * l'ottimizzatore può ricavare pezzi <b>prima</b> di aprire una barra nuova.
 * <p>
 * È legato a un {@link Materiale} (profilo + {@link Colore}): da un avanzo bianco del profilo X
 * si tagliano solo pezzi bianchi del profilo X. La {@code quantita} permette di dichiarare più
 * avanzi identici in un colpo solo (es. "3 spezzoni del telaio bianco da 1200 mm").
 */
public record Avanzo(Profilo profilo, Colore colore, double lunghezza, int quantita) {

    /**
     * Lunghezza minima (mm) perché un residuo di taglio valga la pena di essere tenuto: sotto
     * questa misura lo spezzone è scarto, sopra rientra in magazzino come avanzo riusabile.
     * È una regola di dominio, quindi sta qui: la usano sia chi aggiorna il magazzino dopo
     * un'evasione sia il preventivo, che dichiara in anticipo quanti ritagli si recupereranno.
     */
    public static final double LUNGHEZZA_MINIMA_RIUSO = 500.0;

    /** Il {@link Materiale} (profilo + colore): la chiave con cui l'avanzo viene raggruppato. */
    public Materiale materiale() {
        return new Materiale(profilo, colore);
    }
}
