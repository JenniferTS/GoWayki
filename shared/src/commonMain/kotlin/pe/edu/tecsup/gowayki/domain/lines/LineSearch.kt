package pe.edu.tecsup.gowayki.domain.lines

data class LineSummary(
    val id: String,
    val name: String,
    val agency: String = "",
    val code: String = "",
)

fun filterLines(query: String, lines: List<LineSummary>): List<LineSummary> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return lines
    return lines.filter {
        it.name.lowercase().contains(q) ||
            it.code.lowercase().contains(q) ||
            it.agency.lowercase().contains(q) ||
            it.id.lowercase().contains(q)
    }
}
