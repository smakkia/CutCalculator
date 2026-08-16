package com.cutcalculator.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cutcalculator.app.Etichette
import com.cutcalculator.app.Unita
import com.cutcalculator.dominio.Pezzo
import com.cutcalculator.ottimizzatore.BarraTagliata
import com.cutcalculator.preventivo.Preventivo
import com.cutcalculator.pianificazione.EvasioneOrdini
import java.util.Locale

/**
 * I **risultati** dell'ultimo calcolo, gli stessi cinque documenti del desktop: preventivo, distinta,
 * piano di taglio, sfridi e vetri.
 *
 * Sono sezioni a tendina invece che schede perché su un telefono cinque linguette non ci stanno, e
 * perché il riepilogo di ognuna (quante barre, quanti pezzi, quanto sfrido) si legge **da chiusa**:
 * si apre solo quello che serve davvero.
 *
 * Barre e sfrido hanno senso **solo** sul totale: il calcolo unisce gli ordini in un piano unico e le
 * barre finiscono condivise, quindi al singolo ordine si può attribuire una quota (in proporzione ai
 * millimetri tagliati), non un numero di barre intere. La distinta invece resta **divisa per
 * commessa**: chi taglia deve sapere di chi è ogni pezzo.
 */
@Composable
fun SchermataRisultati(vm: CutCalculatorViewModel, modifier: Modifier = Modifier) {
    val evasione = vm.ultimaEvasione
    if (evasione == null) {
        Column(modifier.fillMaxSize().padding(24.dp)) {
            Text("Nessun calcolo in questa sessione.")
            Text(
                "Vai su Ordini e tocca \"Calcola tutti gli ordini da fare\". I documenti di un ordine " +
                        "gia' calcolato si rivedono da Ordini > Vedi il calcolo, anche dopo aver " +
                        "chiuso l'app.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    val preventivo = evasione.preventivoTotale()
    val piano = evasione.piano()

    // Ogni sezione si apre per conto suo: aprirne una non chiude le altre. Il preventivo parte
    // aperto perche' e' la domanda piu' frequente ("quanto materiale devo comprare").
    var preventivoAperto by rememberSaveable { mutableStateOf(true) }
    var distintaAperta by rememberSaveable { mutableStateOf(false) }
    var pianoAperto by rememberSaveable { mutableStateOf(false) }
    var sfridiAperti by rememberSaveable { mutableStateOf(false) }
    var vetriAperti by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        sezione(
            titolo = "Preventivo",
            riepilogo = "${preventivo.totaleBarreNuove()} barre nuove, " +
                    "%.1f kg".format(Locale.ROOT, preventivo.pesoTotale()) +
                    if (preventivo.valorizzato()) {
                        ", %.2f EUR".format(Locale.ROOT, preventivo.costoTotale())
                    } else "",
            espansa = preventivoAperto,
            cambia = { preventivoAperto = !preventivoAperto }
        ) {
            preventivo(preventivo, evasione, vm.unita)
        }

        sezione(
            titolo = "Distinta di taglio",
            riepilogo = "${evasione.distinte().sumOf { it.distinta.totalePezzi() }} pezzi, " +
                    "divisi per ordine",
            espansa = distintaAperta,
            cambia = { distintaAperta = !distintaAperta }
        ) {
            distinta(evasione, vm.unita)
        }

        sezione(
            titolo = "Piano di taglio",
            riepilogo = "${piano.numeroBarre()} barre " +
                    "(${piano.barreNuove()} nuove, ${piano.numeroBarre() - piano.barreNuove()} avanzi)",
            espansa = pianoAperto,
            cambia = { pianoAperto = !pianoAperto }
        ) {
            piano(piano.barre(), vm.unita)
        }

        sezione(
            titolo = "Sfridi",
            riepilogo = Etichette.misuraConSimbolo(preventivo.sfridoTotale(), vm.unita) +
                    " in tutto, di cui " +
                    Etichette.misuraConSimbolo(preventivo.lunghezzaRecuperabileTotale(), vm.unita) +
                    " recuperabili",
            espansa = sfridiAperti,
            cambia = { sfridiAperti = !sfridiAperti }
        ) {
            sfridi(piano.barre(), vm.sogliaRitaglio(), vm.unita)
        }

        sezione(
            titolo = "Vetri",
            riepilogo = if (preventivo.totaleLastre() == 0) "nessuna lastra" else
                "${preventivo.totaleLastre()} lastre, " +
                        "%.2f mq".format(Locale.ROOT, preventivo.areaVetroTotaleMq()),
            espansa = vetriAperti,
            cambia = { vetriAperti = !vetriAperti }
        ) {
            vetri(preventivo, vm.unita)
        }
    }
}

// --- Le cinque sezioni ------------------------------------------------------------------

private fun LazyListScope.preventivo(
    preventivo: Preventivo,
    evasione: EvasioneOrdini,
    unita: Unita
) {
    item {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Totale", style = MaterialTheme.typography.titleSmall)
                Voce("Barre nuove", preventivo.totaleBarreNuove().toString())
                Voce("Avanzi riusati", preventivo.totaleAvanziUsati().toString())
                Voce("Peso da comprare", "%.1f kg".format(Locale.ROOT, preventivo.pesoTotale()))
                Voce(
                    "Sfrido",
                    Etichette.misuraConSimbolo(preventivo.sfridoTotale(), unita) +
                            " di cui recuperabili " +
                            Etichette.misuraConSimbolo(preventivo.lunghezzaRecuperabileTotale(), unita)
                )
                if (preventivo.totaleLastre() > 0) {
                    Voce(
                        "Vetro",
                        "%d lastre, %.2f mq".format(
                            Locale.ROOT, preventivo.totaleLastre(), preventivo.areaVetroTotaleMq()
                        )
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                if (preventivo.valorizzato()) {
                    Voce(
                        "Costo",
                        "%.2f EUR".format(Locale.ROOT, preventivo.costoTotale()),
                        grassetto = true
                    )
                } else {
                    Text(
                        "Nessun prezzo impostato sui serramenti: i costi restano a zero.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Con un ordine solo la ripartizione ripeterebbe la cifra qui sopra.
    if (evasione.quote().size > 1) {
        item { Sottotitolo("Per ordine") }
        items(evasione.quote().size) { indice ->
            val quota = evasione.quote()[indice]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(quota.ordine(), fontWeight = FontWeight.Bold)
                    Voce(
                        "Quota alluminio",
                        "%.1f kg — %.2f EUR".format(
                            Locale.ROOT, quota.pesoProfili(), quota.costoProfili()
                        )
                    )
                    if (quota.lastre() > 0) {
                        Voce(
                            "Vetro",
                            "%d lastre — %.2f EUR".format(
                                Locale.ROOT, quota.lastre(), quota.costoVetro()
                            )
                        )
                    }
                    Voce("Totale", "%.2f EUR".format(Locale.ROOT, quota.costoTotale()), grassetto = true)
                }
            }
        }
    }

    item { Sottotitolo("Materiali") }
    items(preventivo.righe().size) { indice ->
        val riga = preventivo.righe()[indice]
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    Etichette.profilo(riga.profilo) + " [" + riga.colore.nome() + "]",
                    fontWeight = FontWeight.Bold
                )
                Voce("Barre nuove", riga.barreNuove.toString())
                Voce("Avanzi usati", riga.avanziUsati.toString())
                Voce(
                    "Ritagli recuperabili",
                    "${riga.ritagliRecuperabili} (" +
                            Etichette.misuraConSimbolo(riga.lunghezzaRecuperabile, unita) + ")"
                )
                Voce("Scarto", Etichette.misuraConSimbolo(riga.scarto(), unita))
            }
        }
    }
}

private fun LazyListScope.distinta(evasione: EvasioneOrdini, unita: Unita) {
    if (evasione.distinte().isEmpty()) {
        item { Vuoto("Nessuna distinta: il calcolo non ha prodotto pezzi.") }
        return
    }
    evasione.distinte().forEach { perOrdine ->
        item { Sottotitolo(perOrdine.ordine) }

        val gruppi = raggruppa(perOrdine.distinta.pezzi())
        items(gruppi.size) { indice ->
            val (pezzo, quantita) = gruppi[indice]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(Etichette.materiale(pezzo.materiale()), fontWeight = FontWeight.Bold)
                    Voce(pezzo.descrizione, "x$quantita")
                    Voce(
                        Etichette.misuraConSimbolo(pezzo.lunghezza, unita),
                        Etichette.taglio(pezzo.tipoTaglio)
                    )
                }
            }
        }

        // Le lastre non passano dall'ottimizzatore (sono 2D) e non sono "esplose": ogni riga porta
        // la sua quantita'.
        val lastre = perOrdine.distinta.vetri()
        items(lastre.size) { indice ->
            val vetro = lastre[indice]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Vetro", fontWeight = FontWeight.Bold)
                    Voce(
                        Etichette.misura(vetro.lunghezza, unita) + " x " +
                                Etichette.misuraConSimbolo(vetro.larghezza, unita),
                        "x${vetro.quantita}"
                    )
                }
            }
        }
    }
}

