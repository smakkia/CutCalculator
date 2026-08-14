package com.cutcalculator.catalogo;

import com.cutcalculator.dominio.Categoria;
import com.cutcalculator.dominio.Profilo;

/**
 * I profili che compaiono in <b>più di un sistema</b>, dichiarati una volta sola.
 * <p>
 * I binari {@code SX11.101} e {@code SX11.130} sono usati sia da {@link CatalogoSX110 SX 110} sia da
 * {@link CatalogoSX120 SX 120}. Finché erano due costanti separate ma con gli stessi valori nessuno
 * se ne accorgeva, perché i {@code record} sono uguali per contenuto; appena una delle due copie
 * prende un peso lineare diverso dall'altra, però, diventano due {@link Profilo} <b>diversi</b> — e
 * il profilo è metà della chiave {@code Materiale}, quindi un avanzo di magazzino smetterebbe di
 * combaciare con i pezzi da tagliare, in silenzio.
 * <p>
 * Da qui in avanti il peso di questi profili si scrive in un punto solo. I due cataloghi danno
 * numeri leggermente diversi per gli stessi codici (SX 120 stampa 2.366 e 3.506): sono revisioni
 * diverse, e qui vale la <b>più recente</b>, cioè quella del catalogo SX 110 (R1.1 2024, contro
 * R01.3 2020 dell'SX 120). Lo scarto è dello 0,2%.
 */
final class ProfiliCondivisi {

    private ProfiliCondivisi() {
    }

    /** Binario a 2 vie: telaio delle 4 ante SX 110 e di diverse configurazioni SX 120. */
    static final Profilo BINARIO_2VIE =
            new Profilo("SX11.101", "Telaio (binario 2 vie)", Categoria.TELAIO, 2.372);

    /** Binario a 3 vie: telaio delle 3 ante, in entrambi i sistemi scorrevoli. */
    static final Profilo BINARIO_3VIE =
            new Profilo("SX11.130", "Telaio (binario 3 vie)", Categoria.TELAIO, 3.514);
}
