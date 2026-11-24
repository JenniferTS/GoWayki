package pe.edu.tecsup.gowayki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme

/** Paso de un recorrido: punto de color (caminata o color de la línea) + título y detalle. */
@Composable
fun StepRow(iconTint: Color, title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = GowaykiSpace.xs),
        verticalAlignment = Alignment.Top,
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .padding(top = GowaykiSpace.xs)
                .size(GowaykiSize.stepDot)
                .clip(CircleShape)
                .background(iconTint),
        )
        Spacer(Modifier.width(GowaykiSpace.smd))
        Column {
            Text(title, style = MaterialTheme.typography.subtitle1)
            Text(subtitle, style = MaterialTheme.typography.caption)
        }
    }
}

/** Color `#RRGGBB` de una línea del paquete; [fallback] si no es válido. */
fun parseLineColor(hex: String, fallback: Color): Color {
    val t = hex.trim().removePrefix("#")
    if (t.length != 6) return fallback
    return t.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: fallback
}

@Composable
fun gowaykiFieldColors(): TextFieldColors {
    val colors = GowaykiTheme.colors
    return TextFieldDefaults.outlinedTextFieldColors(
        textColor = colors.onSurface,
        backgroundColor = colors.surfaceElevated,
        cursorColor = colors.primary,
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.outline,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceMuted,
        placeholderColor = colors.onSurfaceMuted,
    )
}
