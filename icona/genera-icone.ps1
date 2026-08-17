# Genera tutte le icone dell'app dal disegno originale `icona-sorgente.png`.
#
# Perche' uno script e non i file a mano: le taglie sono quindici (sei per la finestra JavaFX, sette
# dentro il .ico, cinque densita' Android per due livelli) e ognuna va **ridisegnata**, non
# rimpicciolita a occhio. Se un domani il disegno cambia, si sostituisce il PNG sorgente e si rilancia
# questo file: nessuna taglia resta indietro.
#
#   powershell -ExecutionPolicy Bypass -File icona\genera-icone.ps1
#
# Cosa scrive:
#   desktop/src/main/resources/com/cutcalculator/gui/icona-*.png   icona della finestra (stage.getIcons)
#   icona/CutCalculator.ico                                        icona dell'eseguibile (jpackage --icon)
#   android/src/main/res/mipmap-*/ic_launcher_foreground.png       livello in primo piano dell'adaptive icon
#   android/src/main/res/mipmap-*/ic_launcher.png                  icona piatta di scorta (launcher che non
#                                                                  conoscono le adaptive icon)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$sorgente = Join-Path $PSScriptRoot "icona-sorgente.png"
$radice = Split-Path $PSScriptRoot -Parent

# --- Geometria misurata sull'originale (1254x1254) --------------------------------------------
# La "piastra" e' il quadrato arrotondato bianco su cui poggia il disegno. Nell'originale non ha un
# bordo suo: quel che si vede e' l'ombra portata, che qui **non** si riporta — a 32 pixel diventerebbe
# una macchia grigia. Il suo contorno e' una **superellisse** (squircle in stile iOS), non un
# rettangolo arrotondato: con l'esponente 2 si otterrebbe un'ellisse, con 4 angoli troppo stretti,
# con 5 il profilo dell'originale (verificato sui pixel del contorno).
$PIASTRA_X = 112
$PIASTRA_Y = 94
$PIASTRA_LATO = 1035
$SQUIRCLE = 5.0

# Il disegno vero e proprio (il nodo di serramento in sezione). Il riquadro dei pixel non bianchi e'
# 335,230 - 634x820: qui c'e' **50 px di margine per lato** in piu', e non e' un dettaglio. Il disegno
# ha una sua ombra morbida (valori 235-250) che si spegne fuori da quel riquadro: tagliando raso si
# tagliava anche l'ombra a meta', e restava una cornice grigia intorno al disegno. Col margine il
# taglio cade dove `Sbianca` ha gia' portato tutto a bianco pieno, quindi non c'e' nessuno scalino.
# La posizione e la scala del disegno **non** cambiano: sono calcolate da questo riquadro rispetto
# alla piastra, quindi allargarlo non lo sposta ne' lo rimpicciolisce.
$DISEGNO_X = 285
$DISEGNO_Y = 180
$DISEGNO_W = 734
$DISEGNO_H = 920

# La zona di un'adaptive icon che si vede davvero: 72 dp dei 108 del disegno. I 18 dp per lato che
# restano servono alla maschera del launcher e all'effetto di profondita', e vengono tagliati.
$ADATTIVA_CANVAS = 108.0
$ADATTIVA_VISIBILE = 72.0

# --- Utilita' ---------------------------------------------------------------------------------

# Il contorno della piastra come poligono di 720 punti: |x/r|^n + |y/r|^n = 1 in forma parametrica.
function Percorso-Squircle([double]$centro, [double]$raggio, [double]$n) {
    $percorso = New-Object System.Drawing.Drawing2D.GraphicsPath
    $punti = New-Object System.Collections.Generic.List[System.Drawing.PointF]
    $esponente = 2.0 / $n
    for ($i = 0; $i -lt 720; $i++) {
        $t = 2.0 * [Math]::PI * $i / 720.0
        $cos = [Math]::Cos($t)
        $sin = [Math]::Sin($t)
        $x = $centro + $raggio * [Math]::Sign($cos) * [Math]::Pow([Math]::Abs($cos), $esponente)
        $y = $centro + $raggio * [Math]::Sign($sin) * [Math]::Pow([Math]::Abs($sin), $esponente)
        $punti.Add((New-Object System.Drawing.PointF([float]$x, [float]$y)))
    }
    $percorso.AddPolygon($punti.ToArray())
    return $percorso
}

