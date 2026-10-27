package edu.umg.repforge.modelo

/** Fechas expresadas en milisegundos desde Unix epoch. */
data class Entrenamiento(
    val id: Long,
    val nombre: String,
    val inicio: Long,
    val bloques: List<BloqueEjercicio>,
    val fin: Long? = null
)

data class BloqueEjercicio(
    val id: Long, val ejercicioId: Long, val nombre: String, val orden: Int,
    val usaPesoCorporal: Boolean = false, val series: List<Serie> = emptyList()
)

data class Serie(val id: Long, val numero: Int, val pesoKg: Double, val repeticiones: Int)

/** Resumen para la lista: evita cargar todas las series del historial en memoria. */
data class ResumenEntrenamiento(
    val id: Long, val nombre: String, val inicio: Long, val fin: Long,
    val cantidadEjercicios: Int, val cantidadSeries: Int
)

