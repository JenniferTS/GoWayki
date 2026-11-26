package pe.edu.tecsup.gowayki.domain.tracking

enum class TripNoticeState { Idle, Ongoing, Cancelled, Finished }

fun nextNoticeState(current: TripNoticeState, event: String): TripNoticeState = when (event) {
    "start" -> TripNoticeState.Ongoing
    "cancel" -> TripNoticeState.Cancelled
    "finish" -> TripNoticeState.Finished
    "clear" -> TripNoticeState.Idle
    else -> current
}

fun shouldClearNotification(state: TripNoticeState): Boolean =
    state == TripNoticeState.Cancelled || state == TripNoticeState.Finished || state == TripNoticeState.Idle
