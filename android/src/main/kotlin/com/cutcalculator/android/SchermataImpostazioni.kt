package com.cutcalculator.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cutcalculator.app.Unita
import com.cutcalculator.ottimizzatore.Strategia

/**
 * Le impostazioni dell'app: **unità di misura** e **algoritmo di ottimizzazione**, le stesse due che
 * il desktop tiene nel menu *Impostazioni*. Sono persistite in `impostazioni.properties` dal
 * `Controller`, quindi si ritrovano al riavvio.
 *
 * Sono scelte a effetto diverso, ed è per questo che sono presentate diversamente:
 * l'unità cambia **subito** quel che si vede (il modello resta in millimetri: cambia solo la lente),
 * mentre l'algoritmo vale dal **prossimo** calcolo — un piano già fatto resta quello, e riscriverne i
 * numeri farebbe credere che sia stato rifatto.
 */
@Composable
fun SchermataImpostazioni(vm: CutCalculatorViewModel, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Sezione(
                titolo = "Unita' di misura",
                sottotitolo = "Con cui l'app mostra e legge le misure. I dati restano in millimetri."
            ) {
                Column(Modifier.selectableGroup()) {
                    Unita.entries.forEach { candidata ->
                        Scelta(
                            etichetta = candidata.descrizione(),
                            spiegazione = null,
                            scelta = candidata == vm.unita,
                            onClick = { vm.impostaUnita(candidata) }
                        )
                    }
                }
            }
        }

        item {
            Sezione(
                titolo = "Algoritmo di ottimizzazione",
                sottotitolo = "Come i pezzi vengono impacchettati nelle barre. " +
                        "Vale dal prossimo calcolo: i piani gia' fatti non cambiano."
            ) {
                Column(Modifier.selectableGroup()) {
                    Strategia.entries.forEachIndexed { indice, candidata ->
                        if (indice > 0) {
                            HorizontalDivider()
                        }
                        Scelta(
                            etichetta = candidata.nome(),
                            spiegazione = candidata.spiegazione(),
                            scelta = candidata == vm.strategia,
                            onClick = { vm.impostaStrategia(candidata) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Sezione(
    titolo: String,
    sottotitolo: String,
    contenuto: @Composable () -> Unit
) {
    Column(Modifier.padding(top = 12.dp)) {
        Text(titolo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            sottotitolo,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(Modifier.fillMaxWidth()) { contenuto() }
    }
}

/**
 * Una voce a scelta singola. La riga intera è cliccabile e non solo il pallino: su un telefono il
 * bersaglio di un RadioButton da solo è piccolo.
 */
@Composable
private fun Scelta(
    etichetta: String,
    spiegazione: String?,
    scelta: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = scelta, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // onClick nullo: il click lo gestisce la riga, altrimenti il lettore di schermo
        // annuncerebbe due bersagli per la stessa scelta.
        RadioButton(selected = scelta, onClick = null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(etichetta, fontWeight = if (scelta) FontWeight.Bold else FontWeight.Normal)
            if (spiegazione != null) {
                Text(spiegazione, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
