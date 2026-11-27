package com.myapplication.widget

import android.content.Context
import pe.edu.tecsup.gowayki.domain.widget.NextTripWidgetModel

object NextTripWidgetStore {
    private const val PREFS = "gowayki_widget"
    private const val KEY_ORIGIN = "origin"
    private const val KEY_DEST = "dest"
    private const val KEY_ETA = "eta"

    fun save(context: Context, model: NextTripWidgetModel?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (model == null) {
            prefs.edit().clear().apply()
            return
        }
        prefs.edit()
            .putString(KEY_ORIGIN, model.originName)
            .putString(KEY_DEST, model.destinationName)
            .putString(KEY_ETA, model.etaLabel)
            .apply()
    }

    fun read(context: Context): NextTripWidgetModel? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val origin = prefs.getString(KEY_ORIGIN, null) ?: return null
        val dest = prefs.getString(KEY_DEST, null) ?: return null
        return NextTripWidgetModel(origin, dest, prefs.getString(KEY_ETA, null))
    }
}
