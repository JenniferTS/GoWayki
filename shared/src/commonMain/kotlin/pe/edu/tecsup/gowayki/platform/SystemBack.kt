package pe.edu.tecsup.gowayki.platform

import androidx.compose.runtime.Composable

/** Atrás del sistema (gesto o botón) mientras la pantalla está en composición; en Android admite back predictivo. */
@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)
