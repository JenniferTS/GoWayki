package com.myapplication.widget

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object PinShortcutHelper {
    fun requestPinSearch(context: Context): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val intent = Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse("gowayki://search")).setPackage(context.packageName)
        val info = ShortcutInfoCompat.Builder(context, "pin_search")
            .setShortLabel(context.getString(com.myapplication.R.string.shortcut_search_short))
            .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_search))
            .setIntent(intent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }
}
