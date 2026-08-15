package com.cutcalculator.ottimizzatore

/**
 * Le euristiche di taglio fra cui l'utente può scegliere, con quella **predefinita** ([MULTI_START]).
 * È il registro che mancava: le implementazioni di [Ottimizzatore] esistevano già tutte, ma nessuno
 * le istanziava e si finiva sempre e solo su [BestFitDecreasing].
 *
 * Sta qui e non in `app` perché conosce le classi concrete, che sono sue vicine di package: chi la
 * usa (controller, UI, impostazioni) ne vede solo il nome e [crea].
 *
 * È un'**impostazione**, come l'unità di misura: si sceglie una volta, si persiste e vale per i
 * calcoli successivi. Cambiarla non tocca né i dati né i calcoli già fatti — cambia il piano del
 * *prossimo*.
 */
enum class Strategia(
    private val nome: String,
    private val spiegazione: String,
    private val costruttore: () -> Ottimizzatore
) {
    MULTI_START(
        "Multi-start casuale",
        "prova 50 piani con i pezzi in ordine diverso e tiene il migliore (meno barre nuove," +
                " poi meno sfrido sparso). Il piu' lento, ma non puo' venire peggio degli altri",
        ::MultiStartCasuale
    ),

    MIGLIOR_INCASTRO(
        "Miglior incastro (best-fit)",
        "un piano solo, senza tentativi: ogni pezzo va nella barra che resta piu' piena." +
                " Gli avanzi, essendo corti, tendono a essere riusati per primi",
        ::BestFitDecreasing
    ),

    PRIMA_CHE_ENTRA(
        "Prima barra che entra (first-fit)",
        "piu' semplice e prevedibile: il pezzo va nella prima barra aperta in cui ci sta." +
                " Di solito spreca un po' di piu'",
        ::FirstFitDecreasing
    ),

    SVUOTA_MAGAZZINO(
        "Svuota magazzino",
        "completa prima gli avanzi: apre una barra nuova solo quando nessuno spezzone basta." +
                " Libera la rastrelliera, al prezzo di qualche mm di sfrido in piu'",
        ::SvuotaMagazzino
    );

    /** Il nome da mostrare in un menu: `"Miglior incastro (best-fit)"`. */
    fun nome(): String = nome

    /** Una riga che dice cosa cambia a sceglierla, per chi non conosce le euristiche di taglio. */
    fun spiegazione(): String = spiegazione

    /** Nome e spiegazione insieme, per gli elenchi che hanno spazio (il menu della CLI). */
    fun descrizione(): String = "$nome - $spiegazione"

    /** Un ottimizzatore pronto all'uso. Sono senza stato: se ne crea uno per calcolo. */
    fun crea(): Ottimizzatore = costruttore()

    companion object {
        /**
         * L'euristica usata se l'utente non ne sceglie una: il **multi-start** (scelta dell'utente,
         * 2026-08-15). Non può fare peggio del best-fit — parte proprio da quel piano e lo tiene se
         * nessun tentativo casuale lo batte — e sull'ordine reale l'ha battuto. Costa ~4× il tempo,
         * che su commesse di questa taglia non si vede.
         */
        @JvmField
        val PREDEFINITA: Strategia = MULTI_START

        /**
         * La strategia con questo nome (`MIGLIOR_INCASTRO`, ...), o la [PREDEFINITA] se il testo è
         * assente o sconosciuto: un file di impostazioni corretto a mano non deve far crashare
         * l'app (stessa regola di `Unita.daNome`).
         */
        @JvmStatic
        fun daNome(nome: String?): Strategia {
            if (nome == null) {
                return PREDEFINITA
            }
            return entries.firstOrNull { it.name.equals(nome.trim(), ignoreCase = true) }
                ?: PREDEFINITA
        }
    }
}
