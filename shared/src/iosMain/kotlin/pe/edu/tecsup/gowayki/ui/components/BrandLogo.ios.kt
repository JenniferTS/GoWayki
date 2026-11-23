package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors

@Composable
actual fun BrandLogo(
    modifier: Modifier,
    size: Dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.86f)) {
            val r = size.toPx() * 0.43f
            drawCircle(GowaykiColors.BrandOrange, radius = r * 0.42f, center = Offset(r * 1.05f, r * 0.72f))
            drawCircle(GowaykiColors.BrandInk, radius = r * 0.18f, center = Offset(r * 0.55f, r * 1.15f))
        }
    }
}
