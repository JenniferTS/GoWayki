package pe.edu.tecsup.gowayki.data.community

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import pe.edu.tecsup.gowayki.domain.community.CommunityReport
import pe.edu.tecsup.gowayki.domain.community.RemoteReportsClient
import pe.edu.tecsup.gowayki.domain.community.ReportAuthor
import pe.edu.tecsup.gowayki.domain.community.ReportDraft
import pe.edu.tecsup.gowayki.domain.community.ReportFeed
import pe.edu.tecsup.gowayki.domain.community.ReportKind
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime

class FirestoreReportsClient(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : RemoteReportsClient {

    override fun newId(): String = db.collection("reports").document().id

    override fun createOrReuse(draft: ReportDraft, uid: String): CommunityReport {
        requireRealAccount(uid)
        draft.validate()
        val ref = db.collection("reports").document(draft.id)
        val payload = hashMapOf<String, Any>(
            "uid" to uid,
            "cityCode" to "arequipa",
            "type" to draft.kind.apiValue,
            "description" to draft.description.trim(),
            "latitude" to draft.latitude,
            "longitude" to draft.longitude,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "confirmations" to 0,
        )
        draft.stopId?.let { payload["stopId"] = it }
        draft.lineId?.let { payload["lineId"] = it }

        db.runTransaction { tx ->
            val existing = tx.get(ref)
            if (existing.exists()) {
                val owner = existing.getString("uid")
                if (owner != uid) {
                    throw FirebaseFirestoreException(
                        "Reporte ajeno",
                        FirebaseFirestoreException.Code.PERMISSION_DENIED,
                    )
                }
                return@runTransaction null
            }
            tx.set(ref, payload)
            null
        }.await(15)

        // Re-read for server timestamp
        val snap = ref.get().await(15)
        return snap.toCommunityReport()
            ?: CommunityReport(
                id = draft.id,
                author = ReportAuthor.fromUid(uid),
                kind = draft.kind,
                description = draft.description.trim(),
                latitude = draft.latitude,
                longitude = draft.longitude,
                createdAt = UtcDateTime.now(),
                confirmations = 0,
                stopId = draft.stopId,
                lineId = draft.lineId,
            )
    }

    override fun deleteMine(reportId: String, uid: String) {
        requireRealAccount(uid)
        val ref = db.collection("reports").document(reportId)
        val snap = ref.get().await(10)
        if (!snap.exists()) return
        if (snap.getString("uid") != uid) {
            throw IllegalStateException("Solo puedes retirar tus reportes.")
        }
        ref.delete().await(10)
    }

    override fun activeAround(
        cityCode: String,
        since: UtcDateTime,
        centerLat: Double?,
        centerLng: Double?,
        radiusKm: Double,
    ): ReportFeed {
        val sinceTs = Timestamp(since.epochMicros / 1_000_000L, ((since.epochMicros % 1_000_000L) * 1000).toInt())
        val query: Query = db.collection("reports")
            .whereEqualTo("cityCode", cityCode)
            .whereGreaterThan("createdAt", sinceTs)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(80)
        val snap = query.get().await(15)
        val now = UtcDateTime.now()
        var items = snap.documents.mapNotNull { it.toCommunityReport() }.filter { it.activeAt(now) }
        if (centerLat != null && centerLng != null) {
            items = items.filter { haversineKm(centerLat, centerLng, it.latitude, it.longitude) <= radiusKm }
        }
        return ReportFeed(items, fromCache = snap.metadata.isFromCache)
    }

    override fun mine(uid: String): List<CommunityReport> {
        val snap = db.collection("reports")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await(15)
        return snap.documents.mapNotNull { it.toCommunityReport() }
    }

    override fun confirm(reportId: String, uid: String): Boolean {
        requireRealAccount(uid)
        val doc = db.collection("reports").document(reportId)
        val marker = doc.collection("confirmations").document(uid)
        val index = db.collection("users").document(uid)
            .collection("confirmationRefs").document(reportId)
        return db.runTransaction { tx ->
            val report = tx.get(doc)
            val vote = tx.get(marker)
            if (auth.currentUser?.uid != uid) error("Session changed")
            if (!report.exists()) error("Reporte no existe")
            val created = report.getTimestamp("createdAt")
                ?: error("Sin fecha")
            val expiresMs = created.toDate().time + 72L * 3_600_000L
            if (expiresMs <= System.currentTimeMillis()) error("Reporte vencido")
            if (vote.exists()) return@runTransaction false
            val count = (report.getLong("confirmations") ?: 0L).toInt()
            tx.update(doc, "confirmations", count + 1)
            tx.set(
                marker,
                mapOf(
                    "uid" to uid,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            )
            tx.set(index, mapOf("id" to reportId))
            true
        }.await(15)
    }

    private fun requireRealAccount(uid: String) {
        val user = auth.currentUser
        if (user == null || user.isAnonymous || user.uid != uid) {
            throw IllegalStateException("Necesitas una cuenta Google (no invitado).")
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toCommunityReport(): CommunityReport? {
        val data = data ?: return null
        val created = data["createdAt"] as? Timestamp ?: return null
        return try {
            CommunityReport(
                id = id,
                author = ReportAuthor.fromUid(data["uid"] as String),
                kind = ReportKind.fromApi(data["type"] as String),
                description = data["description"] as String,
                latitude = (data["latitude"] as Number).toDouble(),
                longitude = (data["longitude"] as Number).toDouble(),
                createdAt = UtcDateTime(created.seconds * 1_000_000L + created.nanoseconds / 1000L),
                confirmations = (data["confirmations"] as? Number)?.toInt() ?: 0,
                stopId = data["stopId"] as String?,
                lineId = data["lineId"] as String?,
            )
        } catch (_: Exception) {
            null
        }
    }
}

private fun <T> com.google.android.gms.tasks.Task<T>.await(seconds: Long): T {
    return com.google.android.gms.tasks.Tasks.await(this, seconds, TimeUnit.SECONDS)
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * atan2(sqrt(a), sqrt(1 - a))
}
