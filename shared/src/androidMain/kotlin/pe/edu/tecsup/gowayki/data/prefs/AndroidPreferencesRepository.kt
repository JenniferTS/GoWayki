package pe.edu.tecsup.gowayki.data.prefs

import android.content.Context
import pe.edu.tecsup.gowayki.domain.preferences.OnboardingStatus
import pe.edu.tecsup.gowayki.domain.preferences.PreferencesRepository
import pe.edu.tecsup.gowayki.domain.preferences.RecentTrip
import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace
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

    override fun readPendingScores(): List<String> =
        prefs.getString(KEY_PENDING_SCORES, null)
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toList()
            .orEmpty()

    override fun savePendingScores(lines: List<String>) {
        prefs.edit().putString(KEY_PENDING_SCORES, lines.joinToString("\n")).apply()
    }

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
        private const val KEY_PENDING_SCORES = "pending_scores"
    }
}
