package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Materiale;
import com.cutcalculator.dominio.Pezzo;
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

    /**
     * @param piano  il piano da aggregare
     * @param vetri  le lastre della distinta, da aggregare per misura
     * @param soglia lunghezza minima perché un residuo conti come ritaglio recuperabile
     */
    public Preventivo genera(PianoDiTaglio piano, List<Vetro> vetri, double soglia) {
        List<RigaProfilo> righe = new ArrayList<>();

        for (Map.Entry<Materiale, List<BarraTagliata>> gruppo : piano.perMateriale().entrySet()) {
            Materiale materiale = gruppo.getKey();
            int barreNuove = 0;
            int avanziUsati = 0;
            double lunghezzaNuova = 0;
            double sfrido = 0;
            int ritagli = 0;
            double lunghezzaRecuperabile = 0;

            double costo = 0;
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
                    costo += costoDi(barra);
                }
            }
            righe.add(new RigaProfilo(materiale.profilo(), materiale.colore(), barreNuove,
                    avanziUsati, lunghezzaNuova, sfrido, ritagli, lunghezzaRecuperabile, costo));
        }
        return new Preventivo(righe, aggregaVetri(vetri));
    }

    /**
     * Quanto costa una barra <b>nuova</b>: si paga tutta, sfrido compreso, e il conto lo dividono i
     * pezzi che ci stanno sopra in proporzione alla loro lunghezza — ciascuno al <b>proprio</b> €/kg.
     * <p>
     * Non è un dettaglio pedante: il prezzo dell'alluminio arriva dal serramento (cambia col colore),
     * quindi la stessa barra può contenere pezzi pagati a prezzi diversi, e un solo prezzo per riga
     * darebbe un totale sbagliato. Gli <b>avanzi</b> non entrano nel conto: sono già di proprietà.
     */
    private static double costoDi(BarraTagliata barra) {
        double lunghezzaPezzi = barra.pezzi().stream().mapToDouble(Pezzo::lunghezza).sum();
        if (lunghezzaPezzi <= 0) {
            return 0;   // barra nuova senza pezzi: non dovrebbe succedere, ma non si paga
        }
        double pesoBarra = barra.profilo().peso(barra.lunghezzaBarra());
        double costo = 0;
        for (Pezzo pezzo : barra.pezzi()) {
            costo += pesoBarra * (pezzo.lunghezza() / lunghezzaPezzi) * pezzo.prezzoAlChilo();
        }
        return costo;
    }

    /**
     * Lastre della stessa misura fuse in una riga sola, con le quantità sommate, nell'ordine in cui
     * compaiono. Le misure identiche escono da formule identiche, quindi il confronto esatto tra
     * double è affidabile qui: nessuna tolleranza da tarare.
     */
    private static List<RigaVetro> aggregaVetri(List<Vetro> vetri) {
        Map<String, RigaVetro> perMisura = new LinkedHashMap<>();
        for (Vetro vetro : vetri) {
            // Il prezzo entra nella chiave: stessa misura ma €/mq diverso = due righe diverse,
            // altrimenti il costo di una si perderebbe dentro l'altra.
            String chiave = vetro.lunghezza() + "x" + vetro.larghezza()
                    + "@" + vetro.prezzi().alMqVetro();
            perMisura.merge(chiave,
                    new RigaVetro(vetro.lunghezza(), vetro.larghezza(), vetro.quantita(), vetro.costo()),
                    (vecchia, nuova) -> new RigaVetro(vecchia.lunghezza(), vecchia.larghezza(),
                            vecchia.quantita() + nuova.quantita(), vecchia.costo() + nuova.costo()));
        }
        return new ArrayList<>(perMisura.values());
    }
}
