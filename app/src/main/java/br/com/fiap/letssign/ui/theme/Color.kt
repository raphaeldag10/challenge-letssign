package br.com.fiap.letssign.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Cores base do tema padrão do Material (usadas pelos componentes que não são da marca).
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Superfície única do app: o mesmo branco da tela de login em todas as telas (fundo, cards, barras e campos).
val AppLightSurface = Color(0xFFFFFBFE)
val AppDarkSurface = Color(0xFF1C1B1F)

// Paleta roxa da marca. Os pares foram escolhidos para ter contraste acima de 4,5:1 com o texto.
// Claro: botão roxo com texto branco; container claro com texto roxo escuro.
val BrandPurple = Color(0xFF6D3FD1)
val BrandPurpleContainer = Color(0xFFEFE9FF)
val BrandOnPurpleContainer = Color(0xFF2A1160)

// Escuro: botão lilás com texto roxo muito escuro; container roxo profundo com texto lilás claro.
val BrandPurpleDark = Color(0xFFA98BFF)
val BrandOnPurpleDark = Color(0xFF1F0A52)
val BrandPurpleContainerDark = Color(0xFF3B1F7A)
val BrandOnPurpleContainerDark = Color(0xFFE7DDFF)

/** Verdadeiro quando o esquema atual é escuro (fundo com luminância baixa). */
val ColorScheme.isDark: Boolean
    get() = background.luminance() < 0.5f
