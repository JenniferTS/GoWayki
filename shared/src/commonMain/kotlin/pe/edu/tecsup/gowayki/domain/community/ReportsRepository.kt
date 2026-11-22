package pe.edu.tecsup.gowayki.domain.community

import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime

/** Borradores locales. No publica ni confirma. */
interface LocalDraftStore {
    fun upsert(report: StoredReport)
    fun find(id: String): StoredReport?
    fun list(): List<StoredReport>
    fun remove(id: String)
}

class MemoryDraftStore : LocalDraftStore {
    private val items = LinkedHashMap<String, StoredReport>()

    override fun upsert(report: StoredReport) {
        items[report.id] = report
    }

    override fun find(id: String): StoredReport? = items[id]

    override fun list(): List<StoredReport> = items.values.toList()

    override fun remove(id: String) {
        items.remove(id)
    }
}

interface ReportsRepository {
    fun newId(): String
    fun active(now: UtcDateTime): ReportFeed
    fun mine(uid: String?): List<StoredReport>
    fun saveDraft(draft: ReportDraft, uid: String?)
    fun markPending(id: String)
    fun markPublished(id: String, createdAt: UtcDateTime)
    fun confirm(reportId: String, now: UtcDateTime): Boolean
    fun delete(reportId: String)
}
