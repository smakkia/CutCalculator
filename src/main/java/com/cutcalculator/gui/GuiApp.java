package com.cutcalculator.gui;

import javafx.application.Application;

/**
 * Punto d'ingresso del client grafico. È volutamente una classe <b>separata</b> da {@link GuiFx}
 * e <b>non</b> estende {@code Application}: lanciando direttamente una sottoclasse di
 * {@code Application} da un jar sul classpath, la JVM si ferma con
 * <i>"JavaFX runtime components are missing"</i>. Delegando il {@code launch} da qui il problema
 * non si pone.
 * <p>
 * Avvio: {@code mvn javafx:run} (il client testuale resta su {@code mvn compile exec:java}).
 */
public final class GuiApp {

    private GuiApp() {
    }

    public static void main(String[] args) {
        Application.launch(GuiFx.class, args);
    }
}