private fun LazyListScope.piano(barre: List<BarraTagliata>, unita: Unita) {
    if (barre.isEmpty()) {
        item { Vuoto("Nessuna barra: non c'era niente da tagliare.") }
        return
    }
    items(barre.size) { indice ->
        val barra = barre[indice]
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "${indice + 1}. " + Etichette.materiale(barra.materiale()),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    (if (barra.avanzo()) "Avanzo di magazzino " else "Barra nuova ") +
                            Etichette.misuraConSimbolo(barra.lunghezzaBarra(), unita),
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                raggruppa(barra.pezzi()).forEach { (pezzo, quantita) ->
                    Voce(
                        (if (quantita > 1) "${quantita}x " else "") + pezzo.descrizione,
                        Etichette.misuraConSimbolo(pezzo.lunghezza, unita) + " " +
                                Etichette.taglio(pezzo.tipoTaglio)
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Voce("Sfrido", Etichette.misuraConSimbolo(barra.sfrido(), unita), grassetto = true)
            }
        }
    }
}

private fun LazyListScope.sfridi(barre: List<BarraTagliata>, soglia: Double, unita: Unita) {
    // Dal piu' lungo: sono quelli che decidono se il piano e' buono, e i primi a tornare utili.
    val ordinati = barre.sortedByDescending { it.sfrido() }
    if (ordinati.isEmpty()) {
        item { Vuoto("Nessuna barra, quindi nessuno sfrido.") }
        return
    }
    item {
        Text(
            "Un residuo lungo almeno ${Etichette.misuraConSimbolo(soglia, unita)} non e' scarto: " +
                    "rientra in magazzino come nuovo avanzo, utilizzabile dai calcoli successivi.",
            style = MaterialTheme.typography.bodySmall
        )
    }
    items(ordinati.size) { indice ->
        val barra = ordinati[indice]
        val recuperabile = barra.sfrido() >= soglia
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(Etichette.materiale(barra.materiale()), fontWeight = FontWeight.Bold)
                Voce(
                    if (recuperabile) "Torna a magazzino" else "Scarto",
                    Etichette.misuraConSimbolo(barra.sfrido(), unita),
                    grassetto = recuperabile
                )
            }
        }
    }
}

