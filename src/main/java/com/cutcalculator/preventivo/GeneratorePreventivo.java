package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Profilo;
import com.cutcalculator.ottimizzatore.BarraTagliata;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * L'ultimo motore della pipeline: aggrega un {@link PianoDiTaglio} in un {@link Preventivo},
 * una riga per profilo. Per ogni profilo conta le barre nuove e gli avanzi riusati e somma
 * le lunghezze delle barre nuove e lo sfrido di tutte le barre.
 */
public class GeneratorePreventivo {

    public Preventivo genera(PianoDiTaglio piano) {
        List<RigaProfilo> righe = new ArrayList<>();

        for (Map.Entry<Profilo, List<BarraTagliata>> gruppo : piano.perProfilo().entrySet()) {
            Profilo profilo = gruppo.getKey();
            int barreNuove = 0;
            int avanziUsati = 0;
            double lunghezzaNuova = 0;
            double sfrido = 0;

            for (BarraTagliata barra : gruppo.getValue()) {
                sfrido += barra.sfrido();
                if (barra.avanzo()) {
                    avanziUsati++;
                } else {
                    barreNuove++;
                    lunghezzaNuova += barra.lunghezzaBarra();
                }
            }
            righe.add(new RigaProfilo(profilo, barreNuove, avanziUsati, lunghezzaNuova, sfrido));
        }
        return new Preventivo(righe);
    }
}
