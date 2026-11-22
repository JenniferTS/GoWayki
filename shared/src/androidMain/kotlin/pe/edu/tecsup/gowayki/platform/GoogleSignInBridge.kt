package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.data.auth.AndroidFirebaseAuthRepository
import pe.edu.tecsup.gowayki.domain.auth.SignedInUser

object GoogleSignInBridge {
    var launch: (() -> Unit)? = null
    var authRepository: AndroidFirebaseAuthRepository? = null
    var onSuccess: ((SignedInUser) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
}
