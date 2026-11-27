package com.myapplication.widget

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace

object DynamicShortcuts {
    private const val ID_FAVORITE = "favorite_last"

    fun publishFavorite(context: Context, favorite: SavedPlace?) {
        if (favorite == null) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(ID_FAVORITE))
            return
        }
        val intent = WidgetDeepLink.searchIntent(context.packageName, null, favorite.name)
            .setAction(Intent.ACTION_VIEW)
        val shortcut = ShortcutInfoCompat.Builder(context, ID_FAVORITE)
            .setShortLabel(favorite.name.take(20))
            .setLongLabel("Ir a ${favorite.name}")
            .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_mylocation))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcut))
    }
}
