package pe.edu.tecsup.gowayki.domain.widget

enum class WidgetEmptyReason {
    NoFavorites,
    NoLocation,
    PackUnavailable,
}

fun emptyMessage(reason: WidgetEmptyReason, es: Boolean): String = when (reason) {
    WidgetEmptyReason.NoFavorites -> if (es) "Guarda un favorito para verlo aquí" else "Save a favorite to show it here"
    WidgetEmptyReason.NoLocation -> if (es) "Activa la ubicación para ver paraderos cerca" else "Enable location for nearby stops"
    WidgetEmptyReason.PackUnavailable -> if (es) "Paquete de Arequipa no disponible" else "Arequipa pack unavailable"
}
