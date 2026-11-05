package br.com.fiap.letssign.ui.common

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CpfVisualTransformationTest {

    private fun filter(digits: String) = CpfVisualTransformation.filter(AnnotatedString(digits))

    @Test
    fun formatsDigitsWithMask() {
        assertEquals("123.456.789-01", filter("12345678901").text.text)
        assertEquals("123.45", filter("12345").text.text)
    }

    @Test
    fun ignoresNonDigitsAndLimitsToElevenDigits() {
        assertEquals("123.456.789-01", filter("123.456.789-01999").text.text)
    }

    @Test
    fun cursorAfterDigitsSkipsMaskCharacters() {
        val mapping = filter("12345").offsetMapping
        // "123.45": cursor após 3 dígitos fica depois do "3"; após 4 dígitos, depois do "4" (pulando o ".").
        assertEquals(3, mapping.originalToTransformed(3))
        assertEquals(5, mapping.originalToTransformed(4))
        assertEquals(6, mapping.originalToTransformed(5))
    }

    @Test
    fun cursorInTransformedTextMapsBackToDigitCount() {
        val mapping = filter("12345").offsetMapping
        assertEquals(3, mapping.transformedToOriginal(3))
        assertEquals(4, mapping.transformedToOriginal(5))
        assertEquals(0, mapping.transformedToOriginal(0))
    }

    @Test
    fun cursorAtEndOfFullCpfIsAtEndOfMaskedText() {
        val result = filter("12345678901")
        assertEquals(14, result.offsetMapping.originalToTransformed(11))
        assertEquals(11, result.offsetMapping.transformedToOriginal(14))
    }
}
