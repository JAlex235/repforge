package edu.umg.repforge.datos

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import edu.umg.repforge.modelo.BloqueEjercicio
import edu.umg.repforge.modelo.Entrenamiento

class EntrenamientoRepositorySqlite(private val baseDatos: RepForgeDatabase) {
    @WorkerThread
    fun activo(): Entrenamiento? = transaccion { leerActivo(it) }

    /** Reintentar el inicio devuelve la sesión existente, sin crear otra. */
    @WorkerThread
    fun iniciar(nombre: String, ahora: Long = System.currentTimeMillis()): Entrenamiento = transaccion { db ->
        val limpio = nombre.trim()
        require(limpio.isNotEmpty()) { "Ingresa un nombre para el entrenamiento." }
        leerActivo(db) ?: run {
            db.insertOrThrow("entrenamiento", null, ContentValues().apply {
                put("nombre", limpio)
                put("fecha_hora_inicio", ahora)
                put("estado", 0)
            })
            checkNotNull(leerActivo(db))
        }
    }

    /** Una selección repetida no duplica el bloque de la sesión. */
    @WorkerThread
    fun agregar(entrenamientoId: Long, ejercicioId: Long): Entrenamiento = transaccion { db ->
        val sesion = exigirActivo(db, entrenamientoId)
        val disponible = db.rawQuery("SELECT 1 FROM ejercicio WHERE id_ejercicio = ? AND activo = 1",
            arrayOf(ejercicioId.toString())).use { it.moveToFirst() }
        require(disponible) { "El ejercicio ya no está disponible." }
        if (sesion.bloques.none { it.ejercicioId == ejercicioId }) {
            db.insertOrThrow("bloque_ejercicio", null, ContentValues().apply {
                put("id_entrenamiento", entrenamientoId)
                put("id_ejercicio", ejercicioId)
                put("orden", (sesion.bloques.maxOfOrNull { it.orden } ?: 0) + 1)
            })
        }
        checkNotNull(leerActivo(db))
    }

    @WorkerThread
    fun descartar(entrenamientoId: Long) = transaccion { db ->
        exigirActivo(db, entrenamientoId)
        check(db.delete("entrenamiento", "id_entrenamiento = ? AND estado = 0",
            arrayOf(entrenamientoId.toString())) == 1)
        // Las FK con CASCADE eliminan los bloques y sus series.
    }

    private fun exigirActivo(db: SQLiteDatabase, id: Long): Entrenamiento {
        val actual = leerActivo(db)
        require(actual?.id == id) { "Esta sesión ya no está activa. Vuelve a cargarla." }
        return checkNotNull(actual)
    }

    private fun leerActivo(db: SQLiteDatabase): Entrenamiento? =
        db.rawQuery("SELECT id_entrenamiento, nombre, fecha_hora_inicio FROM entrenamiento WHERE estado = 0", null).use { c ->
            if (!c.moveToFirst()) return@use null
            val id = c.getLong(0)
            val bloques = db.rawQuery("""
                SELECT b.id_bloque_ejercicio, b.id_ejercicio, e.nombre, b.orden
                FROM bloque_ejercicio b JOIN ejercicio e ON e.id_ejercicio = b.id_ejercicio
                WHERE b.id_entrenamiento = ? ORDER BY b.orden
            """.trimIndent(), arrayOf(id.toString())).use { b ->
                buildList { while (b.moveToNext()) add(BloqueEjercicio(b.getLong(0), b.getLong(1), b.getString(2), b.getInt(3))) }
            }
            Entrenamiento(id, c.getString(1), c.getLong(2), bloques)
        }

    private fun <T> transaccion(accion: (SQLiteDatabase) -> T): T {
        val db = baseDatos.abrir()
        db.beginTransaction()
        try {
            val resultado = accion(db)
            db.setTransactionSuccessful()
            return resultado
        } finally {
            db.endTransaction()
        }
    }
}