# Rimpicciolisce **dimezzando** finche' e' possibile, e solo all'ultimo passo interpola.
# Un unico salto da 820 a 13 pixel, per quanto "HighQualityBicubic", campiona una manciata di pixel
# e butta tutto il resto: le linee sottili sparivano o si spezzavano.
function Ridimensiona([System.Drawing.Bitmap]$immagine, [int]$larghezza, [int]$altezza) {
    $corrente = $immagine
    $daLiberare = $false
    while ($corrente.Width -ge 2 * $larghezza -and $corrente.Height -ge 2 * $altezza) {
        $mezzaL = [Math]::Max($larghezza, [int]($corrente.Width / 2))
        $mezzaA = [Math]::Max($altezza, [int]($corrente.Height / 2))
        if ($mezzaL -eq $corrente.Width -and $mezzaA -eq $corrente.Height) { break }
        $meta = Disegna-Scalato $corrente $mezzaL $mezzaA ([System.Drawing.Drawing2D.InterpolationMode]::HighQualityBilinear)
        if ($daLiberare) { $corrente.Dispose() }
        $corrente = $meta
        $daLiberare = $true
    }
    $finale = Disegna-Scalato $corrente $larghezza $altezza ([System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic)
    if ($daLiberare) { $corrente.Dispose() }
    return $finale
}

function Disegna-Scalato([System.Drawing.Bitmap]$immagine, [int]$larghezza, [int]$altezza, $interpolazione) {
    $esito = New-Object System.Drawing.Bitmap $larghezza, $altezza, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($esito)
    $g.InterpolationMode = $interpolazione
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $destinazione = New-Object System.Drawing.Rectangle 0, 0, $larghezza, $altezza
    # TileFlipXY non e' un vezzo: interpolando, i pixel del **bordo** attingono anche da fuori
    # l'immagine, e la' GDI+ mette nero trasparente. Il risultato era un ritaglio col perimetro
    # leggermente scuro (253 invece di 255), che sulla piastra bianca si vedeva come una cornice.
    # Cosi' il bordo attinge dall'immagine specchiata, cioe' da se stesso.
    $attributi = New-Object System.Drawing.Imaging.ImageAttributes
    $attributi.SetWrapMode([System.Drawing.Drawing2D.WrapMode]::TileFlipXY)
    $g.DrawImage($immagine, $destinazione, 0, 0, $immagine.Width, $immagine.Height, [System.Drawing.GraphicsUnit]::Pixel, $attributi)
    $attributi.Dispose()
    $g.Dispose()
    return $esito
}

# Porta a bianco pieno il fondo del ritaglio. Serve perche' la piastra dell'originale **non e' bianco
# puro**: ha una velatura (255 in alto, 246 verso il basso) piu' un filo di contorno e un'ombra
# sfalsata. Incollando il ritaglio su una piastra bianca si vedeva il suo rettangolo, come una
# cornice grigia intorno al disegno — non la velatura in se', che e' impercettibile, ma il **salto**
# sul bordo del ritaglio.
#
# Il criterio non e' "quasi bianco" e basta: si richiede anche che il pixel sia **neutro** (i tre
# canali entro 4 punti). Il vetro e' azzurrato — i suoi canali divergono di decine di punti — quindi
# resta intatto anche dove e' chiarissimo; a sparire sono solo il fondo e i bianchi dell'alluminio,
# che diventano lo stesso bianco su cui vengono incollati.
function Sbianca([System.Drawing.Bitmap]$bitmap) {
    $riquadro = New-Object System.Drawing.Rectangle 0, 0, $bitmap.Width, $bitmap.Height
    $dati = $bitmap.LockBits($riquadro, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $totale = $dati.Stride * $bitmap.Height
    $byte = New-Object byte[] $totale
    [System.Runtime.InteropServices.Marshal]::Copy($dati.Scan0, $byte, 0, $totale)
    for ($riga = 0; $riga -lt $bitmap.Height; $riga++) {
        $base = $riga * $dati.Stride
        for ($colonna = 0; $colonna -lt $bitmap.Width; $colonna++) {
            $i = $base + $colonna * 4          # in memoria l'ordine e' B, G, R, A
            $b = $byte[$i]
            $v = $byte[$i + 1]
            $r = $byte[$i + 2]
            $minimo = [Math]::Min($b, [Math]::Min($v, $r))
            $massimo = [Math]::Max($b, [Math]::Max($v, $r))
            if ($minimo -ge 244 -and ($massimo - $minimo) -le 4) {
                $byte[$i] = 255
                $byte[$i + 1] = 255
                $byte[$i + 2] = 255
                $byte[$i + 3] = 255
            }
        }
    }
    [System.Runtime.InteropServices.Marshal]::Copy($byte, 0, $dati.Scan0, $totale)
    $bitmap.UnlockBits($dati)
}

function Nuova-Tela([int]$dimensione) {
    $bmp = New-Object System.Drawing.Bitmap $dimensione, $dimensione, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    return @{ Bitmap = $bmp; Grafica = $g }
}

# Incolla il disegno dentro una tela quadrata, alla stessa scala e nella stessa posizione che ha
# sull'originale rispetto alla piastra: `riferimento` e' quanti pixel della tela valgono la piastra.
function Incolla-Disegno($grafica, [double]$riferimento, [double]$centroTela) {
    $altezza = $riferimento * $DISEGNO_H / $PIASTRA_LATO
    $larghezza = $altezza * $DISEGNO_W / $DISEGNO_H
    # Sulla piastra il disegno non e' esattamente centrato (un oggetto in assonometria si equilibra
    # a occhio): lo stesso scarto si riporta qui, invece di centrarlo a forza.
    $scartoX = $riferimento * (($DISEGNO_X + $DISEGNO_W / 2.0) - ($PIASTRA_X + $PIASTRA_LATO / 2.0)) / $PIASTRA_LATO
    $scartoY = $riferimento * (($DISEGNO_Y + $DISEGNO_H / 2.0) - ($PIASTRA_Y + $PIASTRA_LATO / 2.0)) / $PIASTRA_LATO
    $l = [Math]::Max(1, [int][Math]::Round($larghezza))
    $a = [Math]::Max(1, [int][Math]::Round($altezza))
    $scalato = Ridimensiona $script:disegno $l $a
    $x = [int][Math]::Round($centroTela + $scartoX - $l / 2.0)
    $y = [int][Math]::Round($centroTela + $scartoY - $a / 2.0)
    $grafica.DrawImage($scalato, $x, $y, $l, $a)
    $scalato.Dispose()
}

# La piastra intera: squircle bianco a pieno campo col disegno dentro. Serve al desktop (dove
# l'icona **deve** avere il suo fondo: il disegno e' grigio scuro e su una barra delle applicazioni
# scura, che e' il tema predefinito di Windows 11, sparirebbe) e come icona piatta di scorta su Android.
function Piastra([int]$dimensione) {
    $tela = Nuova-Tela $dimensione
    $centro = $dimensione / 2.0
    # Mezzo pixel in meno: il bordo dell'antialias resta dentro la tela invece di essere troncato.
    $percorso = Percorso-Squircle $centro ($centro - 0.5) $SQUIRCLE
    $tela.Grafica.FillPath([System.Drawing.Brushes]::White, $percorso)
    $percorso.Dispose()
    Incolla-Disegno $tela.Grafica $dimensione $centro
    $tela.Grafica.Dispose()
    return $tela.Bitmap
}

# Il livello in primo piano dell'adaptive icon: solo il disegno, su tela trasparente, rimpicciolito
# in modo da restare dentro la zona visibile (72 dp su 108). Il fondo lo mette il livello di sotto,
# che e' un colore piatto: il rettangolo bianco del ritaglio ci si perde dentro.
function Primo-Piano([int]$dimensione) {
    $tela = Nuova-Tela $dimensione
    $visibile = $dimensione * $ADATTIVA_VISIBILE / $ADATTIVA_CANVAS
    Incolla-Disegno $tela.Grafica $visibile ($dimensione / 2.0)
    $tela.Grafica.Dispose()
    return $tela.Bitmap
}

function Salva-Png([System.Drawing.Bitmap]$bitmap, [string]$percorso) {
    $cartella = Split-Path $percorso -Parent
    if (-not (Test-Path $cartella)) { New-Item -ItemType Directory -Force $cartella | Out-Null }
    $bitmap.Save($percorso, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output ("  {0,-72} {1}x{1}" -f (Resolve-Path -Relative $percorso), $bitmap.Width)
}

# Un .ico e' un indice di sei campi piu' le immagini in fila. Dalla Vista in poi ogni voce puo'
# essere un PNG intero (e non un BMP capovolto senza intestazione), che e' l'unico modo sensato di
# arrivare a 256x256: qui si scrive esattamente quello.
function Salva-Ico([array]$bitmaps, [string]$percorso) {
    $blocchi = @()
    foreach ($bmp in $bitmaps) {
        $memoria = New-Object System.IO.MemoryStream
        $bmp.Save($memoria, [System.Drawing.Imaging.ImageFormat]::Png)
        $blocchi += , @{ Dimensione = $bmp.Width; Byte = $memoria.ToArray() }
        $memoria.Dispose()
    }
    $flusso = [System.IO.File]::Create($percorso)
    $scrittore = New-Object System.IO.BinaryWriter $flusso
    $scrittore.Write([UInt16]0)                     # riservato
    $scrittore.Write([UInt16]1)                     # 1 = icona (2 sarebbe un cursore)
    $scrittore.Write([UInt16]$blocchi.Count)
    $offset = 6 + 16 * $blocchi.Count
    foreach ($blocco in $blocchi) {
        # 256 non sta in un byte: per convenzione si scrive 0.
        $lato = $blocco.Dimensione
        if ($lato -ge 256) { $lato = 0 }
        $scrittore.Write([Byte]$lato)               # larghezza
        $scrittore.Write([Byte]$lato)               # altezza
        $scrittore.Write([Byte]0)                   # colori nella tavolozza (0 = colore diretto)
        $scrittore.Write([Byte]0)                   # riservato
        $scrittore.Write([UInt16]1)                 # piani
        $scrittore.Write([UInt16]32)                # bit per pixel
        $scrittore.Write([UInt32]$blocco.Byte.Length)
        $scrittore.Write([UInt32]$offset)
        $offset += $blocco.Byte.Length
    }
    foreach ($blocco in $blocchi) { $scrittore.Write($blocco.Byte) }
    $scrittore.Dispose()
    $flusso.Dispose()
    Write-Output ("  {0,-72} {1} taglie" -f (Resolve-Path -Relative $percorso), $blocchi.Count)
}

# --- Generazione ------------------------------------------------------------------------------

$originale = New-Object System.Drawing.Bitmap $sorgente
$riquadro = New-Object System.Drawing.Rectangle $DISEGNO_X, $DISEGNO_Y, $DISEGNO_W, $DISEGNO_H
$script:disegno = $originale.Clone($riquadro, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$originale.Dispose()
Sbianca $script:disegno

Write-Output "Icona della finestra JavaFX (stage.getIcons):"
$risorseGui = Join-Path $radice "desktop\src\main\resources\com\cutcalculator\gui"
foreach ($lato in 16, 32, 48, 64, 128, 256) {
    $bmp = Piastra $lato
    Salva-Png $bmp (Join-Path $risorseGui "icona-$lato.png")
    $bmp.Dispose()
}

Write-Output "Icona dell'eseguibile Windows (jpackage --icon):"
$perIco = @()
foreach ($lato in 16, 24, 32, 48, 64, 128, 256) { $perIco += , (Piastra $lato) }
Salva-Ico $perIco (Join-Path $PSScriptRoot "CutCalculator.ico")
foreach ($bmp in $perIco) { $bmp.Dispose() }

Write-Output "Icona Android (adaptive icon: primo piano + scorta piatta):"
# mdpi e' l'unita': 1 dp = 1 px. Le altre densita' sono i suoi multipli canonici.
$densita = [ordered]@{ mdpi = 1.0; hdpi = 1.5; xhdpi = 2.0; xxhdpi = 3.0; xxxhdpi = 4.0 }
$res = Join-Path $radice "android\src\main\res"
foreach ($nome in $densita.Keys) {
    $fattore = $densita[$nome]
    $cartella = Join-Path $res "mipmap-$nome"

    $primoPiano = Primo-Piano ([int]($ADATTIVA_CANVAS * $fattore))
    Salva-Png $primoPiano (Join-Path $cartella "ic_launcher_foreground.png")
    $primoPiano.Dispose()

    # L'icona piatta di scorta e' 48 dp, la taglia storica del launcher.
    $piatta = Piastra ([int](48 * $fattore))
    Salva-Png $piatta (Join-Path $cartella "ic_launcher.png")
    $piatta.Dispose()
}

$script:disegno.Dispose()
Write-Output "Fatto."
