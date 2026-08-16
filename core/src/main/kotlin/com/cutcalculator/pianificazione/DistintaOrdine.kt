package com.cutcalculator.pianificazione

import com.cutcalculator.formule.Distinta

/**
 * La [Distinta] di un singolo ordine dentro un calcolo globale, col nome dell'ordine accanto.
 *
 * I pezzi restano divisi per commessa anche quando il calcolo li unisce: l'ottimizzatore li mescola
 * sulle barre (ed è il suo mestiere), ma **chi taglia vuole sapere di chi è ogni pezzo**, e chi
 * consegna deve poter riguardare la distinta di un ordine solo.
 *
 * @param ordine   il nome dell'ordine
 * @param distinta i suoi pezzi e le sue lastre
 */
@JvmRecord
data class DistintaOrdine(val ordine: String, val distinta: Distinta)
