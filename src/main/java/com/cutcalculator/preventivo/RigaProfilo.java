package com.cutcalculator.preventivo;

import com.cutcalculator.dominio.Profilo;

/**
 * Una riga del preventivo: il fabbisogno di un singolo profilo, aggregato dal piano di taglio.
 *
 * @param profilo        il profilo a cui si riferisce la riga
 * @param barreNuove     quante barre nuove da 6,5 m comprare
 * @param avanziUsati    quanti spezzoni di magazzino sono stati riutilizzati
 * @param lunghezzaNuova metri lineari di barra nuova ordinati (somma delle lunghezze delle barre nuove)
 * @param sfrido         sfrido complessivo del profilo, su <b>tutte</b> le barre (nuove e avanzi)
 */
public record RigaProfilo(Profilo profilo, int barreNuove, int avanziUsati,
        double lunghezzaNuova, double sfrido) {
}
