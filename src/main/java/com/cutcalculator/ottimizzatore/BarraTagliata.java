package com.cutcalculator.ottimizzatore;

import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.dominio.Profilo;

import java.util.ArrayList;
import java.util.List;

/**
 * Una singola barra grezza presa a magazzino, con i pezzi già tagliati dentro.
 * È l'unità che l'ottimizzatore riempie, un pezzo alla volta, finché c'è spazio.
 * <p>
 * Una barra appartiene a <b>un solo {@link Materiale}</b> (profilo + colore): da lei si possono
 * ricavare solo pezzi dello stesso profilo e colore (un telaio bianco non esce da una barra d'anta,
 * né da una barra bronzo). È <b>mutabile</b>, perché l'ottimizzatore ci aggiunge pezzi
 * progressivamente.
 * <p>
 * Può essere un <b>avanzo</b> già in magazzino (gratis, di proprietà) oppure una
 * <b>barra nuova</b> da comprare: lo dice il flag {@link #avanzo()}. Il preventivo
 * conta come materiale da acquistare solo le barre nuove.
 * <p>
 * Modello del kerf (spessore della lama): semplice, ogni pezzo "costa"
 * {@code lunghezza + kerf}, quindi {@code occupato = Σ lunghezze + n·kerf}. Anche il
 * primo pezzo paga un kerf, il che tiene conto di un po' di spuntatura ai bordi.
 * <p>
 * A questo si somma, per i profili che lo dichiarano, il <b>sovrapprezzo dei tagli in diagonale</b>
 * ({@link Pezzo#extraKerf()}): un profilo maggiorato tagliato a 45° mangia più barra di uno piccolo.
 * Non è più vero, quindi, che tutti i pezzi costano uguale a parità di lunghezza — il conto passa da
 * {@code n · KERF} alla somma pezzo per pezzo.
 */
public class BarraTagliata {

    /** Spessore della lama a ogni taglio, in mm: ogni pezzo "costa" la sua lunghezza piu' questo. */
    public static final double KERF = 4.0;

    /** Tolleranza per i confronti tra double (le lunghezze sono in mm). */
    private static final double EPS = 1e-9;

    private final Profilo profilo;
    private final Colore colore;
    private final double lunghezzaBarra;
    private final boolean avanzo;
    private final List<Pezzo> pezzi = new ArrayList<>();
    /**
     * Lo spazio consumato, tenuto aggiornato a ogni {@link #aggiungi}: è il numero più consultato
     * dell'ottimizzatore. Il selettore best-fit chiama {@link #entra} e {@link #sfrido} su ogni
     * barra aperta per ogni pezzo, e ricalcolare la somma da capo ogni volta rendeva
     * l'impacchettamento <b>quadratico</b> nel numero di pezzi. I pezzi non si tolgono mai da una
     * barra, quindi tenere il totale non può disallinearsi.
     */
    private double occupato;

    public BarraTagliata(Profilo profilo, Colore colore, double lunghezzaBarra, boolean avanzo) {
        this.profilo = profilo;
        this.colore = colore;
        this.lunghezzaBarra = lunghezzaBarra;
        this.avanzo = avanzo;
    }

    /** Spazio consumato: somma delle lunghezze più il kerf (base + diagonali) di ogni pezzo. */
    public double occupato() {
        return occupato;
    }

    /**
     * Quanta barra costa un pezzo: la sua lunghezza, il kerf della lama e il sovrapprezzo dei tagli
     * a 45°, che dipende dalla sezione del profilo (vedi {@link Pezzo#extraKerf()}).
     */
    private double costoDi(Pezzo pezzo) {
        return pezzo.lunghezza() + KERF + pezzo.extraKerf();
    }

    /** Spazio ancora libero sulla barra (lo scarto se la si chiudesse così com'è). */
    public double sfrido() {
        return lunghezzaBarra - occupato();
    }

    /**
     * Il pezzo ci starebbe? Verifica solo la lunghezza: si assume che il profilo sia
     * quello giusto (l'ottimizzatore raggruppa i pezzi per profilo prima di riempire).
     */
    public boolean entra(Pezzo pezzo) {
        return occupato() + costoDi(pezzo) <= lunghezzaBarra + EPS;
    }

    /**
     * Colloca il pezzo sulla barra.
     *
     * @throws IllegalArgumentException se il pezzo è di un materiale diverso (profilo o colore)
     * @throws IllegalStateException    se il pezzo non ci sta (chiama prima {@link #entra})
     */
    public void aggiungi(Pezzo pezzo) {
        if (!pezzo.materiale().equals(materiale())) {
            throw new IllegalArgumentException(
                    "Pezzo " + pezzo.profilo().codice() + " " + pezzo.colore().nome()
                            + " non collocabile su una barra " + profilo.codice() + " " + colore.nome());
        }
        if (!entra(pezzo)) {
            throw new IllegalStateException(
                    "Il pezzo (" + pezzo.lunghezza() + " mm) non entra: sfrido residuo "
                            + sfrido() + " mm");
        }
        pezzi.add(pezzo);
        occupato += costoDi(pezzo);
    }

    public Profilo profilo() {
        return profilo;
    }

    public Colore colore() {
        return colore;
    }

    /** Il {@link Materiale} (profilo + colore) di questa barra: la chiave con cui la si raggruppa. */
    public Materiale materiale() {
        return new Materiale(profilo, colore);
    }

    public double lunghezzaBarra() {
        return lunghezzaBarra;
    }

    /** {@code true} se è uno spezzone di magazzino riusato, {@code false} se è una barra nuova da comprare. */
    public boolean avanzo() {
        return avanzo;
    }

    public double kerf() {
        return KERF;
    }

    /** Peso (kg) della barra <b>intera</b>, sfrido compreso: è quello che si compra e si trasporta. */
    public double peso() {
        return profilo.peso(lunghezzaBarra);
    }

    /** Peso (kg) dei soli pezzi utili ricavati: il netto, senza sfrido né kerf. */
    public double pesoPezzi() {
        return pezzi.stream().mapToDouble(Pezzo::peso).sum();
    }

    /**
     * Costo (€) della barra intera: peso × prezzo al chilo. Si paga tutta la barra, anche la
     * parte che finirà in sfrido — per questo il prezzo si calcola sulla lunghezza grezza.
     */
    public double prezzo() {
        return profilo.prezzo(lunghezzaBarra);
    }

    /** Vista in sola lettura dei pezzi collocati, nell'ordine di taglio. */
    public List<Pezzo> pezzi() {
        return List.copyOf(pezzi);
    }
}
