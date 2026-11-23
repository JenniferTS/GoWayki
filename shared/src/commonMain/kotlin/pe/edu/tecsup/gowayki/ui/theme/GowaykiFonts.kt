package pe.edu.tecsup.gowayki.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Tipografías del manual de identidad gowayki go:
 * - Display: Righteous (única variante)
 * - Principal: Ubuntu (todas las variantes)
 * - Secundaria: Poppins (Regular / SemiBold / Bold / Black)
 */
expect object GowaykiFonts {
    val Display: FontFamily
    val Primary: FontFamily
    val Secondary: FontFamily
}
