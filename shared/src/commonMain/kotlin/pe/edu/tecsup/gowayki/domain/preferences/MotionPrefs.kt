package pe.edu.tecsup.gowayki.domain.preferences

data class MotionPrefs(
    val reduceMotion: Boolean = false,
) {
    fun toggle(): MotionPrefs = copy(reduceMotion = !reduceMotion)
}
