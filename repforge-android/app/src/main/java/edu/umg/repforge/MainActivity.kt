package edu.umg.repforge

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import edu.umg.repforge.databinding.ActivityMainBinding
import edu.umg.repforge.modelo.Usuario
import edu.umg.repforge.servicio.ValidadorUsuario
import java.text.DateFormat
import java.util.Calendar

class MainActivity : PantallaBaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private val validador = ValidadorUsuario()
    private var fechaNacimiento: Int? = null
    private var formularioInicializado = false
    private var baseDisponible = false
    private var ocupado = false
    private var existePerfil = false
    private val usuarios get() = (application as RepForgeApplication).usuarios

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        mostrarContenido(binding.root)
        formularioInicializado = savedInstanceState?.getBoolean("formularioInicializado") ?: false
        fechaNacimiento = savedInstanceState?.getInt("fechaNacimiento")?.takeIf { it != 0 }
        mostrarFecha()
        binding.btnFecha.setOnClickListener { seleccionarFecha() }
        binding.btnValidar.setOnClickListener {
            if (!baseDisponible) prepararBaseDatos() else guardarPerfil()
        }
        listOf(binding.etNombre, binding.etPeso, binding.etAltura).forEach { campo ->
            campo.doAfterTextChanged { binding.tvResultado.text = "" }
        }
        binding.rgSexo.setOnCheckedChangeListener { _, _ -> binding.tvResultado.text = "" }
        prepararBaseDatos()
    }

    private fun prepararBaseDatos() {
        if (ocupado) return
        ocupado = true
        actualizarControles()
        lifecycleScope.launch {
            try {
                val perfil = withContext(Dispatchers.IO) { usuarios.obtener() }
                existePerfil = perfil != null
                if (!formularioInicializado) {
                    perfil?.let { mostrarPerfil(it) }
                    formularioInicializado = true
                }
                baseDisponible = true
                binding.tvResultado.text = ""
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("RepForge", "No se pudo abrir la base local", error)
                binding.tvResultado.setText(R.string.database_open_error)
            } finally {
                ocupado = false
                actualizarControles()
            }
        }
    }

    private fun mostrarPerfil(perfil: Usuario) {
        binding.etNombre.setText(perfil.nombre)
        binding.etPeso.setText(perfil.pesoKg.toString())
        binding.etAltura.setText(perfil.alturaCm.toString())
        fechaNacimiento = perfil.fechaNacimiento
        mostrarFecha()
        binding.rgSexo.check(if (perfil.esMasculino) R.id.rbMasculino else R.id.rbFemenino)
    }

    private fun actualizarControles() {
        val editable = baseDisponible && !ocupado
        listOf(binding.etNombre, binding.etPeso, binding.etAltura, binding.btnFecha,
            binding.rbFemenino, binding.rbMasculino).forEach { it.isEnabled = editable }
        binding.btnValidar.isEnabled = !ocupado
        binding.btnValidar.setText(when {
            ocupado && !baseDisponible -> R.string.loading_profile
            ocupado -> R.string.saving_profile
            !baseDisponible -> R.string.retry_profile
            existePerfil -> R.string.update_profile
            else -> R.string.save_profile
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        fechaNacimiento?.let { outState.putInt("fechaNacimiento", it) }
        outState.putBoolean("formularioInicializado", formularioInicializado)
        super.onSaveInstanceState(outState)
    }

    private fun seleccionarFecha() {
        val inicial = Calendar.getInstance()
        fechaNacimiento?.let { fecha ->
            inicial.set(fecha / 10000, fecha / 100 % 100 - 1, fecha % 100)
        }
        val dialogo = DatePickerDialog(this, R.style.Theme_RepForge_DatePicker, { _, anio, mes, dia ->
            fechaNacimiento = anio * 10000 + (mes + 1) * 100 + dia
            mostrarFecha()
            binding.tvResultado.text = ""
        }, inicial.get(Calendar.YEAR), inicial.get(Calendar.MONTH), inicial.get(Calendar.DAY_OF_MONTH))
        dialogo.datePicker.maxDate = System.currentTimeMillis()
        dialogo.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.confirm_date), dialogo)
        dialogo.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel_date), dialogo)
        dialogo.show()
        // Los botones nativos no deben heredar el texto oscuro de los botones rellenos.
        val colorAccion = ContextCompat.getColor(this, R.color.texto_principal)
        dialogo.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(colorAccion)
        dialogo.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(colorAccion)
        val fuenteBoton = ResourcesCompat.getFont(this, R.font.montserrat_600)
        dialogo.getButton(DialogInterface.BUTTON_POSITIVE).typeface = fuenteBoton
        dialogo.getButton(DialogInterface.BUTTON_NEGATIVE).typeface = fuenteBoton
    }

    private fun mostrarFecha() {
        val fecha = fechaNacimiento ?: return
        val calendario = Calendar.getInstance().apply {
            clear()
            set(fecha / 10000, fecha / 100 % 100 - 1, fecha % 100)
        }
        binding.btnFecha.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(calendario.time)
    }

    private fun guardarPerfil() {
        if (ocupado || !baseDisponible) return
        binding.tvResultado.text = ""
        binding.etPeso.error = null
        binding.etAltura.error = null

        // La pantalla convierte el texto; las reglas del perfil viven en el core.
        val peso = binding.etPeso.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (peso == null) {
            binding.etPeso.error = getString(R.string.invalid_weight)
            binding.etPeso.requestFocus()
            return
        }
        val altura = binding.etAltura.text.toString().trim().toIntOrNull()
        if (altura == null) {
            binding.etAltura.error = getString(R.string.invalid_height)
            binding.etAltura.requestFocus()
            return
        }
        val fecha = fechaNacimiento
        if (fecha == null) {
            binding.tvResultado.setText(R.string.missing_birth)
            return
        }
        val masculino = when (binding.rgSexo.checkedRadioButtonId) {
            R.id.rbMasculino -> true
            R.id.rbFemenino -> false
            else -> {
                binding.tvResultado.setText(R.string.missing_sex)
                return
            }
        }
        val usuario = Usuario(
            nombre = binding.etNombre.text.toString().trim(),
            fechaNacimiento = fecha,
            pesoKg = peso,
            alturaCm = altura,
            esMasculino = masculino
        )
        val error = validador.validar(usuario)
        if (error != null) {
            binding.tvResultado.text = error
            return
        }
        ocupado = true
        actualizarControles()
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { usuarios.guardar(usuario) }
                existePerfil = true
                binding.tvResultado.setText(R.string.profile_saved)
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (fallo: Exception) {
                Log.e("RepForge", "No se pudo guardar el perfil", fallo)
                binding.tvResultado.setText(R.string.profile_save_error)
            } finally {
                ocupado = false
                actualizarControles()
            }
        }
    }
}
