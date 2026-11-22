package pe.edu.tecsup.gowayki.data.map

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Copia el paquete offline (mbtiles + style + fonts) a filesDir y reescribe
 * `{MAP_DIR}` hacia la ruta absoluta local para MapLibre (`mbtiles://` / `file://`).
 */
class OfflineMapInstaller(private val context: Context) {
    fun installStyle(dark: Boolean = false): File {
        val root = File(context.filesDir, "map/arequipa").also { it.mkdirs() }
        val manifestText = context.assets.open("map/manifest.json").bufferedReader().use { it.readText() }
        val meta = JSONObject(manifestText)
        require(meta.getInt("schema_version") == 1) { "Mapa incompatible" }
        require(meta.getString("city_code") == "arequipa") { "Mapa incompatible" }
        val files = meta.getJSONObject("files")
        val keys = files.keys().asSequence().toSet()
        require(keys.containsAll(setOf("arequipa.mbtiles", "style.json", "style-dark.json"))) {
            "Mapa incompleto"
        }
        copyAssetTree("map", root)
        val styleName = if (dark) "style-dark.json" else "style.json"
        val absolute = root.absolutePath.replace('\\', '/')
        val patched = File(root, styleName).readText().replace("{MAP_DIR}", absolute)
        val out = File(root, if (dark) "style-dark.local.json" else "style.local.json")
        out.writeText(patched)
        return out
    }

    private fun copyAssetTree(assetPath: String, dest: File) {
        val children = context.assets.list(assetPath)
        if (children.isNullOrEmpty()) {
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            return
        }
        dest.mkdirs()
        for (name in children) {
            copyAssetTree("$assetPath/$name", File(dest, name))
        }
    }
}
