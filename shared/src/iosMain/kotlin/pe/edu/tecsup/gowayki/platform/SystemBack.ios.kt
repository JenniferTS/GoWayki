package pe.edu.tecsup.gowayki.platform

import androidx.compose.runtime.Composable

/** iOS no tiene Atrás del sistema; se vuelve con los botones de cada pantalla. */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
