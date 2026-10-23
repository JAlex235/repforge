package edu.umg.repforge

enum class Seccion(val vistaId: Int, val titulo: Int, val icono: Int) {
    INICIO(R.id.navInicio, R.string.home_title, R.drawable.ic_nav_inicio),
    ENTRENAR(R.id.navEntrenar, R.string.nav_train, R.drawable.ic_nav_entrenar),
    HISTORIAL(R.id.navHistorial, R.string.nav_history, R.drawable.ic_nav_historial),
    EJERCICIOS(R.id.navEjercicios, R.string.nav_exercises, R.drawable.ic_nav_ejercicios),
    PERFIL(R.id.navPerfil, R.string.nav_profile, R.drawable.ic_nav_perfil)
}
