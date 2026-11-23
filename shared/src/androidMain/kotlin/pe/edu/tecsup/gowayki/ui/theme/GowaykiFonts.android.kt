package pe.edu.tecsup.gowayki.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.myapplication.common.R

actual object GowaykiFonts {
    actual val Display: FontFamily = FontFamily(
        Font(R.font.righteous_regular, FontWeight.Normal),
    )

    actual val Primary: FontFamily = FontFamily(
        Font(R.font.ubuntu_light, FontWeight.Light),
        Font(R.font.ubuntu_regular, FontWeight.Normal),
        Font(R.font.ubuntu_medium, FontWeight.Medium),
        Font(R.font.ubuntu_bold, FontWeight.Bold),
    )

    actual val Secondary: FontFamily = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
        Font(R.font.poppins_black, FontWeight.Black),
    )
}
