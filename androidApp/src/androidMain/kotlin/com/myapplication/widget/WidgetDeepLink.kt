package com.myapplication.widget

import android.content.Intent
import android.net.Uri

object WidgetDeepLink {
    const val HOST_SEARCH = "search"
    const val PARAM_ORIGIN = "origin"
    const val PARAM_DEST = "dest"

    fun searchIntent(packageName: String, origin: String?, dest: String?): Intent {
        val uri = Uri.Builder()
            .scheme("gowayki")
            .authority(HOST_SEARCH)
            .appendQueryParameter(PARAM_ORIGIN, origin.orEmpty())
            .appendQueryParameter(PARAM_DEST, dest.orEmpty())
            .build()
        return Intent(Intent.ACTION_VIEW, uri).setPackage(packageName)
    }
}
