package pe.edu.tecsup.gowayki.domain.preferences

enum class ThemePreference(val storageValue: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemePreference =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}
