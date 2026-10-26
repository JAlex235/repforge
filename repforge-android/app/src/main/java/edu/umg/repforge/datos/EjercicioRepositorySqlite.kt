package edu.umg.repforge.datos

import androidx.annotation.WorkerThread
import edu.umg.repforge.modelo.Ejercicio
import edu.umg.repforge.modelo.GrupoMuscular

/** Consultas de catálogo; no modifica los ejercicios precargados ni el historial. */
class EjercicioRepositorySqlite(private val baseDatos: RepForgeDatabase) {
    @WorkerThread
    fun grupos(): List<GrupoMuscular> = baseDatos.abrir().rawQuery(
        "SELECT id_grupo, nombre FROM grupo_muscular ORDER BY nombre COLLATE NOCASE", null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(GrupoMuscular(cursor.getLong(0), cursor.getString(1)))
        }
    }

    @WorkerThread
    fun listar(): List<Ejercicio> = baseDatos.abrir().rawQuery(
        """
        SELECT e.id_ejercicio, e.nombre, p.id_grupo, p.nombre, s.id_grupo, s.nombre,
               e.es_personalizado, e.usa_peso_corporal
        FROM ejercicio e
        JOIN grupo_muscular p ON p.id_grupo = e.id_grupo_principal
        LEFT JOIN grupo_muscular s ON s.id_grupo = e.id_grupo_secundario
        WHERE e.activo = 1
        ORDER BY e.nombre COLLATE NOCASE
        """.trimIndent(), null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(Ejercicio(
                cursor.getLong(0), cursor.getString(1),
                GrupoMuscular(cursor.getLong(2), cursor.getString(3)),
                if (cursor.isNull(4)) null else GrupoMuscular(cursor.getLong(4), cursor.getString(5)),
                cursor.getInt(6) == 1, cursor.getInt(7) == 1
            ))
        }
    }
}

