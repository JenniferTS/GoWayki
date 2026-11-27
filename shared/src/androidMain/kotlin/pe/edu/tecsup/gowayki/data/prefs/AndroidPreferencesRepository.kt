package pe.edu.tecsup.gowayki.data.prefs

import android.content.Context
import pe.edu.tecsup.gowayki.domain.preferences.OnboardingStatus
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository
import pe.edu.tecsup.gowayki.domain.preferences.RecentTrip
import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace
import pe.edu.tecsup.gowayki.domain.preferences.ThemePreference
import pe.edu.tecsup.gowayki.domain.preferences.TutorialProgress

class AndroidPreferencesRepository(context: Context) : PreferencesRepository {
    private val prefs = context.applicationContext.getSharedPreferences("gowayki_prefs", Context.MODE_PRIVATE)

    override fun readCityCode(): String? = prefs.getString(KEY_CITY, null)

    override fun saveCityCode(code: String) {
        prefs.edit().putString(KEY_CITY, code).apply()
    }

    override fun readLocaleCode(): String? = prefs.getString(KEY_LOCALE, null)

    override fun saveLocaleCode(code: String?) {
        prefs.edit().putString(KEY_LOCALE, code).apply()
    }

    override fun readOnboarding(): OnboardingStatus? =
        prefs.getString(KEY_ONBOARDING, null)?.let(OnboardingStatus::fromStorage)

    override fun saveOnboarding(status: OnboardingStatus) {
        prefs.edit().putString(KEY_ONBOARDING, status.storageValue).apply()
    }

    override fun readTutorial(): TutorialProgress? {
        if (!prefs.contains(KEY_TUTORIAL_STATUS)) return null
        return TutorialProgress.fromStorage(
            mapOf(
                "status" to prefs.getString(KEY_TUTORIAL_STATUS, null),
                "version" to prefs.getInt(KEY_TUTORIAL_VERSION, 1),
                "step" to prefs.getInt(KEY_TUTORIAL_STEP, 0),
            ),
        )
    }

    override fun saveTutorial(progress: TutorialProgress) {
        prefs.edit()
            .putString(KEY_TUTORIAL_STATUS, progress.status.storageValue)
            .putInt(KEY_TUTORIAL_VERSION, progress.version)
            .putInt(KEY_TUTORIAL_STEP, progress.step)
            .apply()
    }

    override fun readAlarmMode(): String? = prefs.getString(KEY_ALARM, null)

    override fun saveAlarmMode(mode: String) {
        prefs.edit().putString(KEY_ALARM, mode).apply()
    }

    override fun readFavorites(): List<SavedPlace> =
        prefs.getString(KEY_FAVORITES, null)
            ?.lineSequence()
            ?.mapNotNull(SavedPlace::decode)
            ?.toList()
            .orEmpty()

    override fun saveFavorites(places: List<SavedPlace>) {
        prefs.edit().putString(KEY_FAVORITES, places.joinToString("\n") { it.encode() }).apply()
    }

    override fun readRecents(): List<RecentTrip> =
        prefs.getString(KEY_RECENTS, null)
            ?.lineSequence()
            ?.mapNotNull(RecentTrip::decode)
            ?.toList()
            .orEmpty()

    override fun saveRecents(trips: List<RecentTrip>) {
        prefs.edit().putString(KEY_RECENTS, trips.joinToString("\n") { it.encode() }).apply()
    }

    override fun readPendingScores(uid: String): List<String> =
        prefs.getString(pendingScoresKey(uid), null)
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toList()
            .orEmpty()

    override fun savePendingScores(uid: String, lines: List<String>) {
        val edit = prefs.edit()
        if (lines.isEmpty()) {
            edit.remove(pendingScoresKey(uid))
        } else {
            edit.putString(pendingScoresKey(uid), lines.joinToString("\n"))
        }
        edit.apply()
    }

    override fun hasLegacyPendingScores(): Boolean = prefs.contains(KEY_LEGACY_PENDING_SCORES)

    override fun discardLegacyPendingScores() {
        prefs.edit().remove(KEY_LEGACY_PENDING_SCORES).apply()
    }

    override fun readLocationDisclosureAccepted(): Boolean =
        prefs.getBoolean(KEY_LOCATION_DISCLOSURE, false)

    override fun saveLocationDisclosureAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_DISCLOSURE, accepted).apply()
    }

    override fun readAccountsBeingDeleted(): Set<String> =
        prefs.getStringSet(KEY_ACCOUNTS_DELETING, null)?.toSet().orEmpty()

    /** `commit` y no `apply`: la pausa debe quedar en disco antes de tocar la nube. */
    override fun saveAccountBeingDeleted(uid: String, deleting: Boolean) {
        val next = readAccountsBeingDeleted().toMutableSet()
        if (deleting) next.add(uid) else next.remove(uid)
        check(prefs.edit().putStringSet(KEY_ACCOUNTS_DELETING, next).commit()) {
            "No se pudo guardar el estado de borrado."
        }
    }

    override fun readGameCombi(): String? = prefs.getString(KEY_GAME_COMBI, null)

    override fun saveGameCombi(name: String) {
        prefs.edit().putString(KEY_GAME_COMBI, name).apply()
    }

    override fun readThemePreference(): ThemePreference =
        ThemePreference.fromStorage(prefs.getString(KEY_THEME, null))

    override fun saveThemePreference(theme: ThemePreference) {
        prefs.edit().putString(KEY_THEME, theme.storageValue).apply()
    }

    private fun pendingScoresKey(uid: String): String = "$KEY_PENDING_SCORES_PREFIX$uid"

    companion object {
        private const val KEY_CITY = "city_code"
        private const val KEY_LOCALE = "locale"
        private const val KEY_ONBOARDING = "onboarding"
        private const val KEY_TUTORIAL_STATUS = "tutorial_status"
        private const val KEY_TUTORIAL_VERSION = "tutorial_version"
        private const val KEY_TUTORIAL_STEP = "tutorial_step"
        private const val KEY_ALARM = "alarm_mode"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_RECENTS = "recents"
        /** Cola compartida (sin uid) de versiones anteriores; se descarta al arrancar. */
        private const val KEY_LEGACY_PENDING_SCORES = "pending_scores"
        private const val KEY_PENDING_SCORES_PREFIX = "pending_scores.v2."
        private const val KEY_LOCATION_DISCLOSURE = "location_disclosure_accepted"
        private const val KEY_ACCOUNTS_DELETING = "accounts_deleting"
        private const val KEY_GAME_COMBI = "game_combi"
        private const val KEY_THEME = "theme_preference"
    }
}
