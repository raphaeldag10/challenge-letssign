package br.com.fiap.letssign.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
// Cores da marca usadas nas telas de acesso e no painel do administrador.
val BrandBlue = Color(0xFF2563EB)
val BrandBlueSoft = Color(0xFFEAF1FF)

// Superfície única do app: o mesmo branco da tela de login em todas as telas (fundo, cards, barras e campos).
// Assim não há tons de branco diferentes entre telas.
val AppLightSurface = Color(0xFFFFFBFE)
val AppDarkSurface = Color(0xFF1C1B1F)

// Variantes da marca para o tema escuro (contraste suficiente sobre superfícies escuras).
val BrandBlueDark = Color(0xFF8AB4FF)
val BrandOnBlueDark = Color(0xFF0B2A6B)
val BrandBlueContainerDark = Color(0xFF1E3A8A)
val BrandOnBlueContainerDark = Color(0xFFDCE7FF)
val BrandOnBlueContainerLight = Color(0xFF0B2A6B)

/** Verdadeiro quando o esquema atual é escuro (fundo com luminância baixa). */
val androidx.compose.material3.ColorScheme.isDark: Boolean
    get() = background.luminance() < 0.5f
