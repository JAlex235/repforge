package edu.umg.repforge

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import edu.umg.repforge.datos.RepForgeDatabase
import edu.umg.repforge.datos.UsuarioRepositorySqlite

class RepForgeApplication : Application() {
    val baseDatos by lazy { RepForgeDatabase(this) }
    val usuarios by lazy { UsuarioRepositorySqlite(baseDatos) }

    override fun onCreate() {
        super.onCreate()
        val preferencias = getSharedPreferences("apariencia", MODE_PRIVATE)
        val modo = if (!preferencias.contains("oscuro")) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else if (preferencias.getBoolean("oscuro", false)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(modo)
    }
}
