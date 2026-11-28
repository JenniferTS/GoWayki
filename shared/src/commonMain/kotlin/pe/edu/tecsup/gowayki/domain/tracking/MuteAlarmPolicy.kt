package pe.edu.tecsup.gowayki.domain.tracking

import pe.edu.tecsup.gowayki.platform.AlarmMode

/** Silenciar alarma del viaje actual sin cambiar la preferencia global. */
data class MuteAlarmPolicy(
    val globalMode: AlarmMode,
    val mutedForTrip: Boolean = false,
) {
    fun effective(): AlarmMode = if (mutedForTrip) AlarmMode.Off else globalMode
    fun mute(): MuteAlarmPolicy = copy(mutedForTrip = true)
    fun resetForNewTrip(): MuteAlarmPolicy = copy(mutedForTrip = false)
}
