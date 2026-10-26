package edu.umg.repforge

import android.content.res.Configuration
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import edu.umg.repforge.databinding.ActivityBaseBinding

/** Las pantallas usan este contenedor para mantener el selector fuera del scroll. */
abstract class PantallaBaseActivity : AppCompatActivity() {
    protected open val seccionActual = Seccion.INICIO
    private var navegacionDisponible = true
    private var cambiandoSeccion = false

    override fun onResume() {
        super.onResume()
        cambiandoSeccion = false
    }

    protected fun habilitarNavegacion(disponible: Boolean) {
        navegacionDisponible = disponible
    }

    protected open fun antesDeNavegar(continuar: () -> Unit) = continuar()

    protected fun navegarA(seccion: Seccion) {
        if (seccion == seccionActual || cambiandoSeccion) return
        if (!navegacionDisponible) {
            Toast.makeText(this, R.string.nav_complete_profile, Toast.LENGTH_SHORT).show()
            return
        }
        antesDeNavegar {
            if (!cambiandoSeccion) {
                cambiandoSeccion = true
                val destino = when (seccion) {
                    Seccion.INICIO -> InicioActivity::class.java
                    Seccion.PERFIL -> MainActivity::class.java
                    Seccion.EJERCICIOS -> EjerciciosActivity::class.java
                    Seccion.ENTRENAR -> EntrenamientoActivity::class.java
                    else -> SeccionPendienteActivity::class.java
                }
                startActivity(Intent(this, destino).apply {
                    putExtra("seccion", seccion.name)
                    if (seccion == Seccion.INICIO) {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                })
                // Inicio es la raíz. No acumular una Activity por cada toque en una pestaña.
                if (seccionActual != Seccion.INICIO) finish()
            }
        }
    }

    protected fun mostrarContenido(contenido: View) {
        enableEdgeToEdge()
        val marco = ActivityBaseBinding.inflate(layoutInflater)
        marco.contenidoPantalla.addView(contenido)
        setContentView(marco.root)
        Seccion.entries.forEach { seccion ->
            val seleccionada = seccion == seccionActual
            val color = ContextCompat.getColor(this,
                if (seleccionada) R.color.acento else R.color.texto_secundario)
            val opcion = LinearLayout(this).apply {
                id = seccion.vistaId
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                clipChildren = false
                clipToPadding = false
                isSelected = seleccionada
                isFocusable = true
                contentDescription = getString(seccion.titulo)
                minimumHeight = dp(80)
                setOnClickListener { navegarA(seccion) }
            }
            opcion.addView(ImageView(this).apply {
                setImageResource(seccion.icono)
                setColorFilter(color)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                if (seleccionada) {
                    translationY = -dp(12).toFloat()
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(this@PantallaBaseActivity, R.color.superficie))
                        setStroke(dp(5), ContextCompat.getColor(this@PantallaBaseActivity, R.color.pantalla_fondo))
                    }
                }
            }, LinearLayout.LayoutParams(dp(56), dp(56)))
            opcion.addView(TextView(this).apply {
                setText(seccion.titulo)
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(color)
                typeface = ResourcesCompat.getFont(this@PantallaBaseActivity,
                    if (seleccionada) R.font.montserrat_600 else R.font.montserrat_500)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            marco.opcionesNavegacion.addView(opcion, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        ViewCompat.setOnApplyWindowInsetsListener(marco.root) { view, insets ->
            val espacio = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(espacio.left, espacio.top, espacio.right, espacio.bottom)
            insets
        }

        marco.switchTema.isChecked = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        marco.switchTema.setOnCheckedChangeListener { _, oscuro ->
            getSharedPreferences("apariencia", MODE_PRIVATE).edit()
                .putBoolean("oscuro", oscuro).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (oscuro) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}
