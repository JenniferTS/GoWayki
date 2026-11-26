package pe.edu.tecsup.gowayki.domain.community

data class ReportListFilter(
    val kinds: Set<ReportKind> = ReportKind.entries.toSet(),
    val maxDistanceMeters: Double? = null,
    val onlySinceEpochMs: Long? = null,
) {
    fun matches(
        kind: ReportKind,
        distanceMeters: Double?,
        createdEpochMs: Long,
    ): Boolean {
        if (kind !in kinds) return false
        if (maxDistanceMeters != null && distanceMeters != null && distanceMeters > maxDistanceMeters) return false
        if (onlySinceEpochMs != null && createdEpochMs < onlySinceEpochMs) return false
        return true
    }
}
