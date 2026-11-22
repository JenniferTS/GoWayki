package pe.edu.tecsup.gowayki.domain.preferences

enum class OnboardingStatus(val storageValue: String) {
    Completed("completed"),
    Skipped("skipped"),
    ;

    companion object {
        fun fromStorage(value: String): OnboardingStatus =
            values().firstOrNull { it.storageValue == value }
                ?: throw IllegalArgumentException("Estado de bienvenida desconocido.")
    }
}

enum class TutorialStatus(val storageValue: String) {
    Pending("pending"),
    Started("started"),
    Completed("completed"),
    Skipped("skipped"),
    ;

    companion object {
        fun fromStorage(value: String): TutorialStatus =
            values().firstOrNull { it.storageValue == value }
                ?: throw IllegalArgumentException("Estado de tutorial desconocido.")
    }
}

const val TUTORIAL_VERSION: Int = 1

data class TutorialProgress(
    val status: TutorialStatus,
    val version: Int = TUTORIAL_VERSION,
    val step: Int = 0,
) {
    init {
        require(version == TUTORIAL_VERSION) { "Versión de tutorial desconocida." }
        require(step >= 0) { "Paso inválido." }
        if (status == TutorialStatus.Pending) {
            require(step == 0) { "El tutorial pendiente empieza en el paso 0." }
        }
    }

    fun toStorageMap(): Map<String, Any> = mapOf(
        "status" to status.storageValue,
        "version" to version,
        "step" to step,
    )

    companion object {
        /** Null cuando el documento de la cuenta no trae el campo. */
        fun fromStorage(data: Map<String, Any?>?): TutorialProgress? {
            if (data == null || !data.containsKey("status")) return null
            val status = TutorialStatus.fromStorage(data["status"] as String)
            val version = (data["version"] as Number).toInt()
            val step = (data["step"] as Number).toInt()
            return TutorialProgress(status, version, step)
        }
    }
}

enum class AppLocale(val code: String) {
    Es("es"),
    En("en"),
    ;

    companion object {
        fun parse(code: String?): AppLocale? {
            val language = code
                ?.trim()
                ?.lowercase()
                ?.substringBefore('-')
                ?.substringBefore('_')
                .orEmpty()
            return when (language) {
                "es" -> Es
                "en" -> En
                else -> null
            }
        }

        /** Guardado, si existe; si no, idioma del dispositivo; si no es es/en, español. */
        fun resolve(savedCode: String?, deviceLanguage: String?): AppLocale =
            parse(savedCode) ?: parse(deviceLanguage) ?: Es
    }
}

/**
 * El campo `tutorial` no existe en las cuentas creadas antes de esta versión.
 * Esas cuentas no son altas nuevas: el tutorial queda completado.
 * [TutorialStatus.Pending] solo se asigna en el alta con Google.
 */
object TutorialMigrationPolicy {
    fun forNewGoogleRegistration(): TutorialProgress = TutorialProgress(
        status = TutorialStatus.Pending,
        version = TUTORIAL_VERSION,
        step = 0,
    )

    fun forExistingAccount(stored: TutorialProgress?): TutorialProgress =
        stored ?: TutorialProgress(
            status = TutorialStatus.Completed,
            version = TUTORIAL_VERSION,
            step = 0,
        )

    /**
     * [newGoogleRegistration] es verdadero solo en la transacción de alta,
     * antes de que exista el campo. Las lecturas posteriores pasan falso
     * y conservan lo guardado.
     */
    fun resolve(newGoogleRegistration: Boolean, stored: TutorialProgress?): TutorialProgress =
        if (newGoogleRegistration) forNewGoogleRegistration() else forExistingAccount(stored)
}

interface PreferencesRepository {
    fun readCityCode(): String?
    fun saveCityCode(code: String)
    fun readLocaleCode(): String?
    fun saveLocaleCode(code: String?)
    fun readOnboarding(): OnboardingStatus?
    fun saveOnboarding(status: OnboardingStatus)

    /** Null si la cuenta vieja no tiene el campo tutorial. */
    fun readTutorial(): TutorialProgress?
    fun saveTutorial(progress: TutorialProgress)
    fun locale(deviceLanguage: String?): AppLocale = AppLocale.resolve(readLocaleCode(), deviceLanguage)
}

class InMemoryPreferencesRepository : PreferencesRepository {
    private var cityCode: String? = null
    private var localeCode: String? = null
    private var onboarding: OnboardingStatus? = null
    private var tutorial: TutorialProgress? = null
    private var tutorialFieldPresent: Boolean = false

    override fun readCityCode(): String? = cityCode

    override fun saveCityCode(code: String) {
        require(code.isNotBlank()) { "Ciudad inválida." }
        cityCode = code
    }

    override fun readLocaleCode(): String? = localeCode

    override fun saveLocaleCode(code: String?) {
        if (code != null) require(AppLocale.parse(code) != null) { "Idioma inválido." }
        localeCode = code
    }

    override fun readOnboarding(): OnboardingStatus? = onboarding

    override fun saveOnboarding(status: OnboardingStatus) {
        onboarding = status
    }

    override fun readTutorial(): TutorialProgress? = if (tutorialFieldPresent) tutorial else null

    override fun saveTutorial(progress: TutorialProgress) {
        tutorial = progress
        tutorialFieldPresent = true
    }
}

fun PreferencesRepository.tutorialOnOpen(newGoogleRegistration: Boolean): TutorialProgress =
    TutorialMigrationPolicy.resolve(newGoogleRegistration, readTutorial())
