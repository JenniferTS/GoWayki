package pe.edu.tecsup.gowayki.domain.community

import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime

class InMemoryReportsRepository(
    private val store: LocalDraftStore = MemoryDraftStore(),
) : ReportsRepository {
    private var sequence = 0

    override fun newId(): String = "draft-${++sequence}"

    override fun active(now: UtcDateTime): ReportFeed {
        val items = store.list().mapNotNull { it.publishedOrNull() }.filter { it.activeAt(now) }
        return ReportFeed(items, fromCache = true)
    }

    override fun mine(uid: String?): List<StoredReport> {
        val author = authorOf(uid)
        return store.list().filter { it.author == author }
    }

    override fun saveDraft(draft: ReportDraft, uid: String?) {
        draft.validate()
        val current = store.find(draft.id)
        if (current != null && current.state != ReportPublicationState.Draft) {
            throw IllegalStateException("Ese reporte ya salió del borrador.")
        }
        store.upsert(
            StoredReport(
                id = draft.id,
                author = authorOf(uid),
                kind = draft.kind,
                description = draft.description,
                latitude = draft.latitude,
                longitude = draft.longitude,
                state = ReportPublicationState.Draft,
                createdAt = null,
                confirmations = 0,
                stopId = draft.stopId,
                lineId = draft.lineId,
            ),
        )
    }

    override fun markPending(id: String) {
        val current = store.find(id) ?: throw IllegalArgumentException("Borrador inexistente.")
        if (current.state != ReportPublicationState.Draft && current.state != ReportPublicationState.Pending) {
            throw IllegalStateException("Solo un borrador puede quedar pendiente.")
        }
        store.upsert(current.copy(state = ReportPublicationState.Pending))
    }

    override fun markPublished(id: String, createdAt: UtcDateTime) {
        val current = store.find(id) ?: throw IllegalArgumentException("Borrador inexistente.")
        if (current.state != ReportPublicationState.Pending) {
            throw IllegalStateException("Hay que dejarlo pendiente antes de publicarlo.")
        }
        store.upsert(
            current.copy(
                state = ReportPublicationState.Published,
                createdAt = createdAt,
            ),
        )
    }

    override fun confirm(reportId: String, now: UtcDateTime): Boolean {
        val published = store.find(reportId)?.publishedOrNull() ?: return false
        if (!published.activeAt(now)) return false
        val current = store.find(reportId) ?: return false
        store.upsert(current.copy(confirmations = current.confirmations + 1))
        return true
    }

    override fun delete(reportId: String) {
        store.remove(reportId)
    }

    private fun authorOf(uid: String?): ReportAuthor =
        if (uid.isNullOrBlank()) ReportAuthor.anonymous() else ReportAuthor.fromUid(uid)
}
