package edu.umg.repforge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import edu.umg.repforge.databinding.ActivityInicioBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InicioActivity : PantallaBaseActivity() {
    private lateinit var binding: ActivityInicioBinding
    private var carga: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicioBinding.inflate(layoutInflater)
        mostrarContenido(binding.root)
        binding.btnIniciarEntrenamiento.setOnClickListener { navegarA(Seccion.ENTRENAR) }
        binding.btnReintentarInicio.setOnClickListener { cargarPerfil() }
    }

    override fun onStart() {
        super.onStart()
        // Recargar también al volver de editar el perfil para actualizar el saludo.
        cargarPerfil()
    }

    override fun onStop() {
        carga?.cancel()
        super.onStop()
    }

    private fun cargarPerfil() {
        carga?.cancel()
        habilitarNavegacion(false)
        binding.contenidoInicio.isVisible = false
        binding.estadoInicio.isVisible = true
        binding.progresoInicio.isVisible = true
        binding.btnReintentarInicio.isVisible = false
        binding.tvEstadoInicio.setText(R.string.loading_profile)
        carga = lifecycleScope.launch {
            try {
                val perfil = withContext(Dispatchers.IO) {
                    (application as RepForgeApplication).usuarios.obtener()
                }
                if (perfil == null) {
                    startActivity(Intent(this@InicioActivity, MainActivity::class.java))
                    finish()
                    return@launch
                }
                binding.tvSaludo.text = getString(R.string.home_greeting, perfil.nombre)
                binding.estadoInicio.isVisible = false
                binding.contenidoInicio.isVisible = true
                habilitarNavegacion(true)
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("RepForge", "No se pudo cargar Inicio", error)
                binding.progresoInicio.isVisible = false
                binding.tvEstadoInicio.setText(R.string.database_open_error)
                binding.btnReintentarInicio.isVisible = true
            }
        }
    }
}
