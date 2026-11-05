package br.com.fiap.letssign.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CpfTest {

    @Test
    fun acceptsValidCpfWithOrWithoutMask() {
        assertTrue(Cpf.isValid("52998224725"))
        assertEquals("52998224725", Cpf.validated("529.982.247-25"))
        assertEquals("12345678909", Cpf.validated("123.456.789-09"))
    }

    @Test
    fun rejectsWrongCheckDigitsAndRepeatedDigits() {
        assertFalse(Cpf.isValid("52998224726"))
        assertFalse(Cpf.isValid("11111111111"))
        assertFalse(Cpf.isValid("123"))
    }

    @Test
    fun validatedThrowsForInvalidCpf() {
        val error = assertThrows(IllegalArgumentException::class.java) { Cpf.validated("111.111.111-11") }
        assertEquals("CPF inválido", error.message)
    }

    @Test
    fun formatsWhileTyping() {
        assertEquals("", Cpf.format(""))
        assertEquals("529", Cpf.format("529"))
        assertEquals("529.982", Cpf.format("529982"))
        assertEquals("529.982.247-25", Cpf.format("52998224725"))
        // Ignora caracteres não numéricos e limita a 11 dígitos.
        assertEquals("529.982.247-25", Cpf.format("529.982.247-25999"))
    }
}
