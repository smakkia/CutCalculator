package com.cutcalculator.dominio;

import java.util.EnumMap;
import java.util.Map;

/**
 * Le {@link Variante varianti} scelte per un {@link Serramento}: al più una per ruolo
 * (telaio, anta, ...). È l'unico posto dove vive la regola del <b>contenimento</b>, così il
 * generatore della distinta non deve saperne nulla.
 * <p>
 * La regola: una variante restringe tutto ciò che le sta <b>dentro</b>, cioè le categorie di
 * {@link Categoria#livello() livello} maggiore del suo. Il telaio maggiorato accorcia anta, montante,
 * traverso, fermavetro e vetro; l'anta maggiorata accorcia traverso, fermavetro e vetro ma <b>non</b>
 * il montante d'incontro, che sta fra le ante e non dentro; nessuna delle due accorcia sé stessa.
 * I restringimenti di ruoli diversi <b>si sommano</b> (telaio maggiorato + anta maggiorata = 48 per
 * lato sul vetro).
 */
public record Varianti(Map<Categoria, Variante> scelte) {

    /** Nessuna scelta: tutti i profili base, quote invariate. */
    public static final Varianti NESSUNA = new Varianti(Map.of());

    /** Copia difensiva, e ogni variante deve stare sotto la chiave del proprio ruolo. */
    public Varianti {
        Map<Categoria, Variante> copia = new EnumMap<>(Categoria.class);
        scelte.forEach((ruolo, variante) -> {
            if (variante.ruolo() != ruolo) {
                throw new IllegalArgumentException("La variante " + variante.nome() + " è del ruolo "
                        + variante.ruolo() + ", non " + ruolo);
            }
            copia.put(ruolo, variante);
        });
        scelte = Map.copyOf(copia);
    }

    /** Una sola scelta, chiavata dal ruolo del suo profilo. */
    public static Varianti di(Variante variante) {
        return new Varianti(Map.of(variante.ruolo(), variante));
    }

    /** Questa scelta più un'altra (o la sostituzione di quella dello stesso ruolo). */
    public Varianti con(Variante variante) {
        // Non si usa EnumMap(Map): rifiuta le mappe vuote, e partire da NESSUNA è il caso normale.
        Map<Categoria, Variante> nuove = new EnumMap<>(Categoria.class);
        nuove.putAll(scelte);
        nuove.put(variante.ruolo(), variante);
        return new Varianti(nuove);
    }

    public boolean vuota() {
        return scelte.isEmpty();
    }

    /**
     * Il profilo da tagliare davvero al posto di {@code base}: quello della variante scelta per il suo
     * ruolo, se c'è. Un profilo di un ruolo senza varianti torna identico.
     */
    public Profilo profiloDi(Profilo base) {
        Variante variante = scelte.get(base.categoria());
        return variante == null ? base : variante.profilo();
    }

    /**
     * Quanti millimetri, <b>per estremità</b>, perde un pezzo di questa categoria: la somma dei
     * restringimenti delle varianti che lo contengono.
     */
    public double restringimentoDi(Categoria categoria) {
        return restringimentoAlLivello(categoria.livello());
    }

    /** Come sopra per il vetro, che non è un profilo ma sta dentro tutto. */
    public double restringimentoDelVetro() {
        return restringimentoAlLivello(Categoria.LIVELLO_VETRO);
    }

    private double restringimentoAlLivello(int livello) {
        return scelte.values().stream()
                .filter(v -> v.ruolo().livello() < livello)
                .mapToDouble(Variante::restringimento)
                .sum();
    }
}
