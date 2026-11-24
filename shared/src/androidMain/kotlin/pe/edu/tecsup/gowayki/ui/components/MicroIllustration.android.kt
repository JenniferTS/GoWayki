package pe.edu.tecsup.gowayki.ui.components

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.myapplication.common.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val microCache = object : LruCache<String, ImageBitmap>(32 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.asAndroidBitmap().byteCount
}

private fun Micro.resId(): Int = when (this) {
    Micro.AlbotranCb -> R.drawable.albotran_cb
    Micro.AlbotranF2 -> R.drawable.albotran_f2
    Micro.AlbotranR -> R.drawable.albotran_r
    Micro.AlbotranTerminal -> R.drawable.albotran_s_terminal_terrestre
    Micro.AlbotranSinLetra -> R.drawable.albotran_sin_letra
    Micro.MegabusM -> R.drawable.c10_megabus_m
    Micro.ConoNorteA -> R.drawable.c2_cono_norte_a
    Micro.ConoNorteC -> R.drawable.c2_cono_norte_c
    Micro.ConoNorteCima -> R.drawable.c2_cono_norte_cima
    Micro.ConoNorteCimaS -> R.drawable.c2_cono_norte_cima_s
    Micro.TranscaymaEmbajada -> R.drawable.c3_transcayma_embajada
    Micro.UnionHuarangal -> R.drawable.c4_union_aqp_7a_huarangal
    Micro.AqpMasivo -> R.drawable.c7_aqpmasivo_05
    Micro.CharacatoQuequena -> R.drawable.c8_bus_characato_t_quequena
    Micro.CharacatoSanAntonio -> R.drawable.c8_bus_characato_t_san_antonio
}

private fun sampleSizeFor(sourceWidth: Int, targetWidth: Int): Int {
    var sample = 1
    while (sourceWidth / (sample * 2) >= targetWidth.coerceAtLeast(1)) sample *= 2
    return sample
}

private fun decodeMicro(resources: Resources, micro: Micro, targetWidthPx: Int): ImageBitmap? = runCatching {
    val id = micro.resId()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(resources, id, bounds)
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, targetWidthPx)
        inScaled = false
    }
    BitmapFactory.decodeResource(resources, id, options)?.asImageBitmap()
}.getOrNull()

@Composable
actual fun rememberMicroBitmap(micro: Micro, targetWidthPx: Int): ImageBitmap? {
    val resources = LocalContext.current.resources
    val bucket = Integer.highestOneBit(targetWidthPx.coerceAtLeast(1))
    val key = "${micro.name}@$bucket"
    val bitmap by produceState(initialValue = microCache.get(key), key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                microCache.get(key) ?: decodeMicro(resources, micro, targetWidthPx)?.also { microCache.put(key, it) }
            }
        }
    }
    return bitmap
}
