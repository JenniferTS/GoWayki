package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.components.AtmosphereBackground
import pe.edu.tecsup.gowayki.ui.components.GlassPanel
import pe.edu.tecsup.gowayki.ui.components.GowaykiIcons
import pe.edu.tecsup.gowayki.ui.components.InlineMessage
import pe.edu.tecsup.gowayki.ui.components.PrimaryButton
import pe.edu.tecsup.gowayki.ui.components.SoftButton
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSize
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme

/**
 * Aviso destacado de ubicación (Flutter: `location_disclosure_screen.dart`, ruta `/ubicacion`).
 * Es la única puerta al diálogo del sistema: nada pide el permiso sin pasar antes por aquí.
 */
@Composable
fun LocationDisclosureScreen(session: AppSession) {
    val s = session.strings
    val colors = GowaykiTheme.colors
    val busy = session.locationDisclosureBusy
    AtmosphereBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(GowaykiSpace.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(GowaykiSize.logoLg)
                    .clip(CircleShape)
                    .background(colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    GowaykiIcons.Location,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(GowaykiSize.iconLg),
                )
            }
            Spacer(Modifier.height(GowaykiSpace.lg))
            Text(
                s.locationDisclosureTitle,
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(GowaykiSpace.md))
            GlassPanel(Modifier.fillMaxWidth()) {
                Text(s.locationDisclosureBody, style = MaterialTheme.typography.body1, color = colors.onSurface)
                Spacer(Modifier.height(GowaykiSpace.smd))
                Text(s.locationDisclosureBackground, style = MaterialTheme.typography.body2, color = colors.onSurfaceMuted)
            }
            Spacer(Modifier.height(GowaykiSpace.smd))
            Text(
                s.locationDisclosureOptional,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceMuted,
            )
            if (session.locationDisclosureDenied) {
                Spacer(Modifier.height(GowaykiSpace.sm))
                InlineMessage(s.locationDisclosureDenied, isError = true)
                SoftButton(s.openSettings, onClick = { session.openLocationSettings() })
            }
            Spacer(Modifier.height(GowaykiSpace.lg))
            PrimaryButton(
                text = if (busy) s.loading else s.allowLocation,
                onClick = { session.acceptLocationDisclosure() },
                enabled = !busy,
            )
            Spacer(Modifier.height(GowaykiSpace.sm))
            TextButton(onClick = { session.declineLocationDisclosure() }, enabled = !busy) {
                Text(s.continueWithoutLocation, style = MaterialTheme.typography.button, color = colors.primary)
            }
        }
    }
}
