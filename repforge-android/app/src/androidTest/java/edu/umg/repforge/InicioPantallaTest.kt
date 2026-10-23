package edu.umg.repforge

import android.content.res.Configuration
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import edu.umg.repforge.modelo.Usuario
import androidx.test.espresso.Espresso.pressBack
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class }
InicioPantallaTest {
    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentacion.targetContext.applicationContext as RepForgeApplication

    @Before
    fun preparar() {
        assumeTrue("Ejecutar con -PpruebasAisladas=true", app.packageName == "edu.umg.repforge.pruebas")
        app.baseDatos.abrir().delete("usuario", null, null)
    }

    @After
    fun cerrarPantallas() {
        if (app.packageName != "edu.umg.repforge.pruebas") return
        instrumentacion.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            Stage.values().filter { it != Stage.DESTROYED }.flatMap {
                monitor.getActivitiesInStage(it).toList()
            }.distinct().forEach { it.finish() }
        }
        instrumentacion.waitForIdleSync()
    }

    @Test
    fun primerInicioSolicitaPerfilYGuardarAbreInicio() {
        val intent = app.packageManager.getLaunchIntentForPackage(app.packageName)!!
        assertEquals(InicioActivity::class.java.name, intent.component!!.className)
        ActivityScenario.launch<InicioActivity>(intent).use {
            esperarTexto(R.id.btnValidar, app.getString(R.string.save_profile))
            onView(withId(R.id.navPerfil)).check(matches(isSelected()))
            onView(withId(R.id.navHistorial)).perform(click())
            onView(withId(R.id.btnValidar)).check(matches(withText(R.string.save_profile)))
            onView(withId(R.id.etNombre)).perform(replaceText("José"), closeSoftKeyboard())
            onView(withId(R.id.btnFecha)).perform(scrollTo(), click())
            onView(withText(R.string.confirm_date)).perform(click())
            onView(withId(R.id.etPeso)).perform(scrollTo(), replaceText("75.5"), closeSoftKeyboard())
            onView(withId(R.id.etAltura)).perform(scrollTo(), replaceText("170"), closeSoftKeyboard())
            onView(withId(R.id.rbMasculino)).perform(scrollTo(), click())
            onView(withId(R.id.btnValidar)).perform(scrollTo(), click())
            esperarTexto(R.id.tvSaludo, "Hola, José")
            assertEquals("José", app.usuarios.obtener()!!.nombre)
            onView(withId(R.id.navInicio)).check(matches(isSelected()))
            onView(withId(R.id.navPerfil)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun perfilExistenteAbreInicioYActualizaSaludoAlVolverConTemaConservado() {
        app.usuarios.guardar(Usuario("José", 20000514, 75.5, 170, true))
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            esperarTexto(R.id.tvSaludo, "Hola, José")
            onView(withId(R.id.navPerfil)).perform(click())
            esperarTexto(R.id.btnValidar, app.getString(R.string.update_profile))
            onView(withId(R.id.etNombre)).perform(scrollTo(), replaceText("María Muñoz"), closeSoftKeyboard())
            onView(withId(R.id.btnValidar)).perform(scrollTo(), click())
            esperarTexto(R.id.tvResultado, app.getString(R.string.profile_saved))
            onView(withId(R.id.navInicio)).perform(click())
            esperarTexto(R.id.tvSaludo, "Hola, María Muñoz")
            var eraOscuro = false
            escenario.onActivity {
                eraOscuro = it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
            onView(withId(R.id.switchTema)).perform(click())
            esperarTexto(R.id.tvSaludo, "Hola, María Muñoz")
            escenario.onActivity {
                assertEquals(!eraOscuro, it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
            }
            escenario.recreate()
            esperarTexto(R.id.tvSaludo, "Hola, María Muñoz")
        }
        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.tvSaludo, "Hola, María Muñoz")
        }
    }

    @Test
    fun barraNavegaSinAcumularPantallasYProtegeEdiciones() {
        app.usuarios.guardar(Usuario("José", 20000514, 75.5, 170, true))
        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.tvSaludo, "Hola, José")
            for (seccion in listOf(Seccion.ENTRENAR, Seccion.HISTORIAL, Seccion.EJERCICIOS)) {
                onView(withId(seccion.vistaId)).perform(click())
                esperarTexto(R.id.tvSeccion, app.getString(seccion.titulo))
                onView(withId(seccion.vistaId)).check(matches(isSelected()))
            }
            // Las pestañas reemplazan destinos secundarios; Atrás vuelve directamente a Inicio.
            pressBack()
            esperarTexto(R.id.tvSaludo, "Hola, José")
            onView(withId(R.id.navPerfil)).perform(click())
            esperarTexto(R.id.btnValidar, app.getString(R.string.update_profile))
            onView(withId(R.id.etNombre)).perform(scrollTo(), replaceText("Borrador"), closeSoftKeyboard())
            onView(withId(R.id.navInicio)).perform(click())
            onView(withText(R.string.keep_editing)).perform(click())
            onView(withId(R.id.etNombre)).check(matches(withText("Borrador")))
            onView(withId(R.id.navInicio)).perform(click())
            onView(withText(R.string.discard_changes)).perform(click())
            esperarTexto(R.id.tvSaludo, "Hola, José")
            assertEquals("José", app.usuarios.obtener()!!.nombre)
        }
    }

    private fun esperarTexto(id: Int, texto: String) {
        val limite = SystemClock.elapsedRealtime() + 15000
        do {
            try {
                onView(withId(id)).check(matches(withText(texto)))
                return
            } catch (_: NoMatchingViewException) {
                // Puede estar cambiando de Activity o recargando el perfil.
            } catch (_: AssertionError) {
                // La consulta de SQLite termina fuera del hilo de interfaz.
            }
            SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < limite)
        onView(withId(id)).check(matches(withText(texto)))
    }
}
