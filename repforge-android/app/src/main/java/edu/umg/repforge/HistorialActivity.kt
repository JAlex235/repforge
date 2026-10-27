package edu.umg.repforge

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.activity.OnBackPressedCallback
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import edu.umg.repforge.databinding.ActivityHistorialBinding
import edu.umg.repforge.databinding.BloqueHistorialBinding
import edu.umg.repforge.databinding.ItemHistorialBinding
import edu.umg.repforge.modelo.Entrenamiento
import edu.umg.repforge.modelo.ResumenEntrenamiento
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class HistorialActivity : PantallaBaseActivity() {
    override val seccionActual = Seccion.HISTORIAL
    private lateinit var binding: ActivityHistorialBinding
    private val repo get() = (application as RepForgeApplication).entrenamientos
    private val filas = AdaptadorHistorial()
    private var seleccionado = 0L
    private var estadoLista: Parcelable? = null
    private var posicionDetalle = 0
    private var carga: Job? = null
    private val volver = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = mostrarLista()
    }
    private val fecha = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("es", "GT"))
    private val numero = NumberFormat.getNumberInstance(Locale("es", "GT")).apply {
        isGroupingUsed = false
        maximumFractionDigits = 340
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seleccionado = savedInstanceState?.getLong("entrenamiento") ?: 0L
        estadoLista = savedInstanceState?.let { BundleCompat.getParcelable(it, "lista", Parcelable::class.java) }
        posicionDetalle = savedInstanceState?.getInt("detalleY") ?: 0
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        mostrarContenido(binding.root)
        binding.listaHistorial.adapter = filas
        binding.listaHistorial.setOnItemClickListener { _, _, posicion, _ ->
            estadoLista = binding.listaHistorial.onSaveInstanceState()
            seleccionado = filas.getItem(posicion).id
            posicionDetalle = 0
            cargar()
        }
        binding.reintentarHistorial.setOnClickListener { cargar() }
        binding.volverHistorial.setOnClickListener { mostrarLista() }
        onBackPressedDispatcher.addCallback(this, volver)
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    override fun onPause() {
        if (binding.listaHistorial.isVisible) estadoLista = binding.listaHistorial.onSaveInstanceState()
        if (binding.detalleHistorial.isVisible) posicionDetalle = binding.detalleHistorial.scrollY
        super.onPause()
    }

    override fun alSeleccionarSeccionActual() {
        if (seleccionado != 0L) mostrarLista()
    }

    private fun mostrarLista() {
        seleccionado = 0L
        posicionDetalle = 0
        cargar()
    }

    private fun cargar() {
        carga?.cancel()
        val id = seleccionado
        volver.isEnabled = id != 0L
        binding.volverHistorial.isVisible = id != 0L
        binding.listaHistorial.isVisible = false
        binding.detalleHistorial.isVisible = false
        binding.reintentarHistorial.isVisible = false
        binding.estadoHistorial.setText(R.string.history_loading)
        carga = lifecycleScope.launch {
            try {
                if (id == 0L) {
                    filas.datos = withContext(Dispatchers.IO) { repo.historial() }
                    filas.notifyDataSetChanged()
                    binding.estadoHistorial.text = if (filas.count == 0) getString(R.string.history_empty)
                        else resources.getQuantityString(R.plurals.history_count, filas.count, filas.count)
                    binding.listaHistorial.isVisible = filas.count > 0
                    estadoLista?.let { binding.listaHistorial.onRestoreInstanceState(it) }
                } else {
                    val sesion = withContext(Dispatchers.IO) { repo.finalizado(id) }
                    if (sesion == null) binding.estadoHistorial.setText(R.string.history_missing)
                    else mostrarDetalle(sesion)
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("RepForge", "No se pudo consultar el historial", error)
                binding.listaHistorial.isVisible = false
                binding.detalleHistorial.isVisible = false
                binding.estadoHistorial.setText(R.string.history_error)
                binding.reintentarHistorial.isVisible = true
            }
        }
    }

    private fun mostrarDetalle(sesion: Entrenamiento) {
        val fin = checkNotNull(sesion.fin)
        binding.nombreHistorial.text = sesion.nombre
        binding.fechasHistorial.text = getString(R.string.history_dates, fecha.format(Date(sesion.inicio)), fecha.format(Date(fin)))
        binding.duracionHistorial.text = duracion(sesion.inicio, fin)
        binding.resumenHistorial.text = cantidades(sesion.bloques.size, sesion.bloques.sumOf { it.series.size })
        binding.bloquesHistorial.removeAllViews()
        sesion.bloques.forEach { bloque ->
            val tarjeta = BloqueHistorialBinding.inflate(layoutInflater, binding.bloquesHistorial, false)
            tarjeta.nombreBloqueHistorial.text = getString(R.string.training_block, bloque.orden, bloque.nombre)
            tarjeta.tipoBloqueHistorial.setText(if (bloque.usaPesoCorporal) R.string.history_bodyweight else R.string.history_external)
            tarjeta.seriesHistorial.text = bloque.series.joinToString("\n") {
                getString(R.string.history_series_row, it.numero, numero.format(it.pesoKg), it.repeticiones)
            }
            binding.bloquesHistorial.addView(tarjeta.root)
        }
        binding.estadoHistorial.setText(R.string.history_read_only)
        binding.detalleHistorial.isVisible = true
        binding.detalleHistorial.post {
            if (seleccionado == sesion.id) binding.detalleHistorial.scrollTo(0, posicionDetalle)
        }
    }

    private fun cantidades(ejercicios: Int, series: Int) = getString(R.string.history_numbers,
        resources.getQuantityString(R.plurals.history_exercises, ejercicios, ejercicios),
        resources.getQuantityString(R.plurals.history_series, series, series))

    private fun duracion(inicio: Long, fin: Long): String {
        val segundos = ((fin - inicio) / 1000).coerceAtLeast(0)
        return String.format(Locale.ROOT, "%02d:%02d:%02d", segundos / 3600, segundos / 60 % 60, segundos % 60)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("entrenamiento", seleccionado)
        outState.putParcelable("lista", if (binding.listaHistorial.isVisible) binding.listaHistorial.onSaveInstanceState() else estadoLista)
        outState.putInt("detalleY", binding.detalleHistorial.scrollY)
        super.onSaveInstanceState(outState)
    }

    private inner class AdaptadorHistorial : BaseAdapter() {
        var datos = emptyList<ResumenEntrenamiento>()
        override fun getCount() = datos.size
        override fun getItem(position: Int) = datos[position]
        override fun getItemId(position: Int) = getItem(position).id
        override fun hasStableIds() = true
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val fila = convertView?.tag as? ItemHistorialBinding
                ?: ItemHistorialBinding.inflate(layoutInflater, parent, false).also { it.root.tag = it }
            val sesion = getItem(position)
            fila.nombreResumen.text = sesion.nombre
            fila.fechaResumen.text = fecha.format(Date(sesion.inicio))
            fila.cifrasResumen.text = getString(R.string.history_summary,
                cantidades(sesion.cantidadEjercicios, sesion.cantidadSeries), duracion(sesion.inicio, sesion.fin))
            return fila.root
        }
    }
}
