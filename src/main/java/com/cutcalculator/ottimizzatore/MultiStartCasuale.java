package com.cutcalculator.ottimizzatore;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.formule.Distinta;

import java.util.List;
import java.util.Random;

/**
 * Meta-strategia multi-start: genera più piani candidati e tiene il migliore secondo la
 * {@link PianoDiTaglio#mediaGeometricaSfrido() media geometrica degli sfridi} (più bassa =
 * meno spreco). È l'uso previsto di quella metrica.
 * <p>
 * Parte da un candidato deterministico (best-fit su ordine decrescente), poi prova
 * {@code iterazioni} impacchettamenti best-fit con i pezzi in ordine <b>casuale</b>
 * (stesso {@code seed} ⇒ risultato riproducibile). Best-fit su ordine perturbato a volte
 * batte l'ordine decrescente puro: tenendo il migliore non si può fare peggio del baseline.
 */
public class MultiStartCasuale implements Ottimizzatore {

    private final int iterazioni;
    private final long seed;

    public MultiStartCasuale(int iterazioni, long seed) {
        this.iterazioni = iterazioni;
        this.seed = seed;
    }

    /** Default comodo: 50 tentativi, seed fisso per risultati riproducibili. */
    public MultiStartCasuale() {
        this(50, 0L);
    }

    @Override
    public PianoDiTaglio ottimizza(Distinta distinta, List<Avanzo> avanzi, double lunghezzaBarraStandard) {
        PianoDiTaglio migliore = Impacchettatore.impacchetta(distinta, avanzi, lunghezzaBarraStandard,
                Impacchettatore.DECRESCENTE, Impacchettatore.MIGLIOR_INCASTRO);
        double miglioreMedia = migliore.mediaGeometricaSfrido();

        Random rng = new Random(seed);
        for (int i = 0; i < iterazioni; i++) {
            PianoDiTaglio candidato = Impacchettatore.impacchetta(distinta, avanzi, lunghezzaBarraStandard,
                    Impacchettatore.casuale(rng), Impacchettatore.MIGLIOR_INCASTRO);
            double media = candidato.mediaGeometricaSfrido();
            if (media < miglioreMedia) {
                miglioreMedia = media;
                migliore = candidato;
            }
        }
        return migliore;
    }
}
