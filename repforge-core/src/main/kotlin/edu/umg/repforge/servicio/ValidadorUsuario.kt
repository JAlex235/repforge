package edu.umg.repforge.servicio

import edu.umg.repforge.modelo.Usuario
import java.util.Calendar

class ValidadorUsuario {
    /** Devuelve el primer error, o null si el perfil es válido, como en la guía. */
    fun validar(usuario: Usuario, fechaActual: Int = hoy()): String? {
        require(esFechaValida(fechaActual)) { "La fecha actual debe ser válida y usar YYYYMMDD." }

        if (usuario.nombre.isBlank()) return "El nombre no puede estar vacío."
        if (!esFechaValida(usuario.fechaNacimiento)) {
            return "La fecha de nacimiento no es válida."
        }
        if (usuario.fechaNacimiento > fechaActual) {
            return "La fecha de nacimiento no puede ser futura."
        }
        if (!usuario.pesoKg.isFinite() || usuario.pesoKg <= 0) {
            return "El peso debe ser un número mayor que cero."
        }
        if (usuario.alturaCm <= 0) return "La altura debe ser mayor que cero."
        return null
    }

    // Calendario gregoriano, sin java.time para no requerir API 26 en Android.
    private fun esFechaValida(fecha: Int): Boolean {
        val anio = fecha / 10000
        val mes = (fecha / 100) % 100
        val dia = fecha % 100
        if (anio !in 1..9999 || mes !in 1..12) return false
        val bisiesto = anio % 400 == 0 || (anio % 4 == 0 && anio % 100 != 0)
        val diasDelMes = when (mes) {
            2 -> if (bisiesto) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return dia in 1..diasDelMes
    }

    private fun hoy(): Int {
        val calendario = Calendar.getInstance()
        return calendario.get(Calendar.YEAR) * 10000 +
            (calendario.get(Calendar.MONTH) + 1) * 100 +
            calendario.get(Calendar.DAY_OF_MONTH)
    }
}
