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

/** Línea persistida: name|lat|lng */
data class SavedPlace(val name: String, val latitude: Double, val longitude: Double) {
    fun encode(): String = "$name|$latitude|$longitude"

    companion object {
        fun decode(raw: String): SavedPlace? {
            val p = raw.split('|')
            if (p.size < 3) return null
            val lat = p[p.size - 2].toDoubleOrNull() ?: return null
            val lng = p[p.size - 1].toDoubleOrNull() ?: return null
            val name = p.dropLast(2).joinToString("|")
            if (name.isBlank()) return null
            return SavedPlace(name, lat, lng)
        }
    }
}

/** Viaje reciente: fromName|fromLat|fromLng|toName|toLat|toLng */
data class RecentTrip(
    val from: SavedPlace,
    val to: SavedPlace,
) {
    fun encode(): String = "${from.encode()}||${to.encode()}"

    companion object {
        fun decode(raw: String): RecentTrip? {
            val parts = raw.split("||")
            if (parts.size != 2) return null
            val from = SavedPlace.decode(parts[0]) ?: return null
            val to = SavedPlace.decode(parts[1]) ?: return null
            return RecentTrip(from, to)
        }
    }
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
    fun readAlarmMode(): String?
    fun saveAlarmMode(mode: String)
    fun readFavorites(): List<SavedPlace>
    fun saveFavorites(places: List<SavedPlace>)
    fun readRecents(): List<RecentTrip>
    fun saveRecents(trips: List<RecentTrip>)
    /** Cola de puntajes de una cuenta: líneas `week|score|durationMs`. */
    fun readPendingScores(uid: String): List<String>
    fun savePendingScores(uid: String, lines: List<String>)

    /** Cola única sin uid de versiones anteriores: no se sabe de qué cuenta es, solo se descarta. */
    fun hasLegacyPendingScores(): Boolean
    fun discardLegacyPendingScores()

    /** El usuario aceptó el aviso de ubicación antes del permiso del sistema. */
    fun readLocationDisclosureAccepted(): Boolean
    fun saveLocationDisclosureAccepted(accepted: Boolean)

    /** Cuentas con borrado iniciado y sin terminar: sus subidas quedan en pausa hasta completarlo. */
    fun readAccountsBeingDeleted(): Set<String>
    fun saveAccountBeingDeleted(uid: String, deleting: Boolean)

    /** Combi elegida en Wayki Combi (nombre del enum `Micro`). */
    fun readGameCombi(): String?
    fun saveGameCombi(name: String)
    fun locale(deviceLanguage: String?): AppLocale = AppLocale.resolve(readLocaleCode(), deviceLanguage)
}

class InMemoryPreferencesRepository : PreferencesRepository {
    private var cityCode: String? = null
    private var localeCode: String? = null
    private var onboarding: OnboardingStatus? = null
    private var tutorial: TutorialProgress? = null
    private var tutorialFieldPresent: Boolean = false
    private var alarmMode: String? = null
    private var favorites: List<SavedPlace> = emptyList()
    private var recents: List<RecentTrip> = emptyList()
    private val pendingScores = mutableMapOf<String, List<String>>()
    private var legacyPendingScores: List<String> = emptyList()
    private var locationDisclosureAccepted = false
    private val accountsBeingDeleted = mutableSetOf<String>()
    private var gameCombi: String? = null

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

    override fun readAlarmMode(): String? = alarmMode
    override fun saveAlarmMode(mode: String) {
        alarmMode = mode
    }

    override fun readFavorites(): List<SavedPlace> = favorites
    override fun saveFavorites(places: List<SavedPlace>) {
        favorites = places.take(20)
    }

    override fun readRecents(): List<RecentTrip> = recents
    override fun saveRecents(trips: List<RecentTrip>) {
        recents = trips.take(10)
    }

    override fun readPendingScores(uid: String): List<String> = pendingScores[uid].orEmpty()
    override fun savePendingScores(uid: String, lines: List<String>) {
        if (lines.isEmpty()) pendingScores.remove(uid) else pendingScores[uid] = lines
    }

    override fun hasLegacyPendingScores(): Boolean = legacyPendingScores.isNotEmpty()
    override fun discardLegacyPendingScores() {
        legacyPendingScores = emptyList()
    }

    /** Simula la cola compartida que dejaban las versiones anteriores. */
    fun seedLegacyPendingScores(lines: List<String>) {
        legacyPendingScores = lines
    }

    override fun readLocationDisclosureAccepted(): Boolean = locationDisclosureAccepted
    override fun saveLocationDisclosureAccepted(accepted: Boolean) {
        locationDisclosureAccepted = accepted
    }

    override fun readGameCombi(): String? = gameCombi
    override fun saveGameCombi(name: String) {
        gameCombi = name
    }

    override fun readAccountsBeingDeleted(): Set<String> = accountsBeingDeleted.toSet()
    override fun saveAccountBeingDeleted(uid: String, deleting: Boolean) {
        if (deleting) accountsBeingDeleted.add(uid) else accountsBeingDeleted.remove(uid)
    }
}

fun PreferencesRepository.tutorialOnOpen(newGoogleRegistration: Boolean): TutorialProgress =
    TutorialMigrationPolicy.resolve(newGoogleRegistration, readTutorial())
