package pe.edu.tecsup.gowayki.domain.onboarding

/** Slide de onboarding: honestidad sobre GPS de buses. */
data class OnboardingSlide(
    val titleKey: String,
    val bodyKey: String,
)

val NO_LIVE_GPS_SLIDE = OnboardingSlide(
    titleKey = "noLiveGpsTitle",
    bodyKey = "noLiveGpsBody",
)
