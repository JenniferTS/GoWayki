package pe.edu.tecsup.gowayki.data.citypack

/** Cursor de solo lectura. Quien lo recibe lo cierra. */
interface DbCursor {
    fun next(): Boolean
    fun isNull(column: String): Boolean
    fun string(column: String): String
    fun double(column: String): Double
    fun long(column: String): Long
    fun close()
}

/** Conexión que solo ejecuta consultas enlazadas. No interpola texto del usuario. */
interface DbConnection {
    fun query(sql: String, args: List<Any?> = emptyList()): DbCursor
    fun close()
}

/** Abre el paquete ya copiado. Android lo implementa con SQLiteDatabase. */
interface SqliteDriver {
    fun open(): DbConnection
}

internal inline fun DbConnection.forEachRow(
    sql: String,
    args: List<Any?> = emptyList(),
    row: (DbCursor) -> Unit,
) {
    val cursor = query(sql, args)
    try {
        while (cursor.next()) row(cursor)
    } finally {
        cursor.close()
    }
}

internal fun DbCursor.longFlag(column: String): Boolean = long(column) == 1L
