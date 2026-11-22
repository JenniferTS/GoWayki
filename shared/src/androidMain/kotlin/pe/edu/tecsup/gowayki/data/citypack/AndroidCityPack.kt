package pe.edu.tecsup.gowayki.data.citypack

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import pe.edu.tecsup.gowayki.domain.repositories.CityPackRepository

const val AREQUIPA_PACK_ASSET: String = "city/arequipa/pack.sqlite"
const val AREQUIPA_MANIFEST_ASSET: String = "city/arequipa/manifest.json"

private const val PACK_DIRECTORY: String = "city/arequipa"
private const val PACK_FILE: String = "pack.sqlite"
private const val STAMP_FILE: String = "pack.sha256"

/**
 * Copia `assets/city/arequipa/pack.sqlite` a almacenamiento interno la primera vez
 * y cuando cambia el SHA-256 del manifiesto. Luego lo abre en solo lectura.
 *
 * No llamarlo en el hilo de interfaz: el archivo pesa unos 16 MB y
 * [CityPackRepository.plannerNetwork] lee toda la red.
 */
fun Context.openArequipaCityPack(): SqliteCityPackRepository {
    val app = applicationContext
    val info = cityPackInfoFromManifest(app.assets.open(AREQUIPA_MANIFEST_ASSET).bufferedReader().use { it.readText() })
    val database = installPackIfNeeded(app, info.sha256)
    val sqlite = SQLiteDatabase.openDatabase(
        database.absolutePath,
        null,
        SQLiteDatabase.OPEN_READONLY,
    )
    return SqliteCityPackRepository(AndroidSqliteConnection(sqlite), info)
}

private fun installPackIfNeeded(context: Context, sha256: String): File {
    val directory = File(context.filesDir, PACK_DIRECTORY)
    directory.mkdirs()
    val destination = File(directory, PACK_FILE)
    val stamp = File(directory, STAMP_FILE)
    if (destination.isFile && destination.length() > 0L && stamp.isFile && stamp.readText() == sha256) {
        return destination
    }
    val temporary = File(directory, "$PACK_FILE.tmp")
    if (temporary.exists() && !temporary.delete()) {
        error("No se pudo preparar la copia del paquete.")
    }
    context.assets.open(AREQUIPA_PACK_ASSET).use { input ->
        temporary.outputStream().use { output -> input.copyTo(output) }
    }
    if (destination.exists() && !destination.delete()) {
        error("No se pudo reemplazar pack.sqlite.")
    }
    if (!temporary.renameTo(destination)) {
        error("No se pudo instalar pack.sqlite.")
    }
    stamp.writeText(sha256)
    return destination
}

private class AndroidSqliteConnection(
    private val database: SQLiteDatabase,
) : DbConnection {
    private var closed = false

    override fun query(sql: String, args: List<Any?>): DbCursor {
        if (closed) throw IllegalStateException("El paquete ya está cerrado.")
        val bound = if (args.isEmpty()) {
            null
        } else {
            args.map { it?.toString() ?: "" }.toTypedArray()
        }
        return AndroidSqliteCursor(database.rawQuery(sql, bound))
    }

    override fun close() {
        if (closed) return
        closed = true
        database.close()
    }
}

private class AndroidSqliteCursor(
    private val cursor: Cursor,
) : DbCursor {
    override fun next(): Boolean = cursor.moveToNext()

    override fun isNull(column: String): Boolean = cursor.isNull(index(column))

    override fun string(column: String): String = cursor.getString(index(column))

    override fun double(column: String): Double = cursor.getDouble(index(column))

    override fun long(column: String): Long = cursor.getLong(index(column))

    override fun close() {
        cursor.close()
    }

    private fun index(column: String): Int = cursor.getColumnIndexOrThrow(column)
}
