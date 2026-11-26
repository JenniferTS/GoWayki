package pe.edu.tecsup.gowayki.domain.privacy

import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository

fun wipeLocalUserData(prefs: PreferencesRepository) {
    prefs.saveFavorites(emptyList())
    prefs.saveRecents(emptyList())
    prefs.saveAlarmMode("off")
    prefs.saveLocaleCode(null)
}
