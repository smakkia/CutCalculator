package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Colore;
import com.cutcalculator.dominio.Profilo;

/**
 * Una riga del preventivo: il fabbisogno di un singolo profilo <b>in un colore</b>, aggregato dal
 * piano di taglio. Profili uguali ma di colore diverso fanno righe distinte: si comprano barre di
 * quel preciso colore.
 *
 * @param profilo        il profilo a cui si riferisce la riga
 * @param colore         il colore/finitura di quelle barre
 * @param barreNuove     quante barre nuove da 6,5 m comprare
 * @param avanziUsati    quanti spezzoni di magazzino sono stati riutilizzati
 * @param lunghezzaNuova metri lineari di barra nuova ordinati (somma delle lunghezze delle barre nuove)
 * @param sfrido         sfrido complessivo, su <b>tutte</b> le barre di quel materiale (nuove e avanzi)
 */
public record RigaProfilo(Profilo profilo, Colore colore, int barreNuove, int avanziUsati,
        double lunghezzaNuova, double sfrido) {
}
