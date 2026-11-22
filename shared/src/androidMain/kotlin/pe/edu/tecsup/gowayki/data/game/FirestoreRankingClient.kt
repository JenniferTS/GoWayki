package pe.edu.tecsup.gowayki.data.game

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import pe.edu.tecsup.gowayki.domain.game.PendingScore
import pe.edu.tecsup.gowayki.domain.game.RankingClient
import pe.edu.tecsup.gowayki.domain.game.RankingScore
import pe.edu.tecsup.gowayki.domain.game.WeeklyRanking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class FirestoreRankingClient(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : RankingClient {
    private fun scores(week: String) =
        db.collection("leaderboards").document(week).collection("scores")

    private fun decode(id: String, data: Map<String, Any?>): RankingScore = RankingScore(
        uid = id,
        name = data["displayName"] as String,
        score = (data["score"] as Number).toInt(),
        durationMs = (data["durationMs"] as Number).toInt(),
    )

    override fun load(week: String, uid: String?): WeeklyRanking {
        val topSnap = await(
            scores(week)
                .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .orderBy("updatedAt")
                .limit(20)
                .get(Source.SERVER),
        )
        val top = topSnap.documents.map { decode(it.id, it.data ?: emptyMap()) }
        val mine = if (uid == null) {
            null
        } else {
            val doc = await(scores(week).document(uid).get(Source.SERVER))
            if (doc.exists()) decode(doc.id, doc.data ?: emptyMap()) else null
        }
        val position = mine?.let { own ->
            val idx = top.indexOfFirst { it.uid == own.uid }
            if (idx >= 0) idx + 1 else top.count { it.score > own.score } + 1
        }
        return WeeklyRanking(top = top, own = mine, position = position)
    }

    override fun publish(uid: String, name: String, score: PendingScore): Int {
        PendingScore.fromMap(score.toMap())
        val doc = scores(score.week).document(uid)
        val index = db.collection("users").document(uid)
            .collection("scoreRefs").document(score.week)
        return await(
            db.runTransaction { tx ->
                val previous = tx.get(doc)
                val reference = tx.get(index)
                if (auth.currentUser?.uid != uid) error("Session changed")
                val best = previous.getLong("score")?.toInt()
                if (best != null && best >= score.score) return@runTransaction best
                val display = name.trim().take(60)
                if (display.isEmpty()) error("Missing public display label")
                val payload = hashMapOf<String, Any>(
                    "uid" to uid,
                    "displayName" to display,
                    "score" to score.score,
                    "durationMs" to score.durationMs,
                    "createdAt" to (previous.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                tx.set(doc, payload)
                if (!reference.exists()) tx.set(index, mapOf("id" to score.week))
                score.score
            },
        )
    }

    private fun <T> await(task: com.google.android.gms.tasks.Task<T>, seconds: Long = 15): T {
        val latch = CountDownLatch(1)
        val result = AtomicReference<T>()
        val error = AtomicReference<Exception>()
        task.addOnSuccessListener {
            result.set(it)
            latch.countDown()
        }.addOnFailureListener {
            error.set(it)
            latch.countDown()
        }
        if (!latch.await(seconds, TimeUnit.SECONDS)) error("Firestore timeout")
        error.get()?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result.get() as T
    }
}
