package com.cutcalculator.dominio;

/**
 * Una lastra di vetro con le sue misure finite, in millimetri.
 * <p>
 * È il pendant 2D del {@link Pezzo}: mentre il pezzo è un'asta (una sola misura utile, la
 * lunghezza), il vetro è una superficie e si misura in {@code lunghezza × larghezza}. Per ora
 * non entra nell'ottimizzatore (niente nesting 2D): si dichiara e se ne calcola l'<b>area</b>,
 * che è quanto serve a stimare il materiale.
 * <p>
 * A differenza del {@link Pezzo} il vetro <b>non</b> è esploso: lastre identiche restano una
 * riga sola con la {@code quantita}, perché nessuno deve piazzarle una a una in una barra.
 * <p>
 * Unità: le misure sono in mm (come tutto il modello), quindi le aree vengono in mm². Il vetro
 * però si compra a metro quadro: {@link #areaMq()} e {@link #areaTotaleMq()} fanno la conversione.
 * <p>
 * I due lati seguono le colonne delle "distinte di taglio vetri" del catalogo, che sono
 * <b>H</b> e <b>L</b>: la {@code lunghezza} è la quota in altezza (colonna H), la {@code larghezza}
 * quella in larghezza (colonna L). Non è detto che la lunghezza sia il lato maggiore — una finestra
 * bassa e larga dà una lastra più larga che alta.
 *
 * @param lunghezza il lato della lastra ricavato dall'altezza H (mm)
 * @param larghezza il lato della lastra ricavato dalla larghezza L (mm)
 * @param quantita  quante lastre identiche servono
 */
public record Vetro(double lunghezza, double larghezza, int quantita) {

    /** Millimetri quadri in un metro quadro: 1 m² = 1000 mm × 1000 mm. */
    private static final double MMQ_PER_MQ = 1_000_000.0;

    /** Comodo per la lastra singola. */
    public Vetro(double lunghezza, double larghezza) {
        this(lunghezza, larghezza, 1);
    }

    /** Area di <b>una</b> lastra, in mm². */
    public double area() {
        return lunghezza * larghezza;
    }

    /** Area di tutte le lastre di questa riga ({@code area × quantita}), in mm². */
    public double areaTotale() {
        return area() * quantita;
    }

    /** Area di una lastra in metri quadri (l'unità con cui il vetro si acquista). */
    public double areaMq() {
        return area() / MMQ_PER_MQ;
    }

    /** Area totale della riga in metri quadri. */
    public double areaTotaleMq() {
        return areaTotale() / MMQ_PER_MQ;
    }

    /** Perimetro di una lastra (mm): serve ai fermavetri e alle guarnizioni. */
    public double perimetro() {
        return 2 * (lunghezza + larghezza);
    }
}
