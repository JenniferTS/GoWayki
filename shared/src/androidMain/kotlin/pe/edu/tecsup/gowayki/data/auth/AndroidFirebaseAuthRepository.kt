package pe.edu.tecsup.gowayki.data.auth

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import pe.edu.tecsup.gowayki.domain.auth.AuthRepository
import pe.edu.tecsup.gowayki.domain.auth.SignedInUser

/**
 * Auth real en Android. [beginGoogleSignIn] lanza el Intent; [completeGoogleSignIn]
 * se llama desde onActivityResult / Activity Result API.
 */
class AndroidFirebaseAuthRepository(
    private val activity: Activity,
    webClientId: String,
) : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val googleClient: GoogleSignInClient = GoogleSignIn.getClient(
        activity,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build(),
    )

    fun beginGoogleSignIn(): Intent = googleClient.signInIntent

    fun completeGoogleSignInAsync(
        data: Intent?,
        onSuccess: (SignedInUser) -> Unit,
        onError: (String) -> Unit,
    ) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = try {
            task.getResult(ApiException::class.java)
        } catch (e: Exception) {
            onError("Google Sign-In falló: ${e.message}")
            return
        }
        val idToken = account.idToken
        if (idToken == null) {
            onError("Sin idToken de Google.")
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val u = result.user
                if (u == null) {
                    onError("Firebase no devolvió usuario.")
                    return@addOnSuccessListener
                }
                onSuccess(
                    SignedInUser(
                        uid = u.uid,
                        displayName = u.displayName,
                        email = u.email,
                        isAnonymous = u.isAnonymous,
                        isNewUser = result.additionalUserInfo?.isNewUser == true,
                    ),
                )
            }
            .addOnFailureListener { onError(it.message ?: "Auth Firebase falló") }
    }

    suspend fun completeGoogleSignIn(data: Intent?): SignedInUser = suspendCoroutine { cont ->
        completeGoogleSignInAsync(
            data = data,
            onSuccess = { cont.resume(it) },
            onError = { cont.resumeWithException(IllegalStateException(it)) },
        )
    }

    override fun currentUser(): SignedInUser? {
        val u = auth.currentUser ?: return null
        return SignedInUser(
            uid = u.uid,
            displayName = u.displayName,
            email = u.email,
            isAnonymous = u.isAnonymous,
            isNewUser = false,
        )
    }

    override suspend fun signInAnonymously(): SignedInUser = suspendCoroutine { cont ->
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val u = result.user!!
                cont.resume(
                    SignedInUser(u.uid, u.displayName, u.email, u.isAnonymous, result.additionalUserInfo?.isNewUser == true),
                )
            }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    override suspend fun signInWithGoogle(): SignedInUser {
        throw UnsupportedOperationException("En Android usa beginGoogleSignIn + completeGoogleSignIn.")
    }

    override suspend fun signOut() {
        auth.signOut()
        googleClient.signOut()
    }
}
