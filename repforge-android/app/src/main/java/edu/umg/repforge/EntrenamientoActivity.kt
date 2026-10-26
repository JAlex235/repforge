package edu.umg.repforge

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import edu.umg.repforge.databinding.ActivityEntrenamientoBinding
import edu.umg.repforge.modelo.Entrenamiento
import edu.umg.repforge.modelo.Ejercicio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class EntrenamientoActivity : PantallaBaseActivity() {
    override val seccionActual = Seccion.ENTRENAR
    private lateinit var binding: ActivityEntrenamientoBinding
    private val repo get() = (application as RepForgeApplication).entrenamientos
    private var sesion: Entrenamiento? = null
    private var ocupado = false
    private var dialogo: AlertDialog? = null
    private var tiempoBase = 0L
    private var relojBase = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntrenamientoBinding.inflate(layoutInflater)
        mostrarContenido(binding.root)
        binding.crearEntrenamiento.setOnClickListener {
            val nombre = binding.nombreEntrenamiento.text.toString().trim()
            if (nombre.isEmpty()) {
                binding.nombreEntrenamiento.error = getString(R.string.training_name_required)
            } else {
                WindowCompat.getInsetsController(window, binding.root).hide(WindowInsetsCompat.Type.ime())
                ejecutar { repo.iniciar(nombre) }
            }
        }
        binding.reintentarEntrenar.setOnClickListener { ejecutar { repo.activo() } }
        binding.agregarEjercicio.setOnClickListener { seleccionarEjercicio() }
        binding.descartarEntrenamiento.setOnClickListener {
            val id = sesion?.id ?: return@setOnClickListener
            if (!ocupado && dialogo == null) mostrarDialogo(AlertDialog.Builder(this)
                .setTitle(R.string.training_discard)
                .setMessage(R.string.training_discard_message)
                .setNegativeButton(R.string.cancel_date, null)
                .setPositiveButton(R.string.discard_changes) { _, _ ->
                    ejecutar { repo.descartar(id); null }
                }.create())
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    actualizarReloj()
                    delay(1000)
                }
            }
        }
        ejecutar { repo.activo() }
    }

    private fun controles(valor: Boolean) {
        ocupado = !valor
        binding.crearEntrenamiento.isEnabled = valor
        binding.agregarEjercicio.isEnabled = valor
        binding.descartarEntrenamiento.isEnabled = valor
        binding.nombreEntrenamiento.isEnabled = valor
    }

    private fun ejecutar(accion: () -> Entrenamiento?) {
        if (ocupado) return
        controles(false)
        binding.reintentarEntrenar.isVisible = false
        binding.estadoEntrenar.setText(R.string.training_working)
        lifecycleScope.launch {
            try {
                sesion = withContext(Dispatchers.IO) { accion() }
                mostrarSesion()
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("RepForge", "No se pudo actualizar el entrenamiento", error)
                binding.estadoEntrenar.setText(R.string.training_error)
                binding.reintentarEntrenar.isVisible = true
            } finally {
                controles(true)
            }
        }
    }

    private fun mostrarSesion() {
        val actual = sesion
        binding.nuevoEntrenamiento.isVisible = actual == null
        binding.sesionEntrenamiento.isVisible = actual != null
        binding.estadoEntrenar.setText(if (actual == null) R.string.training_intro else R.string.training_saved)
        if (actual == null) return
        binding.nombreSesion.text = actual.nombre
        tiempoBase = (System.currentTimeMillis() - actual.inicio).coerceAtLeast(0)
        relojBase = SystemClock.elapsedRealtime()
        actualizarReloj()
        binding.resumenBloques.text = if (actual.bloques.isEmpty()) getString(R.string.training_empty)
            else resources.getQuantityString(R.plurals.training_count, actual.bloques.size, actual.bloques.size)
        binding.bloquesEntrenamiento.removeAllViews()
        actual.bloques.forEach { bloque ->
            binding.bloquesEntrenamiento.addView(TextView(this).apply {
                setTextAppearance(R.style.TextAppearance_RepForge_Body)
                text = getString(R.string.training_block, bloque.orden, bloque.nombre)
                val espacio = (16 * resources.displayMetrics.density).toInt()
                setPadding(espacio, espacio, espacio, espacio)
            })
        }
    }

    private fun actualizarReloj() {
        if (sesion == null) return
        val segundos = (tiempoBase + SystemClock.elapsedRealtime() - relojBase) / 1000
        binding.tiempoSesion.text = String.format(Locale.ROOT, "%02d:%02d:%02d",
            segundos / 3600, segundos / 60 % 60, segundos % 60)
    }

    private fun seleccionarEjercicio() {
        val actual = sesion ?: return
        if (ocupado || dialogo != null) return
        controles(false)
        lifecycleScope.launch {
            try {
                val ejercicios = withContext(Dispatchers.IO) {
                    (application as RepForgeApplication).ejercicios.listar()
                }.filter { e -> actual.bloques.none { it.ejercicioId == e.id } }
                if (ejercicios.isEmpty()) {
                    binding.estadoEntrenar.setText(R.string.training_no_more)
                } else {
                    mostrarSelector(actual.id, ejercicios)
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("RepForge", "No se pudo cargar la selección", error)
                binding.estadoEntrenar.setText(R.string.catalog_error)
            } finally {
                controles(true)
            }
        }
    }

    private fun mostrarSelector(id: Long, ejercicios: List<Ejercicio>) {
        mostrarDialogo(AlertDialog.Builder(this)
            .setTitle(R.string.training_select)
            .setItems(ejercicios.map { it.nombre + " · " + it.grupoPrincipal.nombre }.toTypedArray()) { _, posicion ->
                ejecutar { repo.agregar(id, ejercicios[posicion].id) }
            }
            .setNegativeButton(R.string.cancel_date, null).create())
    }

    private fun mostrarDialogo(nuevo: AlertDialog) {
        if (isFinishing || isDestroyed) return
        dialogo = nuevo
        nuevo.setOnDismissListener { dialogo = null }
        nuevo.show()
        nuevo.window?.setBackgroundDrawableResource(R.drawable.tarjeta_inicio)
        for (boton in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE)) {
            nuevo.getButton(boton)?.setTextColor(ContextCompat.getColor(this, R.color.texto_principal))
        }
    }

    override fun onDestroy() {
        dialogo?.dismiss()
        super.onDestroy()
    }
}

