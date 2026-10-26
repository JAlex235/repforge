package edu.umg.repforge

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import androidx.lifecycle.lifecycleScope
import edu.umg.repforge.databinding.ActivityEjerciciosBinding
import edu.umg.repforge.databinding.ItemEjercicioBinding
import edu.umg.repforge.modelo.Ejercicio
import edu.umg.repforge.modelo.GrupoMuscular
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EjerciciosActivity : PantallaBaseActivity() {
    override val seccionActual = Seccion.EJERCICIOS
    private lateinit var binding: ActivityEjerciciosBinding
    private var grupoSeleccionado = 0L
    private var grupos = emptyList<GrupoMuscular>()
    private var ejercicios = emptyList<Ejercicio>()
    private val filas = AdaptadorEjercicios()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        grupoSeleccionado = savedInstanceState?.getLong("grupo") ?: 0L
        binding = ActivityEjerciciosBinding.inflate(layoutInflater)
        mostrarContenido(binding.root)
        binding.listaEjercicios.adapter = filas
        binding.reintentarCatalogo.setOnClickListener { cargar() }
        cargar()
    }

    private fun cargar() {
        binding.filtroGrupo.isEnabled = false
        binding.reintentarCatalogo.visibility = View.GONE
        binding.estadoCatalogo.setText(R.string.catalog_loading)
        lifecycleScope.launch {
            try {
                val datos = withContext(Dispatchers.IO) {
                    val repo = (application as RepForgeApplication).ejercicios
                    repo.grupos() to repo.listar()
                }
                grupos = datos.first
                ejercicios = datos.second
                binding.filtroGrupo.adapter = ArrayAdapter(
                    this@EjerciciosActivity, R.layout.item_grupo,
                    listOf(getString(R.string.catalog_all)) + grupos.map { it.nombre }
                )
                val posicion = grupos.indexOfFirst { it.id == grupoSeleccionado } + 1
                if (posicion == 0) grupoSeleccionado = 0L
                binding.filtroGrupo.setSelection(posicion)
                binding.filtroGrupo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        grupoSeleccionado = grupos.getOrNull(position - 1)?.id ?: 0L
                        filtrar()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
                binding.filtroGrupo.isEnabled = true
                filtrar()
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("RepForge", "No se pudo cargar el catálogo", error)
                binding.estadoCatalogo.setText(R.string.catalog_error)
                binding.reintentarCatalogo.visibility = View.VISIBLE
            }
        }
    }

    private fun filtrar() {
        filas.datos = ejercicios.filter {
            grupoSeleccionado == 0L || it.grupoPrincipal.id == grupoSeleccionado
        }
        filas.notifyDataSetChanged()
        binding.estadoCatalogo.text = if (filas.datos.isEmpty()) getString(R.string.catalog_empty)
            else resources.getQuantityString(R.plurals.catalog_count, filas.count, filas.count)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("grupo", grupoSeleccionado)
        super.onSaveInstanceState(outState)
    }

    private inner class AdaptadorEjercicios : BaseAdapter() {
        var datos = emptyList<Ejercicio>()
        override fun getCount() = datos.size
        override fun getItem(position: Int) = datos[position]
        override fun getItemId(position: Int) = getItem(position).id
        override fun hasStableIds() = true
        override fun areAllItemsEnabled() = false
        override fun isEnabled(position: Int) = false
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val fila = convertView?.tag as? ItemEjercicioBinding
                ?: ItemEjercicioBinding.inflate(layoutInflater, parent, false).also { it.root.tag = it }
            val ejercicio = getItem(position)
            fila.nombreEjercicio.text = ejercicio.nombre
            fila.gruposEjercicio.text = getString(R.string.catalog_primary, ejercicio.grupoPrincipal.nombre) +
                (ejercicio.grupoSecundario?.let { "\n" + getString(R.string.catalog_secondary, it.nombre) } ?: "")
            fila.tipoEjercicio.setText(if (ejercicio.usaPesoCorporal) R.string.catalog_bodyweight else R.string.catalog_external_weight)
            return fila.root
        }
    }
}

