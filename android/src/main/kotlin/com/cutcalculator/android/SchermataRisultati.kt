package com.cutcalculator.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cutcalculator.app.Etichette
import java.util.Locale

/**
 * Il preventivo dell'ultimo calcolo: quante barre comprare, quanto pesano, quanto costano, e la
 * quota di ogni ordine.
 *
 * Barre e sfrido hanno senso **solo** sul totale: il calcolo unisce gli ordini in un piano unico e
 * le barre finiscono condivise, quindi al singolo ordine si può attribuire una quota (in proporzione
 * ai millimetri tagliati), non un numero di barre intere.
 */
@Composable
fun SchermataRisultati(vm: CutCalculatorViewModel, modifier: Modifier = Modifier) {
    val evasione = vm.ultimaEvasione
    if (evasione == null) {
        Column(modifier.fillMaxSize().padding(24.dp)) {
            Text("Nessun calcolo in questa sessione.")
            Text(
                "Vai su Ordini e tocca \"Calcola tutti gli ordini da fare\".",
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    val preventivo = evasione.preventivoTotale()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Totale", style = MaterialTheme.typography.titleMedium)
                    Voce("Barre nuove", preventivo.totaleBarreNuove().toString())
                    Voce("Avanzi riusati", preventivo.totaleAvanziUsati().toString())
                    Voce("Peso da comprare", "%.1f kg".format(Locale.ROOT, preventivo.pesoTotale()))
                    Voce(
                        "Sfrido",
                        Etichette.misuraConSimbolo(preventivo.sfridoTotale(), vm.unita) +
                                " di cui recuperabili " +
                                Etichette.misuraConSimbolo(
                                    preventivo.lunghezzaRecuperabileTotale(), vm.unita
                                )
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

        if (evasione.quote().size > 1) {
            item { Text("Per ordine", style = MaterialTheme.typography.titleMedium) }
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
                        Voce(
                            "Totale",
                            "%.2f EUR".format(Locale.ROOT, quota.costoTotale()),
                            grassetto = true
                        )
                    }
                }
            }
        }

        item { Text("Materiali", style = MaterialTheme.typography.titleMedium) }
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
                                Etichette.misuraConSimbolo(riga.lunghezzaRecuperabile, vm.unita) + ")"
                    )
                    Voce("Scarto", Etichette.misuraConSimbolo(riga.scarto(), vm.unita))
                }
            }
        }
    }
}

@Composable
private fun Voce(etichetta: String, valore: String, grassetto: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(etichetta, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            valore,
            fontWeight = if (grassetto) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
