package edu.umg.repforge.datos

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import edu.umg.repforge.modelo.Usuario
import edu.umg.repforge.servicio.ValidadorUsuario

/** RepForge utiliza un único perfil local. Los cambios se realizan en una transacción. */
class UsuarioRepositorySqlite(private val baseDatos: RepForgeDatabase) {
    @WorkerThread
    fun obtener(): Usuario? = leer(baseDatos.abrir())

    @WorkerThread
    fun guardar(usuario: Usuario): Usuario {
        val perfil = usuario.copy(nombre = usuario.nombre.trim())
        val error = ValidadorUsuario().validar(perfil)
        require(error == null) { error ?: "Perfil inválido." }
        val db = baseDatos.abrir()
        db.beginTransaction()
        try {
            val existente = leer(db)
            // No sobrescribir un perfil distinto si el llamador entrega un ID incorrecto.
            require(perfil.idUsuario == null || perfil.idUsuario == existente?.idUsuario) {
                "El identificador no corresponde al perfil local."
            }
            val valores = ContentValues().apply {
                put("nombre", perfil.nombre)
                put("fecha_nacimiento", perfil.fechaNacimiento)
                put("peso_kg", perfil.pesoKg)
                put("altura_cm", perfil.alturaCm)
                put("es_masculino", if (perfil.esMasculino) 1 else 0)
            }
            val id = if (existente == null) {
                db.insertOrThrow("usuario", null, valores)
            } else {
                check(db.update("usuario", valores, "id_usuario = ?",
                    arrayOf(existente.idUsuario.toString())) == 1) { "No se pudo actualizar el perfil." }
                checkNotNull(existente.idUsuario)
            }
            db.setTransactionSuccessful()
            return perfil.copy(idUsuario = id)
        } finally {
            db.endTransaction()
        }
    }

    private fun leer(db: SQLiteDatabase): Usuario? = db.query(
        "usuario", arrayOf("id_usuario", "nombre", "fecha_nacimiento", "peso_kg", "altura_cm", "es_masculino"),
        null, null, null, null, "id_usuario", "2"
    ).use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        check(cursor.count == 1) { "La base contiene más de un perfil; se requiere revisión." }
        Usuario(
            idUsuario = cursor.getLong(0), nombre = cursor.getString(1),
            fechaNacimiento = cursor.getInt(2), pesoKg = cursor.getDouble(3),
            alturaCm = cursor.getInt(4), esMasculino = cursor.getInt(5) == 1
        )
    }
}
