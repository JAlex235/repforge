package edu.umg.repforge.servicio

/** El peso representa la carga externa, incluso en ejercicios con peso corporal. */
object ValidadorSerie {
    fun validar(pesoKg: Double, repeticiones: Int, usaPesoCorporal: Boolean) {
        require(pesoKg.isFinite() && pesoKg >= 0) { "Ingresa un peso válido, mayor o igual a cero." }
        require(usaPesoCorporal || pesoKg > 0) { "Este ejercicio requiere un peso mayor a cero." }
        require(repeticiones > 0) { "Ingresa al menos una repetición." }
    }
}
