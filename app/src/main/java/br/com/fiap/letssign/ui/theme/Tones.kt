package br.com.fiap.letssign.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Tom semântico de uma etiqueta de status. */
enum class Tone { INFO, WARNING, SUCCESS }

/**
 * Par (fundo, texto) de uma etiqueta de status. Cada par é legível sobre o próprio fundo
 * no tema claro e no escuro.
 */
@Composable
@ReadOnlyComposable
fun toneColors(tone: Tone): Pair<Color, Color> {
    val dark = MaterialTheme.colorScheme.isDark
    return when (tone) {
        Tone.INFO -> if (dark) BrandPurpleContainerDark to BrandOnPurpleContainerDark else BrandPurpleContainer to BrandOnPurpleContainer
        Tone.WARNING -> if (dark) Color(0xFF4A3800) to Color(0xFFFFD980) else Color(0xFFFFF3C4) to Color(0xFF8A5A00)
        Tone.SUCCESS -> if (dark) Color(0xFF14432A) to Color(0xFF9EF0BC) else Color(0xFFDCFCE7) to Color(0xFF15803D)
    }
}
