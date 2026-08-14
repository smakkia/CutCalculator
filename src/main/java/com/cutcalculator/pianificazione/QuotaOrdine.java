package com.cutcalculator.pianificazione;

import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.dominio.Profilo;

import java.util.List;

/**
 * Quanto costa un singolo ordine dentro un calcolo globale: <b>una voce per ordine</b>, non il suo
 * dettaglio per materiale (quello resta nelle tabelle complessive, che sono l'unico posto dove barre
 * e sfrido hanno un senso).
 * <p>
 * Il calcolo globale unisce gli ordini in un piano solo, e le barre finiscono condivise: la stessa
 * barra può contenere pezzi di commesse diverse. Non esiste quindi "il preventivo di quell'ordine"
 * come somma di barre intere — esiste la sua <b>quota</b>.
 * <p>
 * <b>Come si divide.</b> Per ogni materiale (profilo + colore) il peso e il costo della riga globale
 * si spartiscono tra gli ordini in proporzione ai <b>millimetri di pezzi</b> che ciascuno ha chiesto
 * di quel materiale. Chi taglia di più paga di più, e con la stessa proporzione si spartiscono le
 * barre intere e lo sfrido: sono il prezzo dell'averle messe insieme, e non sarebbe corretto
 * addossarlo a chi per caso è finito nella barra più vuota. Le quote di tutti gli ordini
 * <b>sommano esattamente al totale</b>.
 * <p>
 * Il <b>vetro</b> non ha questo problema: le lastre non si condividono, quindi lastre, superficie e
 * costo sono quelli veri dell'ordine, non una quota.
 *
 * @param ordine           il nome dell'ordine
 * @param lunghezzaProfili millimetri di profilo tagliati per quest'ordine (la base del riparto)
 * @param pesoProfili      quota del peso (kg) delle barre nuove
 * @param costoProfili     quota del costo (€) delle barre nuove
 * @param lastre           lastre di vetro dell'ordine (esatte, non una quota)
 * @param areaVetroMq      superficie vetrata dell'ordine in m² (esatta)
 * @param costoVetro       costo del vetro dell'ordine (esatto)
 */
public record QuotaOrdine(
        String ordine,
        double lunghezzaProfili,
        double pesoProfili,
        double costoProfili,
        int lastre,
        double areaVetroMq,
        double costoVetro,
        List<Riga> righe) {

    public QuotaOrdine {
        righe = List.copyOf(righe);
    }

    /**
     * Il dettaglio per materiale dietro i totali: <b>non</b> si mostra nelle tabelle (lì basta una
     * voce per ordine) ma si <b>salva su disco</b>, perché il preventivo archiviato di una commessa
     * senza le sue righe non servirebbe a nulla.
     *
     * @param lunghezza millimetri di pezzi di questo materiale (la base del riparto)
     * @param peso      quota del peso (kg) delle barre nuove di questo materiale
     * @param costo     quota del costo (€) di quelle barre
     */
    public record Riga(Profilo profilo, Colore colore, double lunghezza, double peso, double costo) {
    }

    /** Quanto costa in tutto quest'ordine: la sua quota di alluminio più il suo vetro. */
    public double costoTotale() {
        return costoProfili + costoVetro;
    }

    /** Quota vuota, per un {@link Ordine} senza serramenti. */
    static QuotaOrdine vuota(String ordine) {
        return new QuotaOrdine(ordine, 0, 0, 0, 0, 0, 0, List.of());
    }
}
