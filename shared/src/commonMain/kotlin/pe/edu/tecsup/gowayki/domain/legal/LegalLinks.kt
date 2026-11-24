package pe.edu.tecsup.gowayki.domain.legal

/**
 * Enlaces legales configurables (Flutter: `legal_links.dart`, `PRIVACY_URL`/`TERMS_URL`).
 * Nulos mientras no existan las páginas: la UI oculta esos enlaces.
 */
data class LegalLinks(
    val privacy: String? = null,
    val terms: String? = null,
) {
    companion object {
        /** Atribución de datos del mapa (ODbL) y de las teselas. */
        const val OSM_COPYRIGHT = "https://www.openstreetmap.org/copyright"
        const val OPENMAPTILES = "https://openmaptiles.org/"

        private val WEB_URL = Regex("^https?://[^/\\s?#]+[^\\s]*$", RegexOption.IGNORE_CASE)

        /** Solo URLs http(s) con host; vacío o inválido → null. */
        fun parse(value: String?): String? = value?.trim()?.takeIf { WEB_URL.matches(it) }
    }
}
