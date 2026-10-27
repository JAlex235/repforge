package edu.umg.repforge.datos

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import edu.umg.repforge.modelo.Usuario

@RunWith(AndroidJUnit4::class)
class RepForgeDatabaseTest {
    private lateinit var contexto: Context
    private lateinit var carpeta: File
    private val gestores = mutableListOf<RepForgeDatabase>()

    @Before
    fun preparar() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        carpeta = File(app.cacheDir, "db-test-${UUID.randomUUID()}")
        contexto = object : ContextWrapper(app) {
            override fun getDatabasePath(name: String) = File(carpeta, name)
        }
    }

    @After
    fun limpiar() {
        gestores.forEach { it.close() }
        // Carpeta exclusiva de esta prueba; nunca toca la base real de la aplicación.
        carpeta.deleteRecursively()
    }

    private fun gestor(asset: () -> InputStream = { contexto.assets.open("databases/repforge.db") }): RepForgeDatabase =
        RepForgeDatabase(contexto, asset).also { gestores.add(it) }

    private fun numero(db: SQLiteDatabase, sql: String): Int =
        db.rawQuery(sql, null).use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun entrenamientoSeRecuperaYAgregarEsIdempotente() {
        val base = gestor()
        val repo = EntrenamientoRepositorySqlite(base)
        assertNull(repo.activo())
        val sesion = repo.iniciar(" Fuerza ", 1000)
        assertEquals("Fuerza", sesion.nombre)
        assertEquals(sesion.id, repo.iniciar("Otra sesión", 2000).id)
        val ejercicios = EjercicioRepositorySqlite(base).listar()
        repo.agregar(sesion.id, ejercicios[0].id)
        repo.agregar(sesion.id, ejercicios[0].id)
        repo.agregar(sesion.id, ejercicios[1].id)
        val esperado = repo.activo()!!
        assertEquals(listOf(1, 2), esperado.bloques.map { it.orden })
        base.close()
        val reabierto = EntrenamientoRepositorySqlite(gestor())
        assertEquals(esperado, reabierto.activo())
        reabierto.descartar(sesion.id)
        assertNull(reabierto.activo())
        assertEquals(0, numero(gestor().abrir(), "SELECT COUNT(*) FROM bloque_ejercicio"))
    }

    @Test
    fun seriesSeEditanRenumeranYConservanAlReabrir() {
        val base = gestor()
        val repo = EntrenamientoRepositorySqlite(base)
        val sesion = repo.iniciar("Series", 1000)
        val ejercicio = EjercicioRepositorySqlite(base).listar().first()
        val bloque = repo.agregar(sesion.id, ejercicio.id).bloques.single()
        repeat(3) { repo.guardarSerie(sesion.id, bloque.id, null, 20.5, 10) }
        val series = repo.activo()!!.bloques.single().series
        repo.guardarSerie(sesion.id, bloque.id, series[0].id, 25.0, 8)
        repo.eliminarSerie(sesion.id, bloque.id, series[1].id)
        repo.guardarSerie(sesion.id, bloque.id, null, 30.0, 6)
        val esperado = repo.activo()!!
        assertEquals(listOf(1, 2, 3), esperado.bloques.single().series.map { it.numero })
        assertEquals(series[2].id, esperado.bloques.single().series[1].id)
        assertEquals(25.0, esperado.bloques.single().series[0].pesoKg, 0.0)
        assertEquals(8, esperado.bloques.single().series[0].repeticiones)
        base.close()
        assertEquals(esperado, EntrenamientoRepositorySqlite(gestor()).activo())
    }

    @Test
    fun finalizarConservaSeriesQuitaVaciosYProtegeSesionTerminada() {
        val base = gestor()
        val repo = EntrenamientoRepositorySqlite(base)
        val sesion = repo.iniciar("Final", 1000)
        val ejercicios = EjercicioRepositorySqlite(base).listar()
        repeat(3) { repo.agregar(sesion.id, ejercicios[it].id) }
        try { repo.finalizar(sesion.id); fail("No debe finalizar vacía") } catch (_: IllegalArgumentException) {}
        assertEquals(3, repo.activo()!!.bloques.size)
        val bloque = repo.activo()!!.bloques[1]
        repo.guardarSerie(sesion.id, bloque.id, null, 20.0, 10)
        repo.finalizar(sesion.id, 500) // Un ajuste del reloj no puede violar la fecha de inicio.
        assertNull(repo.activo())
        assertEquals(1, numero(base.abrir(), "SELECT COUNT(*) FROM serie"))
        assertEquals(1, numero(base.abrir(), "SELECT COUNT(*) FROM bloque_ejercicio"))
        assertEquals(1, numero(base.abrir(), "SELECT orden FROM bloque_ejercicio"))
        assertEquals(1000, numero(base.abrir(), "SELECT fecha_hora_fin FROM entrenamiento"))
        assertEquals(1, numero(base.abrir(), "SELECT estado FROM entrenamiento"))
        try { repo.guardarSerie(sesion.id, bloque.id, null, 20.0, 10); fail("Sesión cerrada") } catch (_: IllegalArgumentException) {}
        try { repo.eliminarSerie(sesion.id, bloque.id, 1); fail("Sesión cerrada") } catch (_: IllegalArgumentException) {}
        assertNotEquals(sesion.id, repo.iniciar("Nueva").id)
    }

    @Test
    fun validarSeriesImpideCambiosInvalidosYReferenciasAjenas() {
        val base = gestor()
        val repo = EntrenamientoRepositorySqlite(base)
        val sesion = repo.iniciar("Validación")
        val ejercicios = EjercicioRepositorySqlite(base).listar()
        val corporal = ejercicios.first { it.usaPesoCorporal }
        val externo = ejercicios.first { !it.usaPesoCorporal }
        repo.agregar(sesion.id, corporal.id)
        val bloques = repo.agregar(sesion.id, externo.id).bloques
        repo.guardarSerie(sesion.id, bloques[0].id, null, 0.0, 12)
        val esperado = repo.activo()
        val operaciones = listOf<() -> Unit>(
            { repo.guardarSerie(sesion.id, bloques[1].id, null, 0.0, 12) },
            { repo.guardarSerie(sesion.id, bloques[0].id, null, 1.0, 0) },
            { repo.guardarSerie(sesion.id, -1, null, 20.0, 10) },
            { repo.guardarSerie(sesion.id, bloques[1].id, esperado!!.bloques[0].series[0].id, 20.0, 10) }
        )
        operaciones.forEach { operacion ->
            try { operacion(); fail("Debe rechazar operación inválida") } catch (_: IllegalArgumentException) {}
            assertEquals(esperado, repo.activo())
        }
        repo.descartar(sesion.id)
        assertEquals(0, numero(base.abrir(), "SELECT COUNT(*) FROM serie"))
    }

    @Test
    fun entrenamientoRechazaNombreVacioEjercicioInactivoYSesionFinalizada() {
        val base = gestor()
        val repo = EntrenamientoRepositorySqlite(base)
        try { repo.iniciar("  "); fail("Debe rechazar nombre vacío") } catch (_: IllegalArgumentException) {}
        assertNull(repo.activo())
        val sesion = repo.iniciar("Fuerza", 1000)
        val id = EjercicioRepositorySqlite(base).listar().first().id
        base.abrir().execSQL("UPDATE ejercicio SET activo = 0 WHERE id_ejercicio = ?", arrayOf(id))
        try { repo.agregar(sesion.id, id); fail("Debe rechazar inactivo") } catch (_: IllegalArgumentException) {}
        assertTrue(repo.activo()!!.bloques.isEmpty())
        base.abrir().execSQL("UPDATE entrenamiento SET estado = 1, fecha_hora_fin = 2000")
        try { repo.descartar(sesion.id); fail("No debe borrar historial") } catch (_: IllegalArgumentException) {}
        assertEquals(1, numero(base.abrir(), "SELECT COUNT(*) FROM entrenamiento"))
    }

    @Test
    fun dosIniciosConcurrentesCreanUnaSolaSesion() {
        val base = gestor()
        val repo = EntrenamientoRepositorySqlite(base)
        val pool = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val futuros = (1..2).map { i -> pool.submit<Long> { repo.iniciar("Sesión $i").id } }
            assertEquals(futuros[0].get(), futuros[1].get())
            assertEquals(1, numero(base.abrir(), "SELECT COUNT(*) FROM entrenamiento"))
        } finally { pool.shutdownNow() }
    }

    @Test
    fun catalogoRelacionaGruposYOmiteEjerciciosInactivos() {
        val base = gestor()
        val repo = EjercicioRepositorySqlite(base)
        assertEquals(10, repo.grupos().size)
        val ejercicios = repo.listar()
        assertEquals(24, ejercicios.size)
        assertTrue(ejercicios.any { it.grupoSecundario == null })
        assertTrue(ejercicios.any { it.grupoSecundario != null })
        assertTrue(ejercicios.any { it.usaPesoCorporal })
        assertTrue(ejercicios.all { e -> repo.grupos().any { it.id == e.grupoPrincipal.id } })
        val id = ejercicios.first().id
        base.abrir().execSQL("UPDATE ejercicio SET activo = 0 WHERE id_ejercicio = ?", arrayOf(id))
        assertEquals(23, repo.listar().size)
        assertFalse(repo.listar().any { it.id == id })
    }

    @Test
    fun guardaActualizaSinDuplicarYRecuperaTodosLosCampos() {
        val base = gestor()
        val repo = UsuarioRepositorySqlite(base)
        assertNull(repo.obtener())
        val nuevo = repo.guardar(Usuario(" José Muñoz ", 20000514, 75.5, 170, true))
        assertNotNull(nuevo.idUsuario)
        assertEquals("José Muñoz", nuevo.nombre)
        val actualizado = repo.guardar(nuevo.copy(nombre = "María", pesoKg = 78.5, esMasculino = false))
        assertEquals(nuevo.idUsuario, actualizado.idUsuario)
        repo.guardar(actualizado.copy(idUsuario = null))
        assertEquals(1, numero(base.abrir(), "SELECT COUNT(*) FROM usuario"))
        base.close()
        assertEquals(actualizado, UsuarioRepositorySqlite(gestor()).obtener())
    }

    @Test
    fun validacionFallidaNoModificaElPerfil() {
        val repo = UsuarioRepositorySqlite(gestor())
        val original = repo.guardar(Usuario("José", 20000514, 75.5, 170, true))
        for (invalido in listOf(original.copy(nombre = " "), original.copy(pesoKg = 0.0),
            original.copy(fechaNacimiento = 20010229), original.copy(alturaCm = -1))) {
            assertThrows(IllegalArgumentException::class.java) { repo.guardar(invalido) }
            assertEquals(original, repo.obtener())
        }
    }

    @Test
    fun falloDuranteTransaccionConservaDatosYPermiteGuardarDespues() {
        val repo = UsuarioRepositorySqlite(gestor())
        val original = repo.guardar(Usuario("José", 20000514, 75.5, 170, true))
        assertThrows(IllegalArgumentException::class.java) {
            repo.guardar(original.copy(idUsuario = original.idUsuario!! + 1, nombre = "No guardar"))
        }
        assertEquals(original, repo.obtener())
        assertEquals("Ana", repo.guardar(original.copy(nombre = "Ana")).nombre)
    }

    @Test
    fun guardadosConcurrentesNoDuplicanElPerfil() {
        val base = gestor()
        val repo = UsuarioRepositorySqlite(base)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val tareas = (1..2).map { n -> executor.submit<Usuario> {
                repo.guardar(Usuario("Perfil $n", 20000514, 75.5, 170, true))
            } }
            val resultados = tareas.map { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }
            assertEquals(resultados[0].idUsuario, resultados[1].idUsuario)
            assertEquals(1, numero(base.abrir(), "SELECT COUNT(*) FROM usuario"))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun instalaCatalogoYActivaClavesForaneas() {
        val administrador = gestor()
        val db = administrador.abrir()
        assertSame(db, administrador.abrir())
        assertEquals(1, db.version)
        assertEquals(10, numero(db, "SELECT COUNT(*) FROM grupo_muscular"))
        assertEquals(24, numero(db, "SELECT COUNT(*) FROM ejercicio"))
        for (tabla in listOf("usuario", "entrenamiento", "bloque_ejercicio", "serie")) {
            assertEquals(0, numero(db, "SELECT COUNT(*) FROM $tabla"))
        }
        assertEquals(1, numero(db, "PRAGMA foreign_keys"))
        assertThrows(SQLiteConstraintException::class.java) {
            db.execSQL("INSERT INTO ejercicio(nombre,id_grupo_principal,es_personalizado,activo,usa_peso_corporal) VALUES('Prueba',999999,1,1,0)")
        }
    }

    @Test
    fun reaperturaConservaPerfilYNoVuelveACopiar() {
        val primero = gestor()
        primero.abrir().execSQL("INSERT INTO usuario(nombre,fecha_nacimiento,peso_kg,altura_cm,es_masculino) VALUES('José',20000514,75.5,170,1)")
        primero.close()
        val db = gestor { error("No debe volver a leer assets") }.abrir()
        db.rawQuery("SELECT nombre FROM usuario", null).use {
            assertTrue(it.moveToFirst())
            assertEquals("José", it.getString(0))
        }
        assertEquals(1, numero(db, "PRAGMA foreign_keys"))
    }

    @Test
    fun copiaInterrumpidaNoDejaBaseDefinitivaYPermiteReintento() {
        val roto = gestor {
            object : InputStream() {
                private var leidos = 0
                override fun read(): Int {
                    if (leidos++ >= 128) throw IOException("Interrupción simulada")
                    return 0
                }
            }
        }
        assertThrows(IOException::class.java) { roto.abrir() }
        assertFalse(contexto.getDatabasePath("repforge.db").exists())
        assertFalse(File(carpeta, "repforge.db.tmp").exists())
        assertEquals(24, numero(gestor().abrir(), "SELECT COUNT(*) FROM ejercicio"))
    }

    @Test
    fun rechazaAssetCorruptoSinInstalarlo() {
        assertThrows(Exception::class.java) {
            gestor { ByteArrayInputStream("No es SQLite".toByteArray()) }.abrir()
        }
        assertFalse(contexto.getDatabasePath("repforge.db").exists())
        assertFalse(File(carpeta, "repforge.db.tmp").exists())
    }

    @Test
    fun conservaBaseExistenteDeVersionDesconocida() {
        val primero = gestor()
        primero.abrir().version = 99
        primero.close()
        assertThrows(IllegalStateException::class.java) { gestor().abrir() }
        SQLiteDatabase.openDatabase(contexto.getDatabasePath("repforge.db").path, null, SQLiteDatabase.OPEN_READONLY).use {
            assertEquals(99, it.version)
            assertEquals(24, numero(it, "SELECT COUNT(*) FROM ejercicio"))
        }
    }

    @Test
    fun conservaArchivoExistenteCorrupto() {
        carpeta.mkdirs()
        val archivo = contexto.getDatabasePath("repforge.db")
        val original = "Archivo existente que requiere recuperación".toByteArray()
        archivo.writeBytes(original)
        assertThrows(Exception::class.java) { gestor().abrir() }
        assertArrayEquals(original, archivo.readBytes())
    }

    @Test
    fun recuperaTemporalDeUnaEjecucionInterrumpida() {
        carpeta.mkdirs()
        File(carpeta, "repforge.db.tmp").writeText("incompleto")
        assertEquals(24, numero(gestor().abrir(), "SELECT COUNT(*) FROM ejercicio"))
        assertFalse(File(carpeta, "repforge.db.tmp").exists())
    }
}
