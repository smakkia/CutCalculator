package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Prezzi;
import com.cutcalculator.dominio.Vetro;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L'ultimo motore della pipeline: aggrega un {@link PianoDiTaglio} in un {@link Preventivo},
 * una riga per {@link Materiale} (profilo + colore). Per ogni materiale conta le barre nuove e gli
 * avanzi riusati, somma le lunghezze delle barre nuove e lo sfrido di tutte le barre, e separa
 * dallo sfrido i <b>ritagli recuperabili</b> — i residui lunghi almeno la soglia, che dopo
 * l'evasione torneranno in magazzino come nuovi avanzi.
 * <p>
 * Il <b>vetro</b> non passa dal piano di taglio (non si ricava da una barra): le lastre arrivano
 * direttamente dalla distinta e vengono aggregate per misura in {@link RigaVetro}.
 */
public class GeneratorePreventivo {

    /** Come {@link #genera(PianoDiTaglio, double)} con la soglia di riuso di default. */
    public Preventivo genera(PianoDiTaglio piano) {
        return genera(piano, Avanzo.LUNGHEZZA_MINIMA_RIUSO);
    }

    /** Come {@link #genera(PianoDiTaglio, List, double)} con la soglia di riuso di default. */
    public Preventivo genera(PianoDiTaglio piano, List<Vetro> vetri) {
        return genera(piano, vetri, Avanzo.LUNGHEZZA_MINIMA_RIUSO);
    }

    /**
     * @param piano  il piano da aggregare
     * @param soglia lunghezza minima perché un residuo conti come ritaglio recuperabile e non
     *               come scarto (stessa regola che applica poi il pianificatore al magazzino)
     */
    public Preventivo genera(PianoDiTaglio piano, double soglia) {
        return genera(piano, List.of(), soglia);
    }

    /** Come {@link #genera(PianoDiTaglio, List, double, Prezzi)} senza listino: solo quantità. */
    public Preventivo genera(PianoDiTaglio piano, List<Vetro> vetri, double soglia) {
        return genera(piano, vetri, soglia, Prezzi.NESSUNO);
    }

    /**
     * @param piano  il piano da aggregare
     * @param vetri  le lastre della distinta, da aggregare per misura
     * @param soglia lunghezza minima perché un residuo conti come ritaglio recuperabile
     * @param prezzi il listino con cui valorizzare barre e vetro (€/kg e €/mq)
     */
    public Preventivo genera(PianoDiTaglio piano, List<Vetro> vetri, double soglia, Prezzi prezzi) {
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
        return new Preventivo(righe, aggregaVetri(vetri), prezzi);
    }

    /**
     * Lastre della stessa misura fuse in una riga sola, con le quantità sommate, nell'ordine in cui
     * compaiono. Le misure identiche escono da formule identiche, quindi il confronto esatto tra
     * double è affidabile qui: nessuna tolleranza da tarare.
     */
    private static List<RigaVetro> aggregaVetri(List<Vetro> vetri) {
        Map<String, RigaVetro> perMisura = new LinkedHashMap<>();
        for (Vetro vetro : vetri) {
            String chiave = vetro.lunghezza() + "x" + vetro.larghezza();
            perMisura.merge(chiave,
                    new RigaVetro(vetro.lunghezza(), vetro.larghezza(), vetro.quantita()),
                    (vecchia, nuova) -> new RigaVetro(vecchia.lunghezza(), vecchia.larghezza(),
                            vecchia.quantita() + nuova.quantita()));
        }
        return new ArrayList<>(perMisura.values());
    }
}
