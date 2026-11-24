package pe.edu.tecsup.gowayki.ui

import androidx.compose.runtime.staticCompositionLocalOf
import pe.edu.tecsup.gowayki.domain.preferences.AppLocale
import pe.edu.tecsup.gowayki.i18n.StringCatalog
import pe.edu.tecsup.gowayki.i18n.Strings

/** Textos del idioma activo para componentes que no reciben [AppSession] (mapa, cabeceras). */
val LocalStrings = staticCompositionLocalOf<Strings> { StringCatalog.forLocale(AppLocale.Es) }
