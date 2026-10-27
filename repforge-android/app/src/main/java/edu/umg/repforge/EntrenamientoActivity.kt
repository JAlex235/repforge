package edu.umg.repforge

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView as TextView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import edu.umg.repforge.databinding.ActivityEntrenamientoBinding
import edu.umg.repforge.databinding.DialogoSerieBinding
import edu.umg.repforge.modelo.BloqueEjercicio
import edu.umg.repforge.modelo.Serie
import edu.umg.repforge.servicio.ValidadorSerie
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
    private var editor: DialogoSerieBinding? = null
    private var bloqueEditado = 0L
    private var serieEditada: Long? = null
    private var borrador: Bundle? = null
    private var finalizado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntrenamientoBinding.inflate(layoutInflater)
        mostrarContenido(binding.root)
        borrador = savedInstanceState?.getBundle("editorSerie")
        finalizado = savedInstanceState?.getBoolean("finalizado") ?: false
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
        binding.finalizarEntrenamiento.setOnClickListener {
            val actual = sesion ?: return@setOnClickListener
            if (ocupado || dialogo != null) return@setOnClickListener
            if (actual.bloques.none { it.series.isNotEmpty() }) {
                binding.estadoEntrenar.setText(R.string.training_finish_empty)
                return@setOnClickListener
            }
            mostrarDialogo(AlertDialog.Builder(this)
                .setTitle(R.string.training_finish).setMessage(R.string.training_finish_message)
                .setNegativeButton(R.string.cancel_date, null)
                .setPositiveButton(R.string.training_finish) { _, _ ->
                    ejecutar(terminar = true) { repo.finalizar(actual.id); null }
                }.create())
        }
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
        binding.finalizarEntrenamiento.isEnabled = valor
        binding.nombreEntrenamiento.isEnabled = valor
    }

    private fun ejecutar(terminar: Boolean = false, alFallar: (() -> Unit)? = null, accion: () -> Entrenamiento?) {
        if (ocupado) return
        controles(false)
        binding.reintentarEntrenar.isVisible = false
        binding.estadoEntrenar.setText(R.string.training_working)
        lifecycleScope.launch {
            try {
                sesion = withContext(Dispatchers.IO) { accion() }
                if (terminar) {
                    finalizado = true
                    binding.nombreEntrenamiento.text.clear()
                }
                if (sesion != null) finalizado = false
                mostrarSesion()
                borrador?.let { estado ->
                    borrador = null
                    sesion?.bloques?.firstOrNull { it.id == estado.getLong("bloque") }?.let { bloque ->
                        editarSerie(bloque, bloque.series.firstOrNull { it.id == estado.getLong("serie") }, estado)
                    }
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("RepForge", "No se pudo actualizar el entrenamiento", error)
                binding.estadoEntrenar.setText(R.string.training_error)
                binding.reintentarEntrenar.isVisible = true
                alFallar?.invoke()
            } finally {
                controles(true)
            }
        }
    }

    private fun mostrarSesion() {
        val actual = sesion
        binding.nuevoEntrenamiento.isVisible = actual == null
        binding.sesionEntrenamiento.isVisible = actual != null
        binding.estadoEntrenar.setText(if (actual == null) {
            if (finalizado) R.string.training_finished else R.string.training_intro
        } else R.string.training_saved)
        if (actual == null) return
        binding.nombreSesion.text = actual.nombre
        tiempoBase = (System.currentTimeMillis() - actual.inicio).coerceAtLeast(0)
        relojBase = SystemClock.elapsedRealtime()
        actualizarReloj()
        binding.resumenBloques.text = if (actual.bloques.isEmpty()) getString(R.string.training_empty)
            else resources.getQuantityString(R.plurals.training_count, actual.bloques.size, actual.bloques.size)
        binding.bloquesEntrenamiento.removeAllViews()
        actual.bloques.forEach { bloque ->
            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.tarjeta_inicio)
                val espacio = (16 * resources.displayMetrics.density).toInt()
                setPadding(espacio, espacio, espacio, espacio)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = espacio }
            }
            tarjeta.addView(TextView(this).apply {
                setTextAppearance(R.style.TextAppearance_RepForge_Subtitle)
                text = getString(R.string.training_block, bloque.orden, bloque.nombre)
            })
            if (bloque.series.isEmpty()) tarjeta.addView(TextView(this).apply {
                setTextAppearance(R.style.TextAppearance_RepForge_Detail)
                setText(R.string.series_empty)
            })
            bloque.series.forEach { serie ->
                tarjeta.addView(AppCompatButton(this).apply {
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = (8 * resources.displayMetrics.density).toInt() }
                    text = getString(R.string.series_row, serie.numero, formatoPeso(serie.pesoKg), serie.repeticiones)
                    setOnClickListener { if (!ocupado && dialogo == null) editarSerie(bloque, serie) }
                })
            }
            tarjeta.addView(AppCompatButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = (8 * resources.displayMetrics.density).toInt() }
                setText(R.string.series_add)
                contentDescription = getString(R.string.series_add) + ": " + bloque.nombre
                setOnClickListener { if (!ocupado && dialogo == null) editarSerie(bloque, null) }
            })
            binding.bloquesEntrenamiento.addView(tarjeta)
        }
    }

    private fun formatoPeso(peso: Double) = java.math.BigDecimal.valueOf(peso).stripTrailingZeros().toPlainString()

    private fun editarSerie(bloque: BloqueEjercicio, serie: Serie?, estado: Bundle? = null) {
        val sesionId = sesion?.id ?: return
        val formulario = DialogoSerieBinding.inflate(layoutInflater)
        editor = formulario
        bloqueEditado = bloque.id
        serieEditada = serie?.id
        formulario.ayudaPeso.setText(if (bloque.usaPesoCorporal) R.string.series_bodyweight else R.string.series_external)
        formulario.pesoSerie.setText(estado?.getString("peso") ?: serie?.let { formatoPeso(it.pesoKg) }.orEmpty())
        formulario.repeticionesSerie.setText(estado?.getString("repeticiones") ?: serie?.repeticiones?.toString().orEmpty())
        val constructor = AlertDialog.Builder(this)
            .setTitle(if (serie == null) getString(R.string.series_add) else getString(R.string.series_edit, serie.numero))
            .setView(formulario.root)
            .setNegativeButton(R.string.cancel_date, null)
            .setPositiveButton(R.string.series_save, null)
        if (serie != null) constructor.setNeutralButton(R.string.series_delete) { _, _ ->
            // El siguiente diálogo se abre después de cerrar el editor.
            binding.root.post {
                mostrarDialogo(AlertDialog.Builder(this)
                    .setTitle(R.string.series_delete).setMessage(R.string.series_delete_message)
                    .setNegativeButton(R.string.cancel_date, null)
                    .setPositiveButton(R.string.series_delete) { _, _ ->
                        ejecutar { repo.eliminarSerie(sesionId, bloque.id, serie.id) }
                    }.create())
            }
        }
        val ventana = constructor.create()
        mostrarDialogo(ventana)
        ventana.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val peso = formulario.pesoSerie.text.toString().trim().replace(',', '.').toDoubleOrNull()
            val repeticiones = formulario.repeticionesSerie.text.toString().trim().toIntOrNull()
            if (peso == null || repeticiones == null) {
                formulario.errorSerie.setText(R.string.series_invalid)
                return@setOnClickListener
            }
            try {
                ValidadorSerie.validar(peso, repeticiones, bloque.usaPesoCorporal)
            } catch (error: IllegalArgumentException) {
                formulario.errorSerie.text = error.message
                return@setOnClickListener
            }
            val pendiente = estadoEditor()
            ventana.dismiss()
            ejecutar(alFallar = { editarSerie(bloque, serie, pendiente) }) {
                repo.guardarSerie(sesionId, bloque.id, serie?.id, peso, repeticiones)
            }
        }
    }

    private fun estadoEditor(): Bundle? = editor?.let { formulario -> Bundle().apply {
        putLong("bloque", bloqueEditado)
        putLong("serie", serieEditada ?: 0L)
        putString("peso", formulario.pesoSerie.text.toString())
        putString("repeticiones", formulario.repeticionesSerie.text.toString())
    } }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle("editorSerie", estadoEditor())
        outState.putBoolean("finalizado", finalizado)
    }

    override fun antesDeNavegar(continuar: () -> Unit) {
        if (!ocupado) continuar()
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
        nuevo.setOnDismissListener { dialogo = null; editor = null }
        nuevo.show()
        nuevo.window?.setBackgroundDrawableResource(R.drawable.tarjeta_inicio)
        for (boton in listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL)) {
            nuevo.getButton(boton)?.setTextColor(ContextCompat.getColor(this, R.color.texto_principal))
        }
    }

    override fun onDestroy() {
        dialogo?.dismiss()
        super.onDestroy()
    }
}

