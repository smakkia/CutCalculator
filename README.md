# CutCalculator

Ottimizza i tagli dei profili in alluminio per la costruzione di porte e finestre, e stima in anticipo
il materiale necessario.

Si parte dalle misure finite di un serramento (larghezza, altezza) e dalla sua tipologia; l'app applica
le **formule di taglio** del catalogo, impacchetta i pezzi nelle barre sprecando il meno possibile e
produce distinta, piano di taglio e preventivo.

## Cosa fa

1. **Distinta dei pezzi** — dalla tipologia scelta e dalle misure escono tutte le aste da tagliare, con
   quota, tipo di taglio (45°/90°) e quantità, più le lastre di vetro.
2. **Piano di taglio** — i pezzi vengono impacchettati nelle barre tenendo conto dello spessore della
   lama (kerf). Prima si riusano gli **avanzi di magazzino**, poi si aprono barre nuove da 6,5 m.
   Tutti gli ordini da calcolare vengono evasi **insieme**, in un piano unico: pezzi di commesse diverse
   condividono le stesse barre e lo sfrido cala.
3. **Preventivo** — barre per materiale, peso, sfrido (distinguendo i ritagli ancora riusabili dallo
   scarto vero), metri quadri di vetro e costi. Con più ordini, ognuno vede anche la propria quota.

Il magazzino degli avanzi è persistito: i ritagli lunghi rientrano da soli dopo ogni calcolo e sono
disponibili per i lavori successivi.

Catalogo incluso: i quattro sistemi **Twin RX 700**, **CX 700**, **SX 110** e **SX 120**, con le
relative varianti di telaio e anta.

## Download

Gli eseguibili pronti all'uso sono nella pagina delle release:

**→ [Ultima release](https://github.com/smakkia/CutCalculator/releases/latest)** ·
[tutte le release](https://github.com/smakkia/CutCalculator/releases)

- **Windows** — `CutCalculator-<versione>-windows-x64.zip`. Contiene l'eseguibile con dentro la sua
  JRE: **non serve avere Java installato**. Estrai lo zip e avvia `CutCalculator.exe`.
- **Android** — `CutCalculator-<versione>-android.apk`. Richiede Android 8.0 o superiore. Installa
  l'APK dal telefono (va autorizzata l'installazione da origini sconosciute).

## Compilare da sorgente

Serve solo un **JDK 25** con `JAVA_HOME` impostato: Gradle arriva col wrapper in repo.

```bash
./gradlew runGui                  # client grafico (JavaFX)
./gradlew run                     # client testuale
./gradlew test                    # esegue i test
./gradlew :desktop:zipWindows     # pacchetto Windows in desktop/build/distribuzione/
./gradlew :android:assembleRelease # APK in android/build/outputs/apk/release/
```

Il modulo Android entra nella build solo se trova l'SDK (`ANDROID_HOME` o `local.properties`):
senza, il resto del progetto si compila lo stesso.

Il progetto è diviso in tre moduli: `core` (il motore di calcolo, Kotlin, senza interfaccia),
`desktop` (i due client CLI e JavaFX) e `android` (l'app Compose). Il core è lo stesso per tutti i
front-end.
