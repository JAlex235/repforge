package edu.umg.repforge.servicio

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ValidadorSerieTest {
    @Test
    fun `cero representa peso corporal sin carga adicional`() {
        assertDoesNotThrow { ValidadorSerie.validar(0.0, 12, true) }
        assertDoesNotThrow { ValidadorSerie.validar(2.5, 1, true) }
        assertDoesNotThrow { ValidadorSerie.validar(20.5, 10, false) }
        assertThrows(IllegalArgumentException::class.java) { ValidadorSerie.validar(0.0, 10, false) }
    }

    @Test
    fun `rechaza pesos invalidos y repeticiones no positivas`() {
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { peso ->
            assertThrows(IllegalArgumentException::class.java) { ValidadorSerie.validar(peso, 10, true) }
        }
        listOf(0, -1).forEach { repeticiones ->
            assertThrows(IllegalArgumentException::class.java) { ValidadorSerie.validar(10.0, repeticiones, false) }
        }
    }
}
