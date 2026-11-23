package pe.edu.tecsup.gowayki.data.auth

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import pe.edu.tecsup.gowayki.domain.auth.AuthRepository
import pe.edu.tecsup.gowayki.domain.auth.SignedInUser

/**
 * Auth real en Android. [prepareGoogleSignIn] limpia sesión previa y entrega el Intent;
 * [completeGoogleSignInAsync] se llama desde Activity Result API.
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
            .requestProfile()
            .build(),
    )

    /** Cierra cuenta Google cacheada y luego entrega el Intent de login. */
    fun prepareGoogleSignIn(onReady: (Intent) -> Unit) {
        googleClient.signOut().addOnCompleteListener {
            onReady(googleClient.signInIntent)
        }
    }

    fun beginGoogleSignIn(): Intent = googleClient.signInIntent

    fun completeGoogleSignInAsync(
        data: Intent?,
        onSuccess: (SignedInUser) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (data == null) {
            onError("Inicio de sesión cancelado.")
            return
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = try {
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            onError(messageForGoogleStatus(e.statusCode, e.message))
            return
        } catch (e: Exception) {
            onError("Google Sign-In falló: ${e.message ?: "error desconocido"}")
            return
        }
        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            onError("Google no devolvió idToken. Revisa el Web Client ID / SHA del APK.")
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
                        displayName = u.displayName ?: account.displayName,
                        email = u.email ?: account.email,
                        isAnonymous = u.isAnonymous,
                        isNewUser = result.additionalUserInfo?.isNewUser == true,
                    ),
                )
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Auth Firebase falló")
            }
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
        throw UnsupportedOperationException("En Android usa prepareGoogleSignIn + completeGoogleSignIn.")
    }

    override suspend fun signOut() {
        auth.signOut()
        googleClient.signOut()
    }

    private fun messageForGoogleStatus(code: Int, detail: String?): String = when (code) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Inicio de sesión cancelado."
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Ya hay un inicio de sesión en curso."
        GoogleSignInStatusCodes.NETWORK_ERROR -> "Sin red. Revisa tu conexión e inténtalo de nuevo."
        GoogleSignInStatusCodes.DEVELOPER_ERROR ->
            "Configuración Google incompleta (SHA-1 del APK / cliente OAuth)."
        GoogleSignInStatusCodes.INVALID_ACCOUNT -> "Cuenta de Google no válida."
        else -> "Google Sign-In falló (código $code)${detail?.let { ": $it" } ?: "."}"
    }
}
