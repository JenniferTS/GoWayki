package pe.edu.tecsup.gowayki.domain.changelog

data class ChangelogEntry(val version: String, val bullets: List<String>)

val APP_CHANGELOG = listOf(
    ChangelogEntry(
        "1.6.0",
        listOf(
            "Widget de próximo viaje y paraderos cerca",
            "Avisos al acercarte al paradero de bajada",
            "Mapa oscuro y buscador de líneas",
            "Favoritos con sync y tema claro/oscuro",
        ),
    ),
    ChangelogEntry(
        "1.5.6",
        listOf("Recorrido con motion Compose y JourneySheet"),
    ),
)
