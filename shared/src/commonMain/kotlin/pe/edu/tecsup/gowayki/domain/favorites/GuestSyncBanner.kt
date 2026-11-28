package pe.edu.tecsup.gowayki.domain.favorites

fun shouldShowGuestSyncBanner(isGuest: Boolean, hasLocalFavorites: Boolean): Boolean =
    isGuest && hasLocalFavorites
