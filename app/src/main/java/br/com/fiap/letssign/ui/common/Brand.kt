package br.com.fiap.letssign.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import br.com.fiap.letssign.R

/**
 * Marca LetsSign: ícone de documento com check e o nome do app.
 * O ícone e o texto usam a cor primária do tema, então acompanham o modo claro e o escuro.
 */
@Composable
fun BrandLogo(modifier: Modifier = Modifier, iconSize: Int = 36) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_letssign_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(iconSize.dp)
        )
        Text(
            text = "LetsSign",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** Iniciais de até duas palavras do nome, em maiúsculas. */
fun initialsOf(fullName: String): String =
    fullName.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

fun firstNameOf(fullName: String): String = fullName.trim().substringBefore(' ')

