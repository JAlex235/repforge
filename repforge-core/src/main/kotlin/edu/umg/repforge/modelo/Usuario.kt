package edu.umg.repforge.modelo

/** Datos del perfil. La validación se realiza mediante ValidadorUsuario. */
data class Usuario(
    val nombre: String,
    // Mismo formato que SQLite: por ejemplo, 20000514 representa el 14/05/2000.
    val fechaNacimiento: Int,
    val pesoKg: Double,
    val alturaCm: Int,
    // Al guardar en SQLite: true = 1, false = 0.
    val esMasculino: Boolean,
    // null significa que SQLite todavía no ha asignado un identificador.
    val idUsuario: Long? = null
)
