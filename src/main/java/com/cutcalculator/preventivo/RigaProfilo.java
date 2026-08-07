package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Profilo;

/**
 * Una riga del preventivo: il fabbisogno di un singolo profilo <b>in un colore</b>, aggregato dal
 * piano di taglio. Profili uguali ma di colore diverso fanno righe distinte: si comprano barre di
 * quel preciso colore.
 * <p>
 * Lo <b>sfrido</b> è tutto quello che avanza; i <b>ritagli</b> ne sono la parte buona, cioè i
 * residui lunghi almeno {@link Avanzo#LUNGHEZZA_MINIMA_RIUSO}, che non sono scarto ma torneranno
 * in magazzino come avanzi riusabili.
 *
 * @param profilo              il profilo a cui si riferisce la riga
 * @param colore               il colore/finitura di quelle barre
 * @param barreNuove           quante barre nuove da 6,5 m comprare
 * @param avanziUsati          quanti spezzoni di magazzino sono stati riutilizzati
 * @param lunghezzaNuova       metri lineari di barra nuova ordinati (somma delle lunghezze delle barre nuove)
 * @param sfrido               sfrido complessivo, su <b>tutte</b> le barre di quel materiale (nuove e avanzi)
 * @param ritagliRecuperabili  quanti residui sopra soglia rientreranno in magazzino
 * @param lunghezzaRecuperabile lunghezza totale di quei residui (la parte di sfrido che non è scarto)
 */
public record RigaProfilo(Profilo profilo, Colore colore, int barreNuove, int avanziUsati,
        double lunghezzaNuova, double sfrido,
        int ritagliRecuperabili, double lunghezzaRecuperabile) {

    /** Lo sfrido che non si recupera: spezzoni troppo corti per tornare a magazzino. */
    public double scarto() {
        return sfrido - lunghezzaRecuperabile;
    }
}
