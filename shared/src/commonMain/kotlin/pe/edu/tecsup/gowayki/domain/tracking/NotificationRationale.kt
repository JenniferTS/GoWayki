package pe.edu.tecsup.gowayki.domain.tracking

/** Cuándo mostrar el copy de justificación antes del permiso del sistema. */
fun shouldShowNotificationRationale(
    permissionGranted: Boolean,
    userDismissedBanner: Boolean,
    tripAboutToStart: Boolean,
): Boolean = !permissionGranted && !userDismissedBanner && tripAboutToStart
