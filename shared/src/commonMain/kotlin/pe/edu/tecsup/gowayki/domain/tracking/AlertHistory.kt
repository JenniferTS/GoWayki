package pe.edu.tecsup.gowayki.domain.tracking

data class AlertEvent(
    val atEpochMs: Long,
    val title: String,
    val body: String,
)

class AlertHistory(private val limit: Int = 30) {
    private val items = ArrayDeque<AlertEvent>()

    fun record(event: AlertEvent) {
        items.addFirst(event)
        while (items.size > limit) items.removeLast()
    }

    fun all(): List<AlertEvent> = items.toList()

    fun clear() = items.clear()
}
