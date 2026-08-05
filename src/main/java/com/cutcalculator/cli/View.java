package com.cutcalculator.cli;

import com.cutcalculator.catalogo.Catalogo;
import com.cutcalculator.dominio.Avanzo;
import com.cutcalculator.dominio.Ordine;
import com.cutcalculator.formule.Distinta;
import com.cutcalculator.ottimizzatore.PianoDiTaglio;
import com.cutcalculator.preventivo.Preventivo;

import java.util.List;

/**
 * L'astrazione del <b>front-end</b>: sa avviarsi sopra un {@link Controller} e sa
 * <b>mostrare</b> all'utente gli oggetti del dominio. Ogni realizzazione fa l'override di
 * questi metodi a modo suo: oggi {@link CliView} (testo su console), domani una {@code GuiView}
 * (JavaFX, con i widget) — senza toccare il controller né il core.
 * <p>
 * Qui ci sono solo le firme. Restano <b>fuori</b> dall'interfaccia i dettagli specifici della
 * CLI (loop dei menu, lettura da tastiera, formattazione ASCII): non avrebbero senso per una
 * view grafica, quindi vivono privati dentro {@link CliView}.
 */
public interface View {

    /** Avvia il front-end sullo stato dato e lo guida fino all'uscita dell'utente. */
    void avvia(Controller controller);

    // --- Viste sui dati: ogni realizzazione le mostra a modo suo -----------------------

    void catalogo(Catalogo catalogo);

    void ordine(Ordine ordine);

    void magazzino(List<Avanzo> avanzi);

    void distinta(Distinta distinta);

    void piano(PianoDiTaglio piano);

    void sfridi(PianoDiTaglio piano);

    void preventivo(Preventivo preventivo);
}
