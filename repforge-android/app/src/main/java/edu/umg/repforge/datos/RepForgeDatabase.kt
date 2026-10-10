package edu.umg.repforge.datos

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import androidx.annotation.WorkerThread
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/** Una instancia compartida por la aplicación. No abre ni copia en el hilo de UI. */
class RepForgeDatabase(
    private val contexto: Context,
    private val abrirAsset: () -> InputStream = { contexto.assets.open("databases/repforge.db") }
) : AutoCloseable {
    private var conexion: SQLiteDatabase? = null

    @WorkerThread
    fun abrir(): SQLiteDatabase = synchronized(bloqueo) {
        conexion?.takeIf { it.isOpen }?.let { return@synchronized it }
        val destino = contexto.getDatabasePath("repforge.db")
        if (!destino.exists()) instalar(destino)

        val db = abrirArchivo(destino, SQLiteDatabase.OPEN_READWRITE)
        try {
            verificar(db)
            db.setForeignKeyConstraintsEnabled(true)
            conexion = db
            db
        } catch (error: Exception) {
            db.close()
            // Una base existente nunca se borra ni se reemplaza por fallar una apertura.
            throw error
        }
    }

    private fun instalar(destino: File) {
        val carpeta = checkNotNull(destino.parentFile)
        check(carpeta.isDirectory || carpeta.mkdirs()) { "No se pudo crear la carpeta de la base." }
        val temporal = File(carpeta, "repforge.db.tmp")
        try {
            // Se sobrescribe únicamente el temporal de una instalación interrumpida.
            abrirAsset().use { origen ->
                FileOutputStream(temporal).use { salida ->
                    origen.copyTo(salida)
                    salida.fd.sync()
                }
            }
            abrirArchivo(temporal, SQLiteDatabase.OPEN_READONLY).use { verificar(it) }
            check(!destino.exists()) { "La base ya fue instalada." }
            check(temporal.renameTo(destino)) { "No se pudo completar la instalación de la base." }
        } finally {
            // Nunca eliminar el archivo definitivo: puede contener datos del usuario.
            if (temporal.exists()) temporal.delete()
        }
    }

    private fun abrirArchivo(archivo: File, modo: Int): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            archivo.absolutePath, null, modo or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
            DatabaseErrorHandler {
                // El manejador predeterminado puede borrar una base corrupta.
                throw SQLiteDatabaseCorruptException("La base necesita revisión; se conserva el archivo.")
            }
        )

    private fun verificar(db: SQLiteDatabase) {
        check(db.version == VERSION) {
            "Versión de base no compatible (${db.version}); se requiere una migración a $VERSION."
        }
        db.rawQuery("PRAGMA quick_check", null).use { cursor ->
            check(cursor.moveToFirst() && cursor.getString(0) == "ok") { "La base no superó la verificación de integridad." }
        }
        val tablas = mutableSetOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { cursor ->
            while (cursor.moveToNext()) tablas.add(cursor.getString(0))
        }
        check(tablas.containsAll(TABLAS)) { "La base no contiene todas las tablas de RepForge." }
        db.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
            check(!cursor.moveToFirst()) { "La base contiene relaciones inválidas." }
        }
    }

    override fun close() = synchronized(bloqueo) {
        conexion?.close()
        conexion = null
    }

    companion object {
        private val bloqueo = Any()
        private const val VERSION = 1
        private val TABLAS = setOf("usuario", "grupo_muscular", "ejercicio", "entrenamiento", "bloque_ejercicio", "serie")
    }
}
