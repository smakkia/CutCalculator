package com.cutcalculator.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cutcalculator.app.Etichette
import com.cutcalculator.dominio.Ordine

/**
 * Gli ordini: l'elenco delle commesse e, aprendone una, i suoi serramenti.
 *
 * Un ordine "da calcolare" entrerà nel prossimo calcolo globale; uno "calcolato" ha già scalato il
 * suo materiale dal magazzino — la distinzione è la stessa del desktop, e vale la pena vederla a
 * colpo d'occhio anche sul telefono.
 */
@Composable
fun SchermataOrdini(vm: CutCalculatorViewModel, modifier: Modifier = Modifier) {
    var aperto by remember { mutableStateOf<Ordine?>(null) }

    // L'ordine aperto potrebbe essere stato rimosso: si torna all'elenco invece di mostrare un
    // fantasma.
    val ordineCorrente = aperto?.takeIf { candidato -> vm.ordini.any { it === candidato } }
    if (ordineCorrente == null) {
        ElencoOrdini(vm, modifier, apri = { aperto = it })
    } else {
        DettaglioOrdine(vm, ordineCorrente, modifier, chiudi = { aperto = null })
    }
}

@Composable
private fun ElencoOrdini(
    vm: CutCalculatorViewModel,
    modifier: Modifier,
    apri: (Ordine) -> Unit
) {
    var chiediNome by remember { mutableStateOf(false) }
    var chiediRipristino by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Button(
                    onClick = { vm.calcolaTutto() },
                    enabled = vm.ordiniDaCalcolare.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text("Calcola tutti gli ordini da fare (${vm.ordiniDaCalcolare.size})")
                }
                Text(
                    "Gli ordini vengono tagliati insieme: i pezzi condividono le barre e lo sfrido cala.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                // Il ripristino sta qui, accanto al calcolo, e non dentro il singolo ordine: annulla
                // **l'ultimo calcolo** per intero, non un ordine — le barre erano condivise.
                // La condizione e' "esiste un ordine da ripristinare", non "il file c'e'": un punto
                // di ripristino che nomina solo ordini spariti non e' annullabile.
                if (vm.ordineDaRipristinare != null) {
                    OutlinedButton(
                        onClick = { chiediRipristino = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null)
                        Text("  Annulla l'ultimo calcolo (${vm.ripristinabili.size})")
                    }
                }
            }

            if (vm.ordini.isEmpty()) {
                item {
                    Text(
                        "Nessun ordine. Toccare + per crearne uno.",
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            items(vm.ordini, key = { it.nome() }) { ordine ->
                Card(
                    onClick = { apri(ordine) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(ordine.nome(), fontWeight = FontWeight.Bold)
                        Text(
                            "${ordine.serramenti().size} righe, ${ordine.totaleSerramenti()} serramenti" +
                                    if (ordine.calcolato()) " — calcolato" else " — da calcolare",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { chiediNome = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuovo ordine")
        }
    }

    if (chiediNome) {
        DialogoNomeOrdine(
            titolo = "Nuovo ordine",
            azione = "Crea",
            proposto = vm.nomeOrdineProposto(),
            nomeLibero = { nome -> vm.nomeLibero(nome) },
            annulla = { chiediNome = false },
            conferma = { nome ->
                vm.nuovoOrdine(nome)
                chiediNome = false
            }
        )
    }

    if (chiediRipristino) {
        AlertDialog(
            onDismissRequest = { chiediRipristino = false },
            title = { Text("Annullare l'ultimo calcolo?") },
            text = {
                Column {
                    Text(
                        "Il magazzino torna esattamente com'era prima e questi ordini tornano fra " +
                                "quelli da fare:"
                    )
                    vm.ripristinabili.forEach { nome ->
                        Text("- $nome", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "I loro documenti calcolati vengono persi. Si annulla tutto il gruppo " +
                                "insieme, perche' quel calcolo aveva unito gli ordini in un piano " +
                                "solo, con le barre condivise.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.ripristinaUltimoCalcolo()
                    chiediRipristino = false
                }) { Text("Annulla il calcolo") }
            },
            dismissButton = {
                TextButton(onClick = { chiediRipristino = false }) { Text("Lascia stare") }
            }
        )
    }
}

@Composable
private fun DettaglioOrdine(
    vm: CutCalculatorViewModel,
    ordine: Ordine,
    modifier: Modifier,
    chiudi: () -> Unit
) {
    var aggiungi by remember { mutableStateOf(false) }
    var rinomina by remember { mutableStateOf(false) }
    var vediCalcolo by remember { mutableStateOf(false) }
    val serramenti = ordine.serramenti()

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = chiudi) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                    Text(
                        ordine.nome(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { rinomina = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rinomina ordine")
                    }
                    IconButton(onClick = {
                        vm.rimuoviOrdine(ordine)
                        chiudi()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina ordine")
                    }
                }
                if (ordine.calcolato()) {
                    Text(
                        "Ordine gia' calcolato: modificarlo lo rimette fra quelli da fare, e il " +
                                "prossimo calcolo gli riassegnera' tutto il materiale da capo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // I documenti si rileggono da disco, quindi ci sono anche di un ordine calcolato in
                // una sessione precedente — e restano consultabili dopo averlo modificato.
                OutlinedButton(
                    onClick = { vediCalcolo = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Text("  Vedi il calcolo dell'ordine")
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }

            if (serramenti.isEmpty()) {
                item { Text("Nessun serramento. Toccare + per aggiungerne uno.") }
            }

            items(serramenti.size) { indice ->
                val serramento = serramenti[indice]
                val d = serramento.dimensione
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(serramento.tipologia.nome(), fontWeight = FontWeight.Bold)
                            val misure = "${Etichette.misura(d.L, vm.unita)} x " +
                                    "${Etichette.misura(d.H, vm.unita)}" +
                                    if (d.HF > 0) " (HF ${Etichette.misura(d.HF, vm.unita)})" else ""
                            Text(
                                "$misure ${vm.unita.simbolo()} — x${serramento.quantita} — " +
                                        serramento.colore.nome() +
                                        // Vuota se non ce n'è, come sul desktop: gli scorrevoli e
                                        // chi tiene i profili base restano scritti come prima.
                                        Etichette.varianti(serramento.varianti),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { vm.rimuoviSerramento(ordine, indice) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Togli serramento")
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { aggiungi = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi serramento")
        }
    }

    if (aggiungi) {
        DialogoSerramento(
            vm = vm,
            annulla = { aggiungi = false },
            conferma = { tipologia, varianti, colore, l, h, hf, quantita, prezzoKg, prezzoMq ->
                vm.aggiungiSerramento(
                    ordine, tipologia, varianti, colore, l, h, hf, quantita, prezzoKg, prezzoMq
                )
                aggiungi = false
            }
        )
    }

    if (rinomina) {
        DialogoNomeOrdine(
            titolo = "Rinomina ordine",
            azione = "Rinomina",
            proposto = ordine.nome(),
            // "tranne l'ordine stesso": tenere il proprio nome non e' un conflitto.
            nomeLibero = { nome -> vm.nomeLibero(nome, ordine) },
            annulla = { rinomina = false },
            conferma = { nome ->
                vm.rinominaOrdine(ordine, nome)
                rinomina = false
            }
        )
    }

    if (vediCalcolo) {
        DialogoCalcolo(vm, ordine, chiudi = { vediCalcolo = false })
    }
}

/**
 * Il dialogo con cui si dà un nome a un ordine: lo stesso per crearne uno e per rinominarlo, perché
 * la regola sul nome è identica nei due casi — cambia solo chi può già averlo (vedi `nomeLibero`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoNomeOrdine(
    titolo: String,
    azione: String,
    proposto: String,
    nomeLibero: (String) -> Boolean,
    annulla: () -> Unit,
    conferma: (String) -> Unit
) {
    var nome by remember { mutableStateOf(proposto) }
    val valido = nome.isNotBlank() && nomeLibero(nome.trim())

    AlertDialog(
        onDismissRequest = annulla,
        title = { Text(titolo) },
        text = {
            Column {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    isError = !valido
                )
                if (!valido) {
                    // Il nome e' la chiave con cui gli ordini si rileggono da disco: due omonimi si
                    // fonderebbero in uno solo al prossimo avvio.
                    Text(
                        "Nome vuoto o gia' usato da un altro ordine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { conferma(nome.trim()) }, enabled = valido) { Text(azione) }
        },
        dismissButton = { TextButton(onClick = annulla) { Text("Annulla") } }
    )
}