private fun LazyListScope.vetri(preventivo: Preventivo, unita: Unita) {
    val righe = preventivo.righeVetro()
    if (righe.isEmpty()) {
        item { Vuoto("Nessuna lastra: le tipologie calcolate non hanno le quote del vetro.") }
        return
    }
    items(righe.size) { indice ->
        val riga = righe[indice]
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    Etichette.misura(riga.lunghezza, unita) + " x " +
                            Etichette.misuraConSimbolo(riga.larghezza, unita),
                    fontWeight = FontWeight.Bold
                )
                Voce("Lastre", riga.quantita.toString())
                Voce("Superficie", "%.2f mq".format(Locale.ROOT, riga.areaTotaleMq()))
                if (riga.costo > 0) {
                    Voce("Costo", "%.2f EUR".format(Locale.ROOT, riga.costo))
                }
            }
        }
    }
}

// --- Utilità ----------------------------------------------------------------------------

/**
 * Ripiega i pezzi **esplosi** in gruppi di uguali: la pipeline produce N oggetti identici perché
 * l'ottimizzatore li piazza uno a uno, ma in officina si legge "4x traverso 1150".
 *
 * La chiave è una lista dei campi che rendono due pezzi intercambiabili — materiale, lunghezza,
 * taglio e ruolo — e non il [Pezzo] intero, che porta con sé anche il listino: due pezzi identici
 * comprati a prezzi diversi vanno tagliati insieme lo stesso.
 */
private fun raggruppa(pezzi: List<Pezzo>): List<Pair<Pezzo, Int>> =
    pezzi.groupBy { listOf(it.materiale(), it.lunghezza, it.tipoTaglio, it.descrizione) }
        .map { (_, uguali) -> uguali.first() to uguali.size }

@Composable
private fun Sottotitolo(testo: String) {
    Text(testo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun Vuoto(testo: String) {
    Text(testo, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
}
