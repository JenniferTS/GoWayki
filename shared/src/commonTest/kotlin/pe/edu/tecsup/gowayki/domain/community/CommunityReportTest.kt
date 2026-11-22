package pe.edu.tecsup.gowayki.domain.community

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pe.edu.tecsup.gowayki.domain.planner.PlannerDuration
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime

class CommunityReportTest {
    private val now = UtcDateTime.of(2026, 11, 22, 16, 0, 0)

    @Test
    fun expiryDependsOnKindAndLegacyReportsLastSeventyTwoHours() {
        assertFalse(report(ReportKind.HeavyTraffic, hoursAgo = 2).activeAt(now))
        assertTrue(report(ReportKind.HeavyTraffic, hoursAgo = 2, extraMicros = 1).activeAt(now))
        assertFalse(report(ReportKind.HeavyTraffic, hoursAgo = 3).activeAt(now))
        assertTrue(report(ReportKind.Detour, hoursAgo = 3).activeAt(now))
        assertFalse(report(ReportKind.Detour, hoursAgo = 6).activeAt(now))
        assertTrue(report(ReportKind.RoadBlocked, hoursAgo = 3).activeAt(now))
        assertFalse(report(ReportKind.RoadBlocked, hoursAgo = 4).activeAt(now))
        assertTrue(report(ReportKind.Other, hoursAgo = 71).activeAt(now))
        assertFalse(report(ReportKind.Other, hoursAgo = 72).activeAt(now))
        assertFalse(report(ReportKind.MissingStop, hoursAgo = 72).activeAt(now))
        assertTrue(report(ReportKind.StopName, hoursAgo = 71).activeAt(now))
        assertFalse(report(ReportKind.LineMissing, hoursAgo = 72).activeAt(now))
        assertFalse(report(ReportKind.Detour, hoursAgo = -1).activeAt(now))
    }

    @Test
    fun readsLegacyApiValuesAndHidesTheAccountId() {
        assertEquals(ReportKind.HeavyTraffic, ReportKind.fromApi("heavy_traffic"))
        assertEquals(ReportKind.RoadBlocked, ReportKind.fromApi("road_blocked"))
        assertEquals(ReportKind.MissingStop, ReportKind.fromApi("missing_stop"))
        assertEquals(ReportKind.StopName, ReportKind.fromApi("stop_name"))
        assertEquals(ReportKind.LineMissing, ReportKind.fromApi("line_missing"))
        assertFailsWith<IllegalArgumentException> { ReportKind.fromApi("closed_street") }
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc".encodeToByteArray()),
        )
        val remote = CommunityReport.fromRemote(
            "report-1",
            mapOf(
                "uid" to "alice",
                "type" to "missing_stop",
                "description" to "Falta el paradero de prueba",
                "latitude" to -16.4,
                "longitude" to -71.5,
                "createdAt" to now,
                "confirmations" to 1,
                "stopId" to "stop-1",
            ),
        )
        assertEquals("2bd806c97f0e00af", remote.author.publicId)
        assertTrue(remote.author.publicId != "alice")
        assertFalse(remote.toString().contains("alice"))
        assertTrue(COMMUNITY_DISCLAIMER.contains("comunidad"))
        assertTrue(COMMUNITY_DISCLAIMER.contains("no los verifica"))
        assertFailsWith<IllegalArgumentException> {
            ReportDraft(
                id = "draft",
                kind = ReportKind.LineMissing,
                description = "La línea no aparece en el mapa",
                latitude = -16.4,
                longitude = -71.5,
                lineId = "line-1",
            ).validate()
        }
    }

    @Test
    fun offlineDraftStaysHiddenUntilItIsPublishedAndThenExpires() {
        val repository = InMemoryReportsRepository()
        val heavyId = repository.newId()
        repository.saveDraft(
            ReportDraft(heavyId, ReportKind.HeavyTraffic, "Hay tráfico en la avenida", -16.4, -71.5),
            uid = "alice",
        )
        assertTrue(repository.active(now).items.isEmpty())
        repository.markPending(heavyId)
        assertTrue(repository.active(now).items.isEmpty())
        assertEquals(ReportPublicationState.Pending, repository.mine("alice").single().state)
        assertTrue(repository.mine("alice").single().author.publicId != "alice")
        assertTrue(repository.mine("bob").isEmpty())
        repository.markPublished(heavyId, now - PlannerDuration.of(hours = 3))
        assertTrue(repository.active(now).items.isEmpty())

        val detourId = repository.newId()
        repository.saveDraft(
            ReportDraft(detourId, ReportKind.Detour, "Desvío por obra en la avenida", -16.4, -71.5),
            uid = "alice",
        )
        repository.markPending(detourId)
        repository.markPublished(detourId, now - PlannerDuration.of(hours = 3))
        assertEquals(listOf(detourId), repository.active(now).items.map { it.id })
        assertTrue(repository.confirm(detourId, now))
        assertEquals(1, repository.active(now).items.single().confirmations)
        assertFalse(repository.confirm(heavyId, now))
        repository.delete(detourId)
        assertTrue(repository.active(now).items.isEmpty())
    }

    private fun report(kind: ReportKind, hoursAgo: Int, extraMicros: Long = 0L): CommunityReport {
        val age = PlannerDuration.of(hours = hoursAgo)
        val created = if (extraMicros == 0L) {
            now - age
        } else {
            now - age + PlannerDuration.of(microseconds = extraMicros)
        }
        return CommunityReport(
            id = kind.apiValue,
            author = ReportAuthor.anonymous(),
            kind = kind,
            description = "Aviso de prueba",
            latitude = -16.4,
            longitude = -71.5,
            createdAt = created,
            confirmations = 0,
        )
    }
}
