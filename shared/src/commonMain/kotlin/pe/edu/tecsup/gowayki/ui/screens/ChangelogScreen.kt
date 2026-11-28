package pe.edu.tecsup.gowayki.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.edu.tecsup.gowayki.domain.changelog.APP_CHANGELOG
import pe.edu.tecsup.gowayki.domain.preferences.AppLocale
import pe.edu.tecsup.gowayki.i18n.extraStrings
import pe.edu.tecsup.gowayki.ui.AppSession
import pe.edu.tecsup.gowayki.ui.Screen
import pe.edu.tecsup.gowayki.ui.components.GlassPanel
import pe.edu.tecsup.gowayki.ui.components.GowaykiScreen
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme

/** Pantalla "Qué hay de nuevo": lista [APP_CHANGELOG] por versión. */
@Composable
fun ChangelogScreen(session: AppSession) {
    val extra = extraStrings(session.locale == AppLocale.Es)
    val colors = GowaykiTheme.colors
    GowaykiScreen(title = extra.changelogTitle, onBack = { session.go(Screen.Settings) }) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(GowaykiSpace.sm),
        ) {
            items(APP_CHANGELOG, key = { it.version }) { entry ->
                GlassPanel(Modifier.fillMaxWidth()) {
                    Text("v${entry.version}", style = MaterialTheme.typography.h6, color = colors.primary)
                    Spacer(Modifier.height(GowaykiSpace.xs))
                    entry.bullets.forEach { bullet ->
                        Text("• $bullet", style = MaterialTheme.typography.body2)
                    }
                }
            }
            item { Spacer(Modifier.height(GowaykiSpace.lg)) }
        }
    }
}
