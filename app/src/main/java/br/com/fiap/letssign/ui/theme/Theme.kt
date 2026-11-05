package br.com.fiap.letssign.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Esquemas com a cor da marca em ambos os temas. Assim, os elementos que usam o azul
// do LetsSign ficam legíveis no modo claro e no escuro.
private val DarkColorScheme = darkColorScheme(
    background = AppDarkSurface,
    surface = AppDarkSurface,
    surfaceContainerLowest = AppDarkSurface,
    surfaceContainerLow = AppDarkSurface,
    surfaceContainer = AppDarkSurface,
    surfaceContainerHigh = AppDarkSurface,
    surfaceContainerHighest = AppDarkSurface,
    surfaceBright = AppDarkSurface,
    // Sem sobreposição colorida na elevação: todas as superfícies ficam com o mesmo tom.
    surfaceTint = Color.Transparent,
    primary = BrandBlueDark,
    onPrimary = BrandOnBlueDark,
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = BrandOnBlueContainerDark,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    background = AppLightSurface,
    surface = AppLightSurface,
    surfaceContainerLowest = AppLightSurface,
    surfaceContainerLow = AppLightSurface,
    surfaceContainer = AppLightSurface,
    surfaceContainerHigh = AppLightSurface,
    surfaceContainerHighest = AppLightSurface,
    surfaceBright = AppLightSurface,
    // Sem sobreposição colorida na elevação: todas as superfícies ficam com o mesmo tom.
    surfaceTint = Color.Transparent,
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = BrandOnBlueContainerLight,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun LetssignTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desativado por padrão: a cor dinâmica do Android 12+ substituiria a identidade visual do LetsSign.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        // Pinta o fundo da raiz: sem isso, a janela (tema claro do Android) aparece branca
        // por trás de telas sem Scaffold, como o login, e não acompanha o modo escuro.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}