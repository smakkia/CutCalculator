package com.cutcalculator.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * L'unica Activity dell'app: monta l'albero Compose e basta.
 *
 * La navigazione è volutamente minima — tre schede in fondo, nessuna libreria di navigazione: le
 * schermate sono poche e ognuna sta in piedi da sola.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppCutCalculator()
        }
    }
}

/** Le quattro schede dell'app. */
private enum class Scheda(val etichetta: String, val icona: ImageVector) {
    ORDINI("Ordini", Icons.Default.Assignment),
    MAGAZZINO("Magazzino", Icons.Default.Inventory2),
    RISULTATI("Risultati", Icons.Default.Receipt),
    IMPOSTAZIONI("Impostazioni", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCutCalculator(vm: CutCalculatorViewModel = viewModel()) {
    val contesto = LocalContext.current
    val schema = remember(contesto) { dynamicLightColorScheme(contesto) }
    var scheda by remember { mutableStateOf(Scheda.ORDINI) }
    val snackbar = remember { SnackbarHostState() }

    // I messaggi del dominio (misura impossibile, nome gia' usato, calcolo fatto) arrivano dal
    // ViewModel e si consumano una volta mostrati.
    val messaggio = vm.messaggio
    LaunchedEffect(messaggio) {
        if (messaggio != null) {
            snackbar.showMessage(messaggio)
            vm.messaggioLetto()
        }
    }

    MaterialTheme(colorScheme = schema) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("CutCalculator") }) },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    Scheda.entries.forEach { voce ->
                        NavigationBarItem(
                            selected = scheda == voce,
                            onClick = { scheda = voce },
                            icon = { Icon(voce.icona, contentDescription = voce.etichetta) },
                            label = { Text(voce.etichetta) }
                        )
                    }
                }
            }
        ) { spazio ->
            val modificatore = Modifier.padding(spazio)
            when (scheda) {
                Scheda.ORDINI -> SchermataOrdini(vm, modificatore)
                Scheda.MAGAZZINO -> SchermataMagazzino(vm, modificatore)
                Scheda.RISULTATI -> SchermataRisultati(vm, modificatore)
                Scheda.IMPOSTAZIONI -> SchermataImpostazioni(vm, modificatore)
            }
        }
    }
}

private suspend fun SnackbarHostState.showMessage(testo: String) {
    showSnackbar(testo)
}
