package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * L'ultimo motore della pipeline: aggrega un {@link PianoDiTaglio} in un {@link Preventivo},
 * una riga per {@link Materiale} (profilo + colore). Per ogni materiale conta le barre nuove e gli
 * avanzi riusati, somma le lunghezze delle barre nuove e lo sfrido di tutte le barre, e separa
 * dallo sfrido i <b>ritagli recuperabili</b> — i residui lunghi almeno la soglia, che dopo
 * l'evasione torneranno in magazzino come nuovi avanzi.
 */
public class GeneratorePreventivo {

    /** Come {@link #genera(PianoDiTaglio, double)} con la soglia di riuso di default. */
    public Preventivo genera(PianoDiTaglio piano) {
        return genera(piano, Avanzo.LUNGHEZZA_MINIMA_RIUSO);
    }

    /**
     * @param piano  il piano da aggregare
     * @param soglia lunghezza minima perché un residuo conti come ritaglio recuperabile e non
     *               come scarto (stessa regola che applica poi il pianificatore al magazzino)
     */
    public Preventivo genera(PianoDiTaglio piano, double soglia) {
        List<RigaProfilo> righe = new ArrayList<>();

        for (Map.Entry<Materiale, List<BarraTagliata>> gruppo : piano.perMateriale().entrySet()) {
            Materiale materiale = gruppo.getKey();
            int barreNuove = 0;
            int avanziUsati = 0;
            double lunghezzaNuova = 0;
            double sfrido = 0;
            int ritagli = 0;
            double lunghezzaRecuperabile = 0;

            for (BarraTagliata barra : gruppo.getValue()) {
                sfrido += barra.sfrido();
                if (barra.sfrido() >= soglia) {
                    ritagli++;
                    lunghezzaRecuperabile += barra.sfrido();
                }
                if (barra.avanzo()) {
                    avanziUsati++;
                } else {
                    barreNuove++;
                    lunghezzaNuova += barra.lunghezzaBarra();
                }
            }
            righe.add(new RigaProfilo(materiale.profilo(), materiale.colore(),
                    barreNuove, avanziUsati, lunghezzaNuova, sfrido, ritagli, lunghezzaRecuperabile));
        }
        return new Preventivo(righe);
    }
}
