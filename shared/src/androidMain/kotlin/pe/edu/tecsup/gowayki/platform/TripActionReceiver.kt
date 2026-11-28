package pe.edu.tecsup.gowayki.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TripActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            TripOngoingNotifier.ACTION_FINISH -> {
                TripOngoingNotifier.cancel(context)
                TripActionBus.emit(TripAction.Finish)
            }
            TripOngoingNotifier.ACTION_MUTE -> {
                TripActionBus.emit(TripAction.Mute)
            }
        }
    }
}

enum class TripAction { Finish, Mute }

object TripActionBus {
    @Volatile private var listener: ((TripAction) -> Unit)? = null
    fun setListener(block: ((TripAction) -> Unit)?) { listener = block }
    fun emit(action: TripAction) { listener?.invoke(action) }
}
