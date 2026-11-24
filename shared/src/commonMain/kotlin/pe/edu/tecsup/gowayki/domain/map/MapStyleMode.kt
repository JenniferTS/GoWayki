package pe.edu.tecsup.gowayki.domain.map

enum class MapStyleMode(val storageValue: String) {
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        fun fromStorage(value: String?): MapStyleMode =
            entries.firstOrNull { it.storageValue == value } ?: Light
    }
}
