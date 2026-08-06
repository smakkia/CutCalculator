package com.cutcalculator.ottimizzatore;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Pezzo;
import com.cutcalculator.formule.Distinta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.UnaryOperator;

/**
 * Nucleo condiviso delle euristiche di taglio 1D: il ciclo "un profilo alla volta,
 * ordina i pezzi, metti ognuno nella barra scelta, apri barre nuove quando serve".
 * Le strategie concrete cambiano solo due cose, passate qui come parametri:
 * <ul>
 *   <li>l'<b>ordine</b> in cui considerare i pezzi ({@link #DECRESCENTE}, {@link #casuale});</li>
 *   <li>il <b>selettore</b> della barra per un pezzo ({@link #PRIMO_CHE_ENTRA},
 *       {@link #MIGLIOR_INCASTRO}, {@link #AVANZI_PRIMA}).</li>
 * </ul>
 * Gli avanzi sono sempre in testa alle barre aperte (dal più corto), così vengono
 * riusati prima di comprare barre nuove.
 */
final class Impacchettatore {

    private Impacchettatore() {
    }

    /** Sceglie in quale barra aperta mettere un pezzo; {@code null} = apri una barra nuova. */
    @FunctionalInterface
    interface SelettoreBarra {
        BarraTagliata scegli(List<BarraTagliata> aperte, Pezzo pezzo);
    }

    /** Pezzi dal più lungo al più corto (la "Decreasing" delle euristiche *-Decreasing). */
    static final UnaryOperator<List<Pezzo>> DECRESCENTE = pezzi -> {
        pezzi.sort(Comparator.comparingDouble(Pezzo::lunghezza).reversed());
        return pezzi;
    };

    /** First-Fit: la prima barra aperta in cui il pezzo entra. */
    static final SelettoreBarra PRIMO_CHE_ENTRA = (aperte, pezzo) ->
            aperte.stream().filter(barra -> barra.entra(pezzo)).findFirst().orElse(null);

    /** Best-Fit: fra le barre in cui entra, quella che resta più piena (sfrido minore). */
    static final SelettoreBarra MIGLIOR_INCASTRO = (aperte, pezzo) ->
            aperte.stream().filter(barra -> barra.entra(pezzo))
                    .min(Comparator.comparingDouble(BarraTagliata::sfrido)).orElse(null);

    /** Come best-fit ma completa prima gli avanzi; le barre nuove solo se nessun avanzo entra. */
    static final SelettoreBarra AVANZI_PRIMA = (aperte, pezzo) -> {
        BarraTagliata avanzo = aperte.stream().filter(barra -> barra.avanzo() && barra.entra(pezzo))
                .min(Comparator.comparingDouble(BarraTagliata::sfrido)).orElse(null);
        if (avanzo != null) {
            return avanzo;
        }
        return aperte.stream().filter(barra -> !barra.avanzo() && barra.entra(pezzo))
                .min(Comparator.comparingDouble(BarraTagliata::sfrido)).orElse(null);
    };

    /** Ordine casuale (per il multi-start): mescola i pezzi con l'{@code rng} dato. */
    static UnaryOperator<List<Pezzo>> casuale(Random rng) {
        return pezzi -> {
            Collections.shuffle(pezzi, rng);
            return pezzi;
        };
    }

    static PianoDiTaglio impacchetta(Distinta distinta, List<Avanzo> avanzi, double lunghezzaBarraStandard,
            UnaryOperator<List<Pezzo>> ordine, SelettoreBarra selettore) {
        Map<Materiale, List<Avanzo>> avanziPerMateriale = raggruppaAvanzi(avanzi);
        List<BarraTagliata> risultato = new ArrayList<>();

        for (Map.Entry<Materiale, List<Pezzo>> gruppo : distinta.perMateriale().entrySet()) {
            Materiale materiale = gruppo.getKey();
            List<Pezzo> pezzi = ordine.apply(new ArrayList<>(gruppo.getValue()));
            List<BarraTagliata> aperte = daAvanzi(materiale, avanziPerMateriale.get(materiale));

            for (Pezzo pezzo : pezzi) {
                BarraTagliata scelta = selettore.scegli(aperte, pezzo);
                if (scelta == null) {
                    scelta = apriNuova(materiale, lunghezzaBarraStandard, pezzo);
                    aperte.add(scelta);
                }
                scelta.aggiungi(pezzo);
            }
            for (BarraTagliata barra : aperte) {
                if (!barra.pezzi().isEmpty()) {
                    risultato.add(barra);
                }
            }
        }
        return new PianoDiTaglio(risultato);
    }

    static Map<Materiale, List<Avanzo>> raggruppaAvanzi(List<Avanzo> avanzi) {
        Map<Materiale, List<Avanzo>> perMateriale = new LinkedHashMap<>();
        for (Avanzo avanzo : avanzi) {
            perMateriale.computeIfAbsent(avanzo.materiale(), m -> new ArrayList<>()).add(avanzo);
        }
        return perMateriale;
    }

    /** Espande gli avanzi di un materiale (dal più corto) in barre vuote riusabili. */
    static List<BarraTagliata> daAvanzi(Materiale materiale, List<Avanzo> avanzi) {
        List<BarraTagliata> barre = new ArrayList<>();
        if (avanzi == null) {
            return barre;
        }
        List<Avanzo> ordinati = new ArrayList<>(avanzi);
        ordinati.sort(Comparator.comparingDouble(Avanzo::lunghezza));
        for (Avanzo avanzo : ordinati) {
            for (int i = 0; i < avanzo.quantita(); i++) {
                barre.add(new BarraTagliata(materiale.profilo(), materiale.colore(), avanzo.lunghezza(), true));
            }
        }
        return barre;
    }

    /** Apre una barra nuova; se il pezzo non ci sta nemmeno lì, è fisicamente impossibile. */
    static BarraTagliata apriNuova(Materiale materiale, double lunghezzaBarraStandard, Pezzo pezzo) {
        BarraTagliata nuova = new BarraTagliata(materiale.profilo(), materiale.colore(), lunghezzaBarraStandard, false);
        if (!nuova.entra(pezzo)) {
            throw new IllegalArgumentException("Pezzo da " + pezzo.lunghezza()
                    + " mm troppo lungo per la barra standard da " + lunghezzaBarraStandard
                    + " mm (profilo " + materiale.profilo().codice() + " " + materiale.colore().nome() + ")");
        }
        return nuova;
    }
}
