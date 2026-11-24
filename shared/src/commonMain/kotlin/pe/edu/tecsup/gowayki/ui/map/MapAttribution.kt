package pe.edu.tecsup.gowayki.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import pe.edu.tecsup.gowayki.domain.legal.LegalLinks
import pe.edu.tecsup.gowayki.platform.platformOpenUrl
import pe.edu.tecsup.gowayki.ui.LocalStrings
import pe.edu.tecsup.gowayki.ui.theme.GowaykiRadius
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme

/**
 * Atribución visible de los datos (ODbL) y teselas del mapa, siempre en pantalla.
 * Al tocarla ofrece los enlaces a la página de copyright de OSM y a OpenMapTiles.
 */
@Composable
fun MapAttribution(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val colors = GowaykiTheme.colors
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(GowaykiRadius.xs)
    Box(modifier) {
        Box(
            Modifier
                .heightIn(min = GowaykiSize.touchTarget)
                .clickable(role = Role.Button) { open = true }
                .semantics { contentDescription = s.mapAttributionDescription },
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                s.mapAttribution,
                style = MaterialTheme.typography.caption,
                color = colors.onSurface,
                modifier = Modifier
                    .clip(shape)
                    .background(colors.surfaceElevated.copy(alpha = ATTRIBUTION_ALPHA))
                    .padding(horizontal = GowaykiSpace.sm, vertical = GowaykiSpace.xs),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(onClick = {
                open = false
                platformOpenUrl(LegalLinks.OSM_COPYRIGHT)
            }) {
                Text(s.osmAttribution, style = MaterialTheme.typography.body2)
            }
            DropdownMenuItem(onClick = {
                open = false
                platformOpenUrl(LegalLinks.OPENMAPTILES)
            }) {
                Text(s.openMapTilesAttribution, style = MaterialTheme.typography.body2)
            }
        }
    }
}

private const val ATTRIBUTION_ALPHA = 0.86f
