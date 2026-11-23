package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors

@Composable
actual fun BrandLogo(
    modifier: Modifier,
    size: Dp,
) {
    Canvas(modifier.size(size)) {
        val r = size.toPx() / 2f
        drawCircle(Color.White, radius = r * 0.98f, center = Offset(r, r))
        drawCircle(GowaykiColors.BrandOrange, radius = r * 0.42f, center = Offset(r * 1.05f, r * 0.72f))
        drawCircle(GowaykiColors.BrandInk, radius = r * 0.18f, center = Offset(r * 0.55f, r * 1.15f))
    }
}
