package br.com.fiap.letssign.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import br.com.fiap.letssign.data.file.FieldPlacement
import kotlin.math.roundToInt

/** Campos em modo leitura. O participante atual fica em destaque; os demais, esmaecidos. */
@Composable
fun BoxScope.ReadOnlyFields(fields: List<FieldPlacement>, size: IntSize, highlightUserId: Long?) {
    val density = LocalDensity.current
    fields.forEach { f ->
        val mine = f.userId == highlightUserId
        val color = signatoryColor(f.userId)
        Box(
            Modifier
                .offset { IntOffset((f.x * size.width).roundToInt(), (f.y * size.height).roundToInt()) }
                .size(
                    with(density) {
                        DpSize((f.width * size.width).toDp(), (f.height * size.height).toDp())
                    }
                )
                .border(if (mine) 2.dp else 1.dp, color.copy(alpha = if (mine) 1f else 0.35f))
                .background(color.copy(alpha = if (mine) 0.18f else 0.05f))
        )
    }
}
