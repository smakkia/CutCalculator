package com.cutcalculator.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cutcalculator.dominio.Ordine
import java.util.Locale

/**
 * I documenti dell'ultimo calcolo di **un** ordine: distinta, quota di preventivo e vetri, riletti da
 * disco (`ArchivioCalcoli`).
 *
 * Sono la fotografia di un calcolo passato, in sola lettura, e sopravvivono alla chiusura dell'app —
 * a differenza della scheda Risultati, che mostra l'ultimo calcolo di **questa** sessione.
 *
 * Le voci sono "piatte" (codici e numeri, non oggetti del dominio) apposta: ricostruirle col catalogo
 * di *oggi* darebbe l'illusione che il documento sia ancora valido, mentre nel frattempo le schede
 * possono essere cambiate.
 *
 * Qui non c'è il piano di taglio, e non è una dimenticanza: le barre sono **condivise** fra gli
 * ordini calcolati insieme, quindi "le barre di quest'ordine" non esistono — esiste la sua quota.
 */
@Composable
fun DialogoCalcolo(vm: CutCalculatorViewModel, ordine: Ordine, chiudi: () -> Unit) {
    val calcolo = remember(ordine, ordine.calcolato()) { vm.calcoloDi(ordine) }
    var distintaAperta by remember { mutableStateOf(true) }
    var preventivoAperto by remember { mutableStateOf(false) }
    var vetriAperti by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = chiudi,
        title = { Text("Calcolo di ${ordine.nome()}") },
        text = {
            if (calcolo.vuoto()) {
                Text(
                    "Di quest'ordine non c'e' nessun calcolo salvato: va calcolato almeno una volta " +
                            "dalla scheda Ordini."
                )
            } else LazyColumn(Modifier.heightIn(max = 460.dp)) {
                item {
                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Voce("Pezzi da tagliare", calcolo.totalePezzi().toString())
                            Voce("Peso alluminio", "%.1f kg".format(Locale.ROOT, calcolo.pesoProfili()))
                            if (calcolo.totaleLastre() > 0) {
                                Voce(
                                    "Vetro",
                                    "%d lastre, %.2f mq".format(
                                        Locale.ROOT, calcolo.totaleLastre(), calcolo.areaVetroMq()
                                    )
                                )
                            }
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            Voce(
                                "Costo",
                                "%.2f EUR".format(Locale.ROOT, calcolo.costoTotale()),
                                grassetto = true
                            )
                        }
                    }
                    // Modificare un ordine calcolato lo rimette fra quelli da fare, ma i documenti
                    // restano quelli di prima: dirlo evita di leggere numeri superati come attuali.
                    if (!ordine.calcolato()) {
                        Text(
                            "L'ordine e' stato modificato dopo il calcolo: questi documenti sono di " +
                                    "prima e non valgono piu'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                sezione(
                    titolo = "Distinta",
                    riepilogo = "${calcolo.totalePezzi()} pezzi",
                    espansa = distintaAperta,
                    cambia = { distintaAperta = !distintaAperta }
                ) {
                    items(calcolo.distinta.size) { indice ->
                        val voce = calcolo.distinta[indice]
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "${voce.profilo} [${voce.colore}]",
                                    fontWeight = FontWeight.Bold
                                )
                                Voce(
                                    "%.1f mm  %s".format(Locale.ROOT, voce.lunghezza, voce.taglio),
                                    "x${voce.quantita}"
                                )
                            }
                        }
                    }
                }

                sezione(
                    titolo = "Preventivo",
                    riepilogo = "%.2f EUR di alluminio".format(Locale.ROOT, calcolo.costoProfili()),
                    espansa = preventivoAperto,
                    cambia = { preventivoAperto = !preventivoAperto }
                ) {
                    items(calcolo.preventivo.size) { indice ->
                        val voce = calcolo.preventivo[indice]
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "${voce.profilo} [${voce.colore}]",
                                    fontWeight = FontWeight.Bold
                                )
                                Voce("Tagliato", "%.1f mm".format(Locale.ROOT, voce.lunghezza))
                                Voce("Peso", "%.2f kg".format(Locale.ROOT, voce.peso))
                                Voce("Costo", "%.2f EUR".format(Locale.ROOT, voce.costo))
                            }
                        }
                    }
                }

                sezione(
                    titolo = "Vetri",
                    riepilogo = if (calcolo.totaleLastre() == 0) "nessuna lastra"
                    else "${calcolo.totaleLastre()} lastre",
                    espansa = vetriAperti,
                    cambia = { vetriAperti = !vetriAperti }
                ) {
                    items(calcolo.vetri.size) { indice ->
                        val voce = calcolo.vetri[indice]
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "%.0f x %.0f mm".format(Locale.ROOT, voce.altezza, voce.larghezza),
                                    fontWeight = FontWeight.Bold
                                )
                                Voce("Lastre", voce.quantita.toString())
                                Voce("Superficie", "%.2f mq".format(Locale.ROOT, voce.areaMq))
                                if (voce.costo > 0) {
                                    Voce("Costo", "%.2f EUR".format(Locale.ROOT, voce.costo))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = chiudi) { Text("Chiudi") } }
    )
}
