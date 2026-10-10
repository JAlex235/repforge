package edu.umg.repforge.servicio

import edu.umg.repforge.modelo.Usuario
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ValidadorUsuarioTest {
    private val validador = ValidadorUsuario()
    private val hoy = 20261009
    private val usuario = Usuario("Alex", 20000514, 75.5, 170, true)

    @ParameterizedTest
    @ValueSource(strings = ["José", "María Muñoz", "Ángela", "Jose\u0301"])
    fun `acepta nombres con tildes y enie sin modificar el texto`(nombre: String) {
        val perfil = usuario.copy(nombre = nombre)
        assertNull(validador.validar(perfil, hoy))
        assertEquals(nombre, perfil.nombre)
    }

    @Test
    fun `acepta un perfil nuevo o guardado y ambos valores de sexo`() {
        assertNull(validador.validar(usuario, hoy))
        assertNull(validador.validar(usuario.copy(idUsuario = 15L, esMasculino = false), hoy))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "\t\n"])
    fun `rechaza nombres sin contenido`(nombre: String) {
        assertEquals("El nombre no puede estar vacío.", validador.validar(usuario.copy(nombre = nombre), hoy))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -20000514, 20001301, 20000010, 20000100, 20000431, 20010229, 19000229, 100000101])
    fun `rechaza fechas imposibles`(fecha: Int) {
        assertEquals("La fecha de nacimiento no es válida.", validador.validar(usuario.copy(fechaNacimiento = fecha), hoy))
    }

    @ParameterizedTest
    @ValueSource(ints = [20000229, 20240229, 20000430, 20261009])
    fun `acepta fechas reales incluidos bisiestos y fecha actual`(fecha: Int) {
        assertNull(validador.validar(usuario.copy(fechaNacimiento = fecha), hoy))
    }

    @Test
    fun `rechaza nacimiento futuro`() {
        assertEquals("La fecha de nacimiento no puede ser futura.",
            validador.validar(usuario.copy(fechaNacimiento = 20261010), hoy))
    }

    @ParameterizedTest
    @ValueSource(doubles = [0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY])
    fun `rechaza pesos no positivos o no finitos`(peso: Double) {
        assertEquals("El peso debe ser un número mayor que cero.", validador.validar(usuario.copy(pesoKg = peso), hoy))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `rechaza alturas no positivas`(altura: Int) {
        assertEquals("La altura debe ser mayor que cero.", validador.validar(usuario.copy(alturaCm = altura), hoy))
    }

    @Test
    fun `informa primero el error del nombre cuando hay varios`() {
        assertEquals("El nombre no puede estar vacío.",
            validador.validar(usuario.copy(nombre = "", pesoKg = -1.0), hoy))
    }

    @Test
    fun `detecta una fecha de referencia mal configurada`() {
        assertThrows(IllegalArgumentException::class.java) { validador.validar(usuario, 20260230) }
    }

    @Test
    fun `puede obtener la fecha actual del sistema`() {
        assertNull(validador.validar(usuario))
    }
}
