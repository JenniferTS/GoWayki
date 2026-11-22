package pe.edu.tecsup.gowayki.domain.community

import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime

/**
 * Acceso remoto a reportes. En Android usa Firestore; en tests puede ser memoria.
 * El id del borrador es el id del documento remoto para evitar duplicados al reintentar.
 */
interface RemoteReportsClient {
    fun newId(): String
    /** Publica o no-op si el doc ya existe con el mismo uid (anti-duplicado). */
    fun createOrReuse(draft: ReportDraft, uid: String): CommunityReport
    fun deleteMine(reportId: String, uid: String)
    fun activeAround(
        cityCode: String = "arequipa",
        since: UtcDateTime,
        centerLat: Double? = null,
        centerLng: Double? = null,
        radiusKm: Double = 8.0,
    ): ReportFeed
    fun mine(uid: String): List<CommunityReport>
}

class NoRemoteReportsClient : RemoteReportsClient {
    override fun newId(): String = "local-${UtcDateTime.now().epochMicros}"
    override fun createOrReuse(draft: ReportDraft, uid: String): CommunityReport {
        throw UnsupportedOperationException("Sin backend remoto.")
    }
    override fun deleteMine(reportId: String, uid: String) = Unit
    override fun activeAround(
        cityCode: String,
        since: UtcDateTime,
        centerLat: Double?,
        centerLng: Double?,
        radiusKm: Double,
    ): ReportFeed = ReportFeed(emptyList(), fromCache = true)
    override fun mine(uid: String): List<CommunityReport> = emptyList()
}
