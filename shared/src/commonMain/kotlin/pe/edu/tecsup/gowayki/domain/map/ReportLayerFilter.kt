package pe.edu.tecsup.gowayki.domain.map

import pe.edu.tecsup.gowayki.domain.community.ReportKind

/** Filtro de capa de reportes en el mapa. */
data class ReportLayerFilter(
    val kinds: Set<ReportKind> = ReportKind.entries.toSet(),
) {
    fun allows(kind: ReportKind): Boolean = kind in kinds

    fun toggle(kind: ReportKind): ReportLayerFilter {
        val next = kinds.toMutableSet()
        if (!next.add(kind)) next.remove(kind)
        return copy(kinds = next.ifEmpty { setOf(kind) })
    }
}
