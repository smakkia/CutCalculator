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
 * @param prezzi      il listino del {@link Serramento} da cui viene: il €/kg può cambiare da un
 *                    serramento all'altro (colori diversi, fornitori diversi), quindi ogni pezzo
 *                    si porta dietro il proprio invece di attingere a un listino unico
 */
public record Pezzo(Profilo profilo, Colore colore, double lunghezza,
        TipoTaglio tipoTaglio, String descrizione, Prezzi prezzi) {

    /** Senza listino: il pezzo si può tagliare ma non valorizzare. */
    public Pezzo(Profilo profilo, Colore colore, double lunghezza,
            TipoTaglio tipoTaglio, String descrizione) {
        this(profilo, colore, lunghezza, tipoTaglio, descrizione, Prezzi.NESSUNO);
    }

    /** Il €/kg da applicare a questo pezzo: quello del serramento da cui viene. */
    public double prezzoAlChilo() {
        return prezzi.alChiloDi(profilo);
    }

    /** Il {@link Materiale} (profilo + colore): la chiave con cui l'ottimizzatore raggruppa i pezzi. */
    public Materiale materiale() {
        return new Materiale(profilo, colore);
    }

    /**
     * Millimetri di barra che questo pezzo consuma <b>oltre</b> alla sua lunghezza e al kerf base:
     * il sovrapprezzo dei tagli in diagonale, {@code extraKerf45 del profilo × estremità a 45°}.
     * <p>
     * Vale 0 per i profili base e per qualunque pezzo tagliato 90/90, quindi finché il catalogo non
     * dichiara profili maggiorati il conto della barra non cambia di un millimetro.
     */
    public double extraKerf() {
        return profilo.extraKerf45() * tipoTaglio.tagliA45();
    }

    /** Peso del pezzo (kg): lunghezza × peso lineare del profilo. */
    public double peso() {
        return profilo.peso(lunghezza);
    }

    /** Costo del solo pezzo (€): peso × €/kg. Il costo <b>vero</b> è quello della barra intera. */
    public double prezzo() {
        return peso() * prezzoAlChilo();
    }
}
