package com.cutcalculator.pianificazione;

import com.cutcalculator.formule.Distinta;

/**
 * La {@link Distinta} di un singolo ordine dentro un calcolo globale, col nome dell'ordine accanto.
 * <p>
 * I pezzi restano divisi per commessa anche quando il calcolo li unisce: l'ottimizzatore li mescola
 * sulle barre (ed è il suo mestiere), ma <b>chi taglia vuole sapere di chi è ogni pezzo</b>, e chi
 * consegna deve poter riguardare la distinta di un ordine solo.
 *
 * @param ordine   il nome dell'ordine
 * @param distinta i suoi pezzi e le sue lastre
 */
public record DistintaOrdine(String ordine, Distinta distinta) {
}
