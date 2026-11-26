package pe.edu.tecsup.gowayki.domain.community

/** Borrador de foto opcional (comprimida) para un reporte. */
data class ReportPhotoDraft(
    val bytes: ByteArray,
    val mime: String = "image/jpeg",
) {
    init {
        require(bytes.size <= MAX_BYTES) { "foto supera $MAX_BYTES bytes" }
        require(mime in ALLOWED) { "mime no permitido" }
    }

    companion object {
        const val MAX_BYTES: Int = 400_000
        val ALLOWED = setOf("image/jpeg", "image/png", "image/webp")
    }
}
