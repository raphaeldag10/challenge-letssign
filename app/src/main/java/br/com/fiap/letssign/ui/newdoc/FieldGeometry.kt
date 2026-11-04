package br.com.fiap.letssign.ui.newdoc

/**
 * Regras geométricas dos campos, em coordenadas normalizadas (0 a 1) da página.
 * Funções puras para permitir teste unitário sem Android.
 */
object FieldGeometry {
    const val MIN_WIDTH = 0.05f
    const val MIN_HEIGHT = 0.03f
    private const val EPS = 1e-4f

    /** Nova posição (x, y) após mover em (dx, dy), sem sair da página. */
    fun move(x: Float, y: Float, w: Float, h: Float, dx: Float, dy: Float): Pair<Float, Float> =
        clamp(x + dx, 0f, 1f - w) to clamp(y + dy, 0f, 1f - h)

    /** Novo tamanho (w, h) após redimensionar em (dw, dh), respeitando tamanho mínimo e borda da página. */
    fun resize(x: Float, y: Float, w: Float, h: Float, dw: Float, dh: Float): Pair<Float, Float> =
        clamp(w + dw, MIN_WIDTH, 1f - x) to clamp(h + dh, MIN_HEIGHT, 1f - y)

    /** Dois retângulos se sobrepõem? Encostar nas bordas não conta como sobreposição. */
    fun overlaps(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float
    ): Boolean = ax < bx + bw - EPS && bx < ax + aw - EPS && ay < by + bh - EPS && by < ay + ah - EPS

    private fun clamp(value: Float, low: Float, high: Float): Float =
        value.coerceIn(low, maxOf(low, high))
}
