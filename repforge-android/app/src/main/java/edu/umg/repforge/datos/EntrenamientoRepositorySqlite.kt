package edu.umg.repforge.datos

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import edu.umg.repforge.modelo.BloqueEjercicio
import edu.umg.repforge.modelo.Entrenamiento
import edu.umg.repforge.modelo.Serie
import edu.umg.repforge.servicio.ValidadorSerie

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

    @WorkerThread
    fun guardarSerie(entrenamientoId: Long, bloqueId: Long, serieId: Long?, pesoKg: Double,
                     repeticiones: Int): Entrenamiento = transaccion { db ->
        val bloque = exigirActivo(db, entrenamientoId).bloques.firstOrNull { it.id == bloqueId }
        requireNotNull(bloque) { "El ejercicio no pertenece a la sesión activa." }
        ValidadorSerie.validar(pesoKg, repeticiones, bloque.usaPesoCorporal)
        val valores = ContentValues().apply {
            put("peso_kg", pesoKg)
            put("repeticiones", repeticiones)
        }
        if (serieId == null) {
            valores.put("id_bloque_ejercicio", bloqueId)
            valores.put("numero_serie", (bloque.series.maxOfOrNull { it.numero } ?: 0) + 1)
            db.insertOrThrow("serie", null, valores)
        } else {
            require(bloque.series.any { it.id == serieId }) { "La serie ya no está disponible." }
            check(db.update("serie", valores, "id_serie = ? AND id_bloque_ejercicio = ?",
                arrayOf(serieId.toString(), bloqueId.toString())) == 1)
        }
        checkNotNull(leerActivo(db))
    }

    @WorkerThread
    fun eliminarSerie(entrenamientoId: Long, bloqueId: Long, serieId: Long): Entrenamiento = transaccion { db ->
        val bloque = exigirActivo(db, entrenamientoId).bloques.firstOrNull { it.id == bloqueId }
        requireNotNull(bloque) { "El ejercicio no pertenece a la sesión activa." }
        require(bloque.series.any { it.id == serieId }) { "La serie ya no está disponible." }
        check(db.delete("serie", "id_serie = ? AND id_bloque_ejercicio = ?",
            arrayOf(serieId.toString(), bloqueId.toString())) == 1)
        // Actualizar en orden ascendente ocupa el hueco libre sin violar UNIQUE.
        bloque.series.filter { it.id != serieId }.forEachIndexed { indice, serie ->
            db.execSQL("UPDATE serie SET numero_serie = ? WHERE id_serie = ?", arrayOf<Any>(indice + 1, serie.id))
        }
        checkNotNull(leerActivo(db))
    }

    @WorkerThread
    fun finalizar(entrenamientoId: Long, ahora: Long = System.currentTimeMillis()) = transaccion { db ->
        val sesion = exigirActivo(db, entrenamientoId)
        require(sesion.bloques.any { it.series.isNotEmpty() }) { "Guarda al menos una serie antes de finalizar." }
        sesion.bloques.filter { it.series.isEmpty() }.forEach {
            db.delete("bloque_ejercicio", "id_bloque_ejercicio = ?", arrayOf(it.id.toString()))
        }
        sesion.bloques.filter { it.series.isNotEmpty() }.forEachIndexed { indice, bloque ->
            db.execSQL("UPDATE bloque_ejercicio SET orden = ? WHERE id_bloque_ejercicio = ?", arrayOf<Any>(indice + 1, bloque.id))
        }
        check(db.update("entrenamiento", ContentValues().apply {
            put("estado", 1)
            put("fecha_hora_fin", maxOf(ahora, sesion.inicio))
        }, "id_entrenamiento = ? AND estado = 0", arrayOf(entrenamientoId.toString())) == 1)
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
                SELECT b.id_bloque_ejercicio, b.id_ejercicio, e.nombre, b.orden, e.usa_peso_corporal
                FROM bloque_ejercicio b JOIN ejercicio e ON e.id_ejercicio = b.id_ejercicio
                WHERE b.id_entrenamiento = ? ORDER BY b.orden
            """.trimIndent(), arrayOf(id.toString())).use { b ->
                buildList {
                    while (b.moveToNext()) {
                        val bloqueId = b.getLong(0)
                        val series = db.rawQuery("SELECT id_serie, numero_serie, peso_kg, repeticiones FROM serie WHERE id_bloque_ejercicio = ? ORDER BY numero_serie",
                            arrayOf(bloqueId.toString())).use { s ->
                            buildList { while (s.moveToNext()) add(Serie(s.getLong(0), s.getInt(1), s.getDouble(2), s.getInt(3))) }
                        }
                        add(BloqueEjercicio(bloqueId, b.getLong(1), b.getString(2), b.getInt(3), b.getInt(4) == 1, series))
                    }
                }
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

