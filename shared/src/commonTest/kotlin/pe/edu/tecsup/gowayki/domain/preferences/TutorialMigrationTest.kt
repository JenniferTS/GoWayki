package pe.edu.tecsup.gowayki.domain.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class TutorialMigrationTest {
    @Test
    fun missingTutorialOnAnOldAccountIsCompletedAndOnlyNewGoogleRegistrationIsPending() {
        assertNull(TutorialProgress.fromStorage(null))
        val oldAccount = TutorialMigrationPolicy.forExistingAccount(null)
        assertEquals(TutorialStatus.Completed, oldAccount.status)
        assertEquals(TUTORIAL_VERSION, oldAccount.version)
        assertEquals(0, oldAccount.step)
        assertNotEquals(TutorialStatus.Pending, oldAccount.status)

        val registration = TutorialMigrationPolicy.forNewGoogleRegistration()
        assertEquals(TutorialStatus.Pending, registration.status)
        assertEquals(TUTORIAL_VERSION, registration.version)
        assertEquals(0, registration.step)
        assertEquals(registration, TutorialMigrationPolicy.resolve(newGoogleRegistration = true, stored = null))
        assertEquals(oldAccount, TutorialMigrationPolicy.resolve(newGoogleRegistration = false, stored = null))

        val started = TutorialProgress(TutorialStatus.Started, version = TUTORIAL_VERSION, step = 3)
        assertEquals(started, TutorialMigrationPolicy.forExistingAccount(started))
        assertEquals(started, TutorialMigrationPolicy.resolve(newGoogleRegistration = false, stored = started))

        val preferences = InMemoryPreferencesRepository()
        assertEquals(TutorialStatus.Completed, preferences.tutorialOnOpen(newGoogleRegistration = false).status)
        preferences.saveTutorial(TutorialMigrationPolicy.forNewGoogleRegistration())
        assertEquals(TutorialStatus.Pending, preferences.readTutorial()?.status)
        preferences.saveTutorial(TutorialProgress(TutorialStatus.Skipped, step = 1))
        assertEquals(TutorialStatus.Skipped, preferences.tutorialOnOpen(newGoogleRegistration = false).status)
        assertEquals(1, preferences.tutorialOnOpen(newGoogleRegistration = false).step)
    }

    @Test
    fun localeUsesTheSavedChoiceThenTheDeviceAndFallsBackToSpanish() {
        assertEquals(AppLocale.En, AppLocale.resolve(savedCode = null, deviceLanguage = "en-US"))
        assertEquals(AppLocale.Es, AppLocale.resolve(savedCode = null, deviceLanguage = "pt-BR"))
        assertEquals(AppLocale.Es, AppLocale.resolve(savedCode = null, deviceLanguage = null))
        assertEquals(AppLocale.Es, AppLocale.resolve(savedCode = "es-PE", deviceLanguage = "en"))
        assertEquals(AppLocale.En, AppLocale.resolve(savedCode = "EN_US", deviceLanguage = "es"))

        val preferences = InMemoryPreferencesRepository()
        preferences.saveLocaleCode("en")
        assertEquals(AppLocale.En, preferences.locale(deviceLanguage = "es"))
        preferences.saveLocaleCode(null)
        assertEquals(AppLocale.Es, preferences.locale(deviceLanguage = "es"))
        preferences.saveOnboarding(OnboardingStatus.Skipped)
        assertEquals(OnboardingStatus.Skipped, preferences.readOnboarding())
        assertEquals(OnboardingStatus.Completed, OnboardingStatus.fromStorage("completed"))
    }
}
