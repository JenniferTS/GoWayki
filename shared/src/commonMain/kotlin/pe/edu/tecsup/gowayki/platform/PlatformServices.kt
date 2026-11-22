package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient
import pe.edu.tecsup.gowayki.domain.preferences.OnboardingStatus
import pe.edu.tecsup.gowayki.domain.preferences.TutorialProgress

data class PlatformLocation(val latitude: Double, val longitude: Double)

interface UserStateSync {
    fun ensureUser(uid: String)
    fun pushTutorial(uid: String, progress: TutorialProgress)
    fun pullTutorial(uid: String): TutorialProgress?
    fun pushOnboarding(uid: String, status: OnboardingStatus)
    fun pullOnboarding(uid: String): OnboardingStatus?
}

object NoUserStateSync : UserStateSync {
    override fun ensureUser(uid: String) = Unit
    override fun pushTutorial(uid: String, progress: TutorialProgress) = Unit
    override fun pullTutorial(uid: String): TutorialProgress? = null
    override fun pushOnboarding(uid: String, status: OnboardingStatus) = Unit
    override fun pullOnboarding(uid: String): OnboardingStatus? = null
}

expect fun platformRemoteReports(): RemoteReportsClient?
expect fun platformUserStateSync(): UserStateSync
expect fun platformLastLocation(): PlatformLocation?
expect fun platformRequestLocationPermission(onResult: (Boolean) -> Unit)
