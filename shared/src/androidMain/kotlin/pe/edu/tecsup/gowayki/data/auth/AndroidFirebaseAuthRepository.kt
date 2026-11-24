package pe.edu.tecsup.gowayki.data.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import pe.edu.tecsup.gowayki.domain.auth.AuthRepository
import pe.edu.tecsup.gowayki.domain.auth.SignedInUser

/**
 * Auth real en Android: Google vía Credential Manager → credencial Firebase.
 * Errores de login salen como [GoogleSignInException] con mensaje listo para UI.
 */
class AndroidFirebaseAuthRepository(
    private val activity: Activity,
    private val webClientId: String,
) : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(activity)

    override suspend fun signInWithGoogle(): SignedInUser {
        val idToken = requestGoogleIdToken()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return suspendCoroutine { cont ->
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val u = result.user
                    if (u == null) {
                        cont.resumeWithException(GoogleSignInException("Firebase no devolvió usuario."))
                    } else {
                        cont.resume(
                            SignedInUser(
                                uid = u.uid,
                                displayName = u.displayName,
                                email = u.email,
                                isAnonymous = u.isAnonymous,
                                isNewUser = result.additionalUserInfo?.isNewUser == true,
                            ),
                        )
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(GoogleSignInException(e.message ?: "Auth Firebase falló."))
                }
        }
    }

    private suspend fun requestGoogleIdToken(): String {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            credentialManager.getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw GoogleSignInException("Inicio de sesión cancelado.", cancelled = true)
        } catch (e: NoCredentialException) {
            throw GoogleSignInException("No hay cuentas de Google en el dispositivo.")
        } catch (e: GetCredentialException) {
            throw GoogleSignInException(
                "Google Sign-In falló (${e.type}). Revisa SHA-1 del APK / Web Client ID.",
            )
        }
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw GoogleSignInException("Credencial inesperada: ${credential.type}")
        }
        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw GoogleSignInException("Google no devolvió un idToken válido.")
        }
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

    override suspend fun signOut() {
        auth.signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}

class GoogleSignInException(message: String, val cancelled: Boolean = false) : Exception(message)
