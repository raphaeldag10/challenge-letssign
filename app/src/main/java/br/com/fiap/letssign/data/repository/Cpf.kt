package br.com.fiap.letssign.data.repository

/** Regras de CPF. O valor armazenado é sempre só com dígitos. */
object Cpf {
    const val LENGTH = 11

    fun digitsOf(raw: String): String = raw.filter(Char::isDigit)

    /** Máscara 000.000.000-00 aplicada enquanto o usuário digita. */
    fun format(raw: String): String {
        val digits = digitsOf(raw).take(LENGTH)
        return buildString {
            digits.forEachIndexed { i, c ->
                if (i == 3 || i == 6) append('.')
                if (i == 9) append('-')
                append(c)
            }
        }
    }

    /** Devolve os dígitos do CPF; lança IllegalArgumentException se o CPF for inválido. */
    fun validated(raw: String): String {
        val digits = digitsOf(raw)
        require(isValid(digits)) { "CPF inválido" }
        return digits
    }

    fun isValid(digits: String): Boolean {
        if (digits.length != LENGTH || digits.all { it == digits[0] }) return false
        fun checkDigit(length: Int): Int {
            val sum = (0 until length).sumOf { (digits[it] - '0') * (length + 1 - it) }
            val rest = (sum * 10) % 11
            return if (rest == 10) 0 else rest
        }
        return checkDigit(9) == digits[9] - '0' && checkDigit(10) == digits[10] - '0'
    }
}
