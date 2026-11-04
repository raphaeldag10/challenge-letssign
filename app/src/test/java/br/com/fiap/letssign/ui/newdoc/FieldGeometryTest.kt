package br.com.fiap.letssign.ui.newdoc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldGeometryTest {

    @Test
    fun moveKeepsFieldInsideThePage() {
        val (x, y) = FieldGeometry.move(x = 0.1f, y = 0.1f, w = 0.3f, h = 0.1f, dx = -0.5f, dy = 2f)
        assertEquals(0f, x, 1e-6f)
        assertEquals(0.9f, y, 1e-6f)
    }

    @Test
    fun moveAppliesDeltaWhenInside() {
        val (x, y) = FieldGeometry.move(x = 0.2f, y = 0.2f, w = 0.3f, h = 0.1f, dx = 0.1f, dy = -0.05f)
        assertEquals(0.3f, x, 1e-6f)
        assertEquals(0.15f, y, 1e-6f)
    }

    @Test
    fun resizeRespectsMinimumSize() {
        val (w, h) = FieldGeometry.resize(x = 0.2f, y = 0.2f, w = 0.3f, h = 0.1f, dw = -1f, dh = -1f)
        assertEquals(FieldGeometry.MIN_WIDTH, w, 1e-6f)
        assertEquals(FieldGeometry.MIN_HEIGHT, h, 1e-6f)
    }

    @Test
    fun resizeStopsAtThePageEdge() {
        val (w, h) = FieldGeometry.resize(x = 0.8f, y = 0.9f, w = 0.1f, h = 0.05f, dw = 1f, dh = 1f)
        assertEquals(0.2f, w, 1e-6f)
        assertEquals(0.1f, h, 1e-6f)
    }

    @Test
    fun overlapDetectsIntersection() {
        assertTrue(FieldGeometry.overlaps(0.1f, 0.1f, 0.3f, 0.1f, 0.2f, 0.15f, 0.3f, 0.1f))
    }

    @Test
    fun touchingEdgesAreNotAnOverlap() {
        assertFalse(FieldGeometry.overlaps(0.1f, 0.1f, 0.3f, 0.1f, 0.4f, 0.1f, 0.3f, 0.1f))
    }

    @Test
    fun separatedFieldsDoNotOverlap() {
        assertFalse(FieldGeometry.overlaps(0.1f, 0.1f, 0.2f, 0.1f, 0.1f, 0.5f, 0.2f, 0.1f))
    }
}
