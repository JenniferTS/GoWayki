package pe.edu.tecsup.gowayki.data.sync

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.concurrent.TimeUnit
import pe.edu.tecsup.gowayki.domain.preferences.OnboardingStatus
import pe.edu.tecsup.gowayki.domain.preferences.TutorialProgress

/** Sincroniza onboarding/tutorial en users/{uid}/preferences/{doc} (reglas actuales). */
class FirestoreUserStateSync(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun pushTutorial(uid: String, progress: TutorialProgress) {
        val ref = db.collection("users").document(uid)
            .collection("preferences").document("tutorial")
        Tasks.await(
            ref.set(progress.toStorageMap(), SetOptions.merge()),
            12,
            TimeUnit.SECONDS,
        )
    }

    fun pullTutorial(uid: String): TutorialProgress? {
        val snap = Tasks.await(
            db.collection("users").document(uid)
                .collection("preferences").document("tutorial").get(),
            12,
            TimeUnit.SECONDS,
        )
        if (!snap.exists()) return null
        return TutorialProgress.fromStorage(snap.data)
    }

    fun pushOnboarding(uid: String, status: OnboardingStatus) {
        val ref = db.collection("users").document(uid)
            .collection("preferences").document("onboarding")
        Tasks.await(
            ref.set(mapOf("status" to status.storageValue), SetOptions.merge()),
            12,
            TimeUnit.SECONDS,
        )
    }

    fun pullOnboarding(uid: String): OnboardingStatus? {
        val snap = Tasks.await(
            db.collection("users").document(uid)
                .collection("preferences").document("onboarding").get(),
            12,
            TimeUnit.SECONDS,
        )
        val status = snap.getString("status") ?: return null
        return OnboardingStatus.fromStorage(status)
    }

    fun ensureUserDoc(uid: String) {
        val ref = db.collection("users").document(uid)
        Tasks.await(ref.set(mapOf("updatedAt" to System.currentTimeMillis()), SetOptions.merge()), 12, TimeUnit.SECONDS)
    }
}
