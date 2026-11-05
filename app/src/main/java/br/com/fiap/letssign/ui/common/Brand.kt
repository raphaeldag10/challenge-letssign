package br.com.fiap.letssign.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.letssign.R
import br.com.fiap.letssign.ui.theme.BrandBlue

/** Marca LetsSign: ícone de documento com check e o nome do app. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier, iconSize: Int = 36) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_letssign_logo),
            contentDescription = null,
            modifier = Modifier.size(iconSize.dp)
        )
        Text(
            text = "LetsSign",
            color = BrandBlue,
            fontWeight = FontWeight.Bold,
            fontSize = (iconSize * 0.6).sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** Iniciais de até duas palavras do nome, em maiúsculas. */
fun initialsOf(fullName: String): String =
    fullName.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

fun firstNameOf(fullName: String): String = fullName.trim().substringBefore(' ')

