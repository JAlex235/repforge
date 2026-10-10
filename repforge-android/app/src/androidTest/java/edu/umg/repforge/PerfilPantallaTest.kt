package edu.umg.repforge

import android.os.SystemClock
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.umg.repforge.modelo.Usuario
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerfilPantallaTest {
    @Test
    fun botonValidaActualizaRecargaYConservaBorradorAlRecrear() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as RepForgeApplication
        // Solo manipular datos del paquete desechable, nunca del usuario.
        assumeTrue("Ejecutar con -PpruebasAisladas=true", app.packageName == "edu.umg.repforge.pruebas")
        app.usuarios.guardar(Usuario("José", 20000514, 75.5, 170, true))
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            esperar(escenario) { it.findViewById<Button>(R.id.btnValidar).text.toString() == it.getString(R.string.update_profile) }
            escenario.onActivity { pantalla ->
                assertEquals("José", pantalla.findViewById<EditText>(R.id.etNombre).text.toString())
                pantalla.findViewById<EditText>(R.id.etNombre).setText(" ")
                pantalla.findViewById<Button>(R.id.btnValidar).performClick()
                assertEquals("El nombre no puede estar vacío.", pantalla.findViewById<TextView>(R.id.tvResultado).text.toString())
            }
            assertEquals("José", app.usuarios.obtener()!!.nombre)
            escenario.onActivity { pantalla ->
                pantalla.findViewById<EditText>(R.id.etNombre).setText("María Muñoz")
                pantalla.findViewById<EditText>(R.id.etPeso).setText("78,5")
                pantalla.findViewById<Button>(R.id.btnValidar).performClick()
                pantalla.findViewById<Button>(R.id.btnValidar).performClick()
            }
            esperar(escenario) { it.findViewById<TextView>(R.id.tvResultado).text.toString() == it.getString(R.string.profile_saved) }
            assertEquals("María Muñoz", app.usuarios.obtener()!!.nombre)
            assertEquals(78.5, app.usuarios.obtener()!!.pesoKg, 0.0)
            escenario.onActivity { it.findViewById<EditText>(R.id.etNombre).setText("Borrador sin guardar") }
            escenario.recreate()
            esperar(escenario) { it.findViewById<Button>(R.id.btnValidar).isEnabled }
            escenario.onActivity { assertEquals("Borrador sin guardar", it.findViewById<EditText>(R.id.etNombre).text.toString()) }
        }
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            esperar(escenario) { it.findViewById<Button>(R.id.btnValidar).text.toString() == it.getString(R.string.update_profile) }
            escenario.onActivity { assertEquals("María Muñoz", it.findViewById<EditText>(R.id.etNombre).text.toString()) }
        }
    }

    private fun esperar(escenario: ActivityScenario<MainActivity>, condicion: (MainActivity) -> Boolean) {
        val limite = SystemClock.elapsedRealtime() + 15000
        do {
            var listo = false
            escenario.onActivity { listo = condicion(it) }
            if (listo) return
            SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < limite)
        fail("La pantalla no alcanzó el estado esperado.")
    }
}
