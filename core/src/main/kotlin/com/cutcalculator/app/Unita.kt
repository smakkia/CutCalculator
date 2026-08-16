package com.cutcalculator.app

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * L'unità con cui le misure vengono **mostrate e lette** nella UI: millimetri (predefinita),
 * centimetri o metri.
 *
 * Riguarda solo la presentazione: il modello e i calcoli restano **sempre in millimetri** (le formule
 * dei cataloghi sono in mm), quindi ogni valore che entra viene riportato in mm con [versoMm] e ogni
 * valore che esce viene convertito con [daMm]. Cambiare unità non tocca i dati su disco.
 *
 * I decimali sono tarati perché un millimetro resti sempre visibile (0,1 cm; 0,001 m) e gli zeri
 * finali inutili non vengono stampati: 6500 mm sono `"6500"`, `"650"`, `"6.5"`.
 */
enum class Unita(
    private val simbolo: String,
    private val nome: String,
    private val mmPerUnita: Double,
    private val decimali: Int
) {
    MILLIMETRI("mm", "millimetri", 1.0, 1),
    CENTIMETRI("cm", "centimetri", 10.0, 2),
    METRI("m", "metri", 1000.0, 3);

    /** Il simbolo da mostrare accanto ai numeri: `mm`, `cm`, `m`. */
    fun simbolo(): String = simbolo

    /** Il nome esteso, per i menu: `"millimetri (mm)"`. */
    fun descrizione(): String = "$nome ($simbolo)"

    /** Da questa unità ai millimetri del modello: quello che l'utente scrive diventa mm. */
    fun versoMm(valore: Double): Double = valore * mmPerUnita

    /** Dai millimetri del modello a questa unità: quello che l'utente legge. */
    fun daMm(mm: Double): Double = mm / mmPerUnita

    /** Il valore convertito e formattato, senza simbolo: `"6.5"`. */
    fun formatta(mm: Double): String =
        BigDecimal.valueOf(daMm(mm))
            .setScale(decimali, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    /** Il valore convertito e formattato, col simbolo: `"6.5 m"`. */
    fun conSimbolo(mm: Double): String = formatta(mm) + " " + simbolo

    companion object {
        /** L'unità usata se l'utente non ne sceglie una. */
        @JvmField
        val PREDEFINITA: Unita = MILLIMETRI

        /**
         * L'unità con questo nome (`MILLIMETRI`, ...), o la [PREDEFINITA] se il testo è assente o
         * sconosciuto: un file di impostazioni corretto a mano non deve far crashare l'app.
         */
        @JvmStatic
        fun daNome(nome: String?): Unita {
            if (nome == null) {
                return PREDEFINITA
            }
            return entries.firstOrNull { it.name.equals(nome.trim(), ignoreCase = true) }
                ?: PREDEFINITA
        }
    }
}
