package edu.umg.repforge

import android.os.Bundle
import edu.umg.repforge.databinding.ActivitySeccionPendienteBinding

/** Destinos de navegación preparados mientras se implementan las siguientes funciones. */
class SeccionPendienteActivity : PantallaBaseActivity() {
    override val seccionActual: Seccion
        get() = Seccion.entries.firstOrNull { it.name == intent.getStringExtra("seccion") }
            ?.takeIf { it != Seccion.INICIO && it != Seccion.PERFIL } ?: Seccion.ENTRENAR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySeccionPendienteBinding.inflate(layoutInflater)
        mostrarContenido(binding.root)
        binding.tvSeccion.setText(seccionActual.titulo)
        binding.iconoSeccion.setImageResource(seccionActual.icono)
        binding.tvDescripcionSeccion.setText(when (seccionActual) {
            Seccion.HISTORIAL -> R.string.pending_history
            Seccion.EJERCICIOS -> R.string.pending_exercises
            else -> R.string.pending_training
        })
    }
}
