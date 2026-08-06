package com.cutcalculator.pianificazione;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.preventivo.Preventivo;

import java.util.List;

/**
 * Il risultato del <b>calcolo globale</b> di un insieme di ordini con magazzino condiviso
 * (vedi {@link PianificatoreOrdini}): il piano di taglio di <b>ogni</b> ordine, il
 * {@link Preventivo} aggregato (il materiale totale che serve per tutti gli ordini) e il
 * <b>magazzino aggiornato</b> dopo aver consumato gli avanzi usati e rimesso i ritagli
 * sopra soglia.
 * <p>
 * È un valore immutabile: chi lo riceve (il controller) decide se applicarlo, cioè
 * sostituire il magazzino con {@link #magazzinoAggiornato()} e persisterlo.
 */
public record EvasioneOrdini(
        List<RisultatoOrdine> perOrdine,
        Preventivo preventivoTotale,
        List<Avanzo> magazzinoAggiornato) {

    public EvasioneOrdini {
        perOrdine = List.copyOf(perOrdine);
        magazzinoAggiornato = List.copyOf(magazzinoAggiornato);
    }

    /** Il piano di taglio di un singolo ordine dentro l'evasione. */
    public record RisultatoOrdine(Ordine ordine, PianoDiTaglio piano) {
    }
}
