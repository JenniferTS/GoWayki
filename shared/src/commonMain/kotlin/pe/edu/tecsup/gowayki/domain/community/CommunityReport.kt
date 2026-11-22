package pe.edu.tecsup.gowayki.domain.community

import kotlin.math.abs
import pe.edu.tecsup.gowayki.domain.planner.PlannerDuration
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime

/**
 * Los reportes de la comunidad los envían otras personas. GoWayki no los verifica:
 * pueden estar equivocados, vencidos o ser falsos, y no describen el estado oficial
 * de las calles ni de las combis.
 */
const val COMMUNITY_DISCLAIMER: String =
    "Los reportes de la comunidad los envían otras personas. GoWayki no los verifica: " +
        "pueden estar equivocados, vencidos o ser falsos, y no describen el estado oficial " +
        "de las calles ni de las combis."

enum class ReportKind(val apiValue: String) {
    HeavyTraffic("heavy_traffic"),
    Detour("detour"),
    RoadBlocked("road_blocked"),
    Other("other"),
    MissingStop("missing_stop"),
    StopName("stop_name"),
    LineMissing("line_missing"),
    ;

    val legacy: Boolean
        get() = this == MissingStop || this == StopName || this == LineMissing

    fun lifetime(): PlannerDuration = when (this) {
        HeavyTraffic -> PlannerDuration.of(hours = 2)
        Detour -> PlannerDuration.of(hours = 6)
        RoadBlocked -> PlannerDuration.of(hours = 4)
        Other, MissingStop, StopName, LineMissing -> PlannerDuration.of(hours = 72)
    }

    companion object {
        fun fromApi(value: String): ReportKind =
            values().firstOrNull { it.apiValue == value }
                ?: throw IllegalArgumentException("Tipo de reporte desconocido.")
    }
}

enum class ReportPublicationState { Draft, Pending, Published }

/** Lo único que se muestra de quien escribe: un hash corto, o la etiqueta anónima. */
data class ReportAuthor(val publicId: String) {
    init {
        require(publicId.isNotEmpty()) { "Autor inválido." }
    }

    val isAnonymous: Boolean get() = publicId == ANONYMOUS

    companion object {
        const val ANONYMOUS: String = "anónimo"

        fun anonymous(): ReportAuthor = ReportAuthor(ANONYMOUS)

        fun fromUid(uid: String): ReportAuthor {
            require(uid.isNotBlank()) { "Cuenta inválida." }
            return ReportAuthor(sha256Hex(uid.encodeToByteArray()).substring(0, 16))
        }
    }
}

data class CommunityReport(
    val id: String,
    val author: ReportAuthor,
    val kind: ReportKind,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: UtcDateTime,
    val confirmations: Int,
    val stopId: String? = null,
    val lineId: String? = null,
) {
    val expiresAt: UtcDateTime get() = createdAt + kind.lifetime()

    fun activeAt(now: UtcDateTime): Boolean = !createdAt.isAfter(now) && expiresAt.isAfter(now)

    companion object {
        fun fromRemote(id: String, data: Map<String, Any?>): CommunityReport {
            val latitude = (data["latitude"] as Number).toDouble()
            val longitude = (data["longitude"] as Number).toDouble()
            val kind = ReportKind.fromApi(data["type"] as String)
            val description = data["description"] as String
            val uid = data["uid"] as String
            val createdAt = data["createdAt"] as UtcDateTime
            val count = (data["confirmations"] as Number).toInt()
            val stopId = data["stopId"] as String?
            val lineId = data["lineId"] as String?
            if (!latitude.finite() ||
                !longitude.finite() ||
                abs(latitude) > 90.0 ||
                abs(longitude) > 180.0 ||
                uid.isEmpty() ||
                description.length < 3 ||
                description.length > 500 ||
                count < 0 ||
                (kind == ReportKind.LineMissing && lineId.isNullOrEmpty())
            ) {
                throw IllegalArgumentException("Reporte inválido.")
            }
            return CommunityReport(
                id = id,
                author = ReportAuthor.fromUid(uid),
                kind = kind,
                description = description,
                latitude = latitude,
                longitude = longitude,
                createdAt = createdAt,
                confirmations = count,
                stopId = stopId,
                lineId = lineId,
            )
        }
    }
}

data class ReportDraft(
    val id: String,
    val kind: ReportKind,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val stopId: String? = null,
    val lineId: String? = null,
) {
    fun validate() {
        if (kind.legacy) throw IllegalArgumentException("Ese tipo de reporte ya no se puede crear.")
        val text = description.trim()
        if (text.length < 3 || text.length > 500) throw IllegalArgumentException("Descripción inválida.")
        if (!latitude.finite() ||
            !longitude.finite() ||
            abs(latitude) > 90.0 ||
            abs(longitude) > 180.0 ||
            (stopId?.length ?: 0) > 128 ||
            (lineId?.length ?: 0) > 128
        ) {
            throw IllegalArgumentException("Reporte inválido.")
        }
    }
}

data class StoredReport(
    val id: String,
    val author: ReportAuthor,
    val kind: ReportKind,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val state: ReportPublicationState,
    val createdAt: UtcDateTime?,
    val confirmations: Int,
    val stopId: String? = null,
    val lineId: String? = null,
) {
    fun publishedOrNull(): CommunityReport? {
        val at = createdAt ?: return null
        if (state != ReportPublicationState.Published) return null
        return CommunityReport(
            id = id,
            author = author,
            kind = kind,
            description = description,
            latitude = latitude,
            longitude = longitude,
            createdAt = at,
            confirmations = confirmations,
            stopId = stopId,
            lineId = lineId,
        )
    }
}

data class ReportFeed(
    val items: List<CommunityReport>,
    val fromCache: Boolean,
)

private fun Double.finite(): Boolean = !isNaN() && !isInfinite()
