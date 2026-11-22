package pe.edu.tecsup.gowayki.platform

import pe.edu.tecsup.gowayki.domain.auth.SignedInUser

expect fun launchPlatformGoogleSignIn(
    onSuccess: (SignedInUser) -> Unit,
    onError: (String) -> Unit,
)
