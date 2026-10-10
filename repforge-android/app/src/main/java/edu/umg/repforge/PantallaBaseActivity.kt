package edu.umg.repforge

import android.content.res.Configuration
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import edu.umg.repforge.databinding.ActivityBaseBinding

/** Las pantallas usan este contenedor para mantener el selector fuera del scroll. */
abstract class PantallaBaseActivity : AppCompatActivity() {
    protected fun mostrarContenido(contenido: View) {
        enableEdgeToEdge()
        val marco = ActivityBaseBinding.inflate(layoutInflater)
        marco.contenidoPantalla.addView(contenido)
        setContentView(marco.root)

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
}
