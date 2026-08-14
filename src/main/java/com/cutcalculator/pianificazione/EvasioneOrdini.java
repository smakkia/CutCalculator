package com.cutcalculator.pianificazione;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.preventivo.Preventivo;

import java.util.List;

/**
 * Il risultato del <b>calcolo globale</b>: tutti gli ordini vengono <b>uniti come se fossero uno
 * solo</b> e ottimizzati insieme (vedi {@link PianificatoreOrdini}), così i pezzi di ordini diversi
 * possono condividere la stessa barra e lo sfrido cala.
 * <p>
 * Contiene gli {@link Ordine ordini} inclusi (solo per riferimento/stampa), le {@link DistintaOrdine
 * distinte} — cosa tagliare, <b>tenute divise per commessa</b> anche se il piano le unisce, perché
 * chi taglia vuole sapere di chi è ogni pezzo —, il <b>piano di taglio unico</b> (come sistemarli
 * sulle barre), il
 * {@link Preventivo} aggregato, il <b>magazzino aggiornato</b> dopo
 * aver consumato gli avanzi usati e rimesso i ritagli sopra soglia, e la <b>ripartizione per
 * ordine</b> ({@link QuotaOrdine}) di quel preventivo — che è una quota, non una somma di barre
 * intere, proprio perché le barre sono condivise.
 * <p>
 * È un valore immutabile: chi lo riceve (il controller) decide se applicarlo, cioè sostituire il
 * magazzino con {@link #magazzinoAggiornato()} e persisterlo.
 */
public record EvasioneOrdini(
        List<Ordine> ordini,
        List<DistintaOrdine> distinte,
        PianoDiTaglio piano,
        Preventivo preventivoTotale,
        List<Avanzo> magazzinoAggiornato,
        List<QuotaOrdine> quote) {

    public EvasioneOrdini {
        ordini = List.copyOf(ordini);
        distinte = List.copyOf(distinte);
        magazzinoAggiornato = List.copyOf(magazzinoAggiornato);
        quote = List.copyOf(quote);
    }

    /** Evasione essenziale, senza distinte né ripartizione: per chi costruisce il record a mano (test). */
    public EvasioneOrdini(List<Ordine> ordini, PianoDiTaglio piano, Preventivo preventivoTotale,
            List<Avanzo> magazzinoAggiornato) {
        this(ordini, List.of(), piano, preventivoTotale, magazzinoAggiornato, List.of());
    }

    /** Tutti i pezzi e tutte le lastre insieme: comodo per i totali "di tutti gli ordini". */
    public Distinta distintaUnita() {
        return new Distinta(
                distinte.stream().flatMap(d -> d.distinta().pezzi().stream()).toList(),
                distinte.stream().flatMap(d -> d.distinta().vetri().stream()).toList());
    }
}
