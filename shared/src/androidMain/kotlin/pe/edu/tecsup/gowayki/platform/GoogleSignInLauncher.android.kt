package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.auth.SignedInUser

actual fun launchPlatformGoogleSignIn(
    onSuccess: (SignedInUser) -> Unit,
    onError: (String) -> Unit,
) {
    GoogleSignInBridge.onSuccess = onSuccess
    GoogleSignInBridge.onError = onError
    val launch = GoogleSignInBridge.launch
    if (launch == null) {
        onError("Google Sign-In no está listo.")
    } else {
        launch()
    }
}

actual fun signOutPlatformGoogle() {
    GoogleSignInBridge.signOut?.invoke()
}
