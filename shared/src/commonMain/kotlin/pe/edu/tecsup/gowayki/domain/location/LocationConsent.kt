package pe.edu.tecsup.gowayki.domain.location

enum class LocationAccessStep {
    /** Ya hay permiso: usar la ubicación. */
    Granted,
    /** Uso pasivo (arranque, abrir una pantalla): sin permiso no se pide nada. */
    Skip,
    /** Primera vez que el usuario lo pide: aviso destacado antes del diálogo del sistema. */
    ShowDisclosure,
    /** El usuario ya aceptó el aviso: se puede abrir el diálogo del sistema. */
    RequestSystem,
}

/**
 * Política de Play (divulgación destacada): el diálogo del sistema solo se abre tras una acción
 * explícita del usuario y después de que vio y aceptó el aviso de ubicación.
 */
object LocationConsent {
    fun next(
        hasPermission: Boolean,
        userInitiated: Boolean,
        disclosureAccepted: Boolean,
    ): LocationAccessStep = when {
        hasPermission -> LocationAccessStep.Granted
        !userInitiated -> LocationAccessStep.Skip
        disclosureAccepted -> LocationAccessStep.RequestSystem
        else -> LocationAccessStep.ShowDisclosure
    }
}
