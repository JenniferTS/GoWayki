package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.auth.SignedInUser

actual fun launchPlatformGoogleSignIn(
    onSuccess: (SignedInUser) -> Unit,
    onError: (String) -> Unit,
) {
    onError("Google Sign-In en iOS aún no está habilitado en esta build.")
}
