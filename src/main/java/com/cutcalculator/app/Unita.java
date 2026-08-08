package com.cutcalculator.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * L'unità con cui le misure vengono <b>mostrate e lette</b> nella UI: millimetri (predefinita),
 * centimetri o metri.
 * <p>
 * Riguarda solo la presentazione: il modello e i calcoli restano <b>sempre in millimetri</b>
 * (le formule dei cataloghi sono in mm), quindi ogni valore che entra viene riportato in mm con
 * {@link #versoMm(double)} e ogni valore che esce viene convertito con {@link #daMm(double)}.
 * Cambiare unità non tocca i dati su disco.
 * <p>
 * I decimali sono tarati perché un millimetro resti sempre visibile (0,1 cm; 0,001 m) e gli zeri
 * finali inutili non vengono stampati: 6500 mm sono {@code "6500"}, {@code "650"}, {@code "6.5"}.
 */
public enum Unita {

    MILLIMETRI("mm", "millimetri", 1.0, 1),
    CENTIMETRI("cm", "centimetri", 10.0, 2),
    METRI("m", "metri", 1000.0, 3);

    /** L'unità usata se l'utente non ne sceglie una. */
    public static final Unita PREDEFINITA = MILLIMETRI;

    private final String simbolo;
    private final String nome;
    private final double mmPerUnita;
    private final int decimali;

    Unita(String simbolo, String nome, double mmPerUnita, int decimali) {
        this.simbolo = simbolo;
        this.nome = nome;
        this.mmPerUnita = mmPerUnita;
        this.decimali = decimali;
    }

    /** Il simbolo da mostrare accanto ai numeri: {@code mm}, {@code cm}, {@code m}. */
    public String simbolo() {
        return simbolo;
    }

    /** Il nome esteso, per i menu: {@code "millimetri (mm)"}. */
    public String descrizione() {
        return nome + " (" + simbolo + ")";
    }

    /** Da questa unità ai millimetri del modello: quello che l'utente scrive diventa mm. */
    public double versoMm(double valore) {
        return valore * mmPerUnita;
    }

    /** Dai millimetri del modello a questa unità: quello che l'utente legge. */
    public double daMm(double mm) {
        return mm / mmPerUnita;
    }

    /** Il valore convertito e formattato, senza simbolo: {@code "6.5"}. */
    public String formatta(double mm) {
        BigDecimal valore = BigDecimal.valueOf(daMm(mm))
                .setScale(decimali, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return valore.toPlainString();
    }

    /** Il valore convertito e formattato, col simbolo: {@code "6.5 m"}. */
    public String conSimbolo(double mm) {
        return formatta(mm) + " " + simbolo;
    }

    /**
     * L'unità con questo nome ({@code MILLIMETRI}, ...), o la {@link #PREDEFINITA} se il testo è
     * assente o sconosciuto: un file di impostazioni corretto a mano non deve far crashare l'app.
     */
    public static Unita daNome(String nome) {
        if (nome == null) {
            return PREDEFINITA;
        }
        for (Unita unita : values()) {
            if (unita.name().equalsIgnoreCase(nome.trim())) {
                return unita;
            }
        }
        return PREDEFINITA;
    }
}
