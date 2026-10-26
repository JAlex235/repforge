package edu.umg.repforge.modelo

data class GrupoMuscular(val id: Long, val nombre: String)

data class Ejercicio(
    val id: Long,
    val nombre: String,
    val grupoPrincipal: GrupoMuscular,
    val grupoSecundario: GrupoMuscular?,
    val esPersonalizado: Boolean,
    val usaPesoCorporal: Boolean
)

