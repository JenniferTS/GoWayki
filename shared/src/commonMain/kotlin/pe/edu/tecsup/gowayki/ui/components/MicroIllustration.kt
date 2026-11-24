package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/** Ilustraciones de combis de Arequipa (mismo set que `BrandAssets.micros` en Flutter). */
enum class Micro {
    AlbotranCb,
    AlbotranF2,
    AlbotranR,
    AlbotranTerminal,
    AlbotranSinLetra,
    MegabusM,
    ConoNorteA,
    ConoNorteC,
    ConoNorteCima,
    ConoNorteCimaS,
    TranscaymaEmbajada,
    UnionHuarangal,
    AqpMasivo,
    CharacatoQuequena,
    CharacatoSanAntonio,
}

/**
 * Decodifica la ilustración reducida a ~[targetWidthPx] (los originales miden 2084 px)
 * fuera del hilo de UI y la cachea. Devuelve null mientras carga o si la plataforma no la tiene.
 */
@Composable
expect fun rememberMicroBitmap(micro: Micro, targetWidthPx: Int): ImageBitmap?

@Composable
fun MicroIllustration(
    micro: Micro,
    width: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val px = with(LocalDensity.current) { width.roundToPx() }
    val bitmap = rememberMicroBitmap(micro, px)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(modifier)
    }
}
