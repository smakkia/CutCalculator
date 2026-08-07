package com.cutcalculator.pianificazione;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.preventivo.Preventivo;

import java.util.List;

/**
 * Il risultato del <b>calcolo globale</b>: tutti gli ordini vengono <b>uniti come se fossero uno
 * solo</b> e ottimizzati insieme (vedi {@link PianificatoreOrdini}), così i pezzi di ordini diversi
 * possono condividere la stessa barra e lo sfrido cala.
 * <p>
 * Contiene gli {@link Ordine ordini} inclusi (solo per riferimento/stampa), il <b>piano di taglio
 * unico</b> di tutti insieme, il {@link Preventivo} aggregato e il <b>magazzino aggiornato</b> dopo
 * aver consumato gli avanzi usati e rimesso i ritagli sopra soglia.
 * <p>
 * È un valore immutabile: chi lo riceve (il controller) decide se applicarlo, cioè sostituire il
 * magazzino con {@link #magazzinoAggiornato()} e persisterlo.
 */
public record EvasioneOrdini(
        List<Ordine> ordini,
        PianoDiTaglio piano,
        Preventivo preventivoTotale,
        List<Avanzo> magazzinoAggiornato) {

    public EvasioneOrdini {
        ordini = List.copyOf(ordini);
        magazzinoAggiornato = List.copyOf(magazzinoAggiornato);
    }
}
