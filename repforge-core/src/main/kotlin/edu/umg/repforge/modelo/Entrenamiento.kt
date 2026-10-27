package edu.umg.repforge.modelo

/** Fechas expresadas en milisegundos desde Unix epoch. */
data class Entrenamiento(
    val id: Long,
    val nombre: String,
    val inicio: Long,
    val bloques: List<BloqueEjercicio>
)

data class BloqueEjercicio(
    val id: Long, val ejercicioId: Long, val nombre: String, val orden: Int,
    val usaPesoCorporal: Boolean = false, val series: List<Serie> = emptyList()
)

data class Serie(val id: Long, val numero: Int, val pesoKg: Double, val repeticiones: Int)

