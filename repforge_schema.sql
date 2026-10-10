-- RepForge
-- Esquema inicial de base de datos SQLite
-- Solo creación de tablas y restricciones. No inserta datos.

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL
        CHECK (length(trim(nombre)) > 0),
    fecha_nacimiento INTEGER NOT NULL,
    peso_kg REAL NOT NULL
        CHECK (peso_kg > 0),
    altura_cm INTEGER NOT NULL
        CHECK (altura_cm > 0),
    es_masculino INTEGER NOT NULL
        CHECK (es_masculino IN (0, 1))
);

CREATE TABLE IF NOT EXISTS grupo_muscular (
    id_grupo INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL COLLATE NOCASE
        CHECK (length(trim(nombre)) > 0),
    UNIQUE (nombre)
);

CREATE TABLE IF NOT EXISTS ejercicio (
    id_ejercicio INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL COLLATE NOCASE
        CHECK (length(trim(nombre)) > 0),
    id_grupo_principal INTEGER NOT NULL,
    id_grupo_secundario INTEGER,
    es_personalizado INTEGER NOT NULL
        CHECK (es_personalizado IN (0, 1)),
    activo INTEGER NOT NULL
        CHECK (activo IN (0, 1)),
    usa_peso_corporal INTEGER NOT NULL
        CHECK (usa_peso_corporal IN (0, 1)),

    UNIQUE (nombre),

    FOREIGN KEY (id_grupo_principal)
        REFERENCES grupo_muscular(id_grupo)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    FOREIGN KEY (id_grupo_secundario)
        REFERENCES grupo_muscular(id_grupo)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS entrenamiento (
    id_entrenamiento INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL
        CHECK (length(trim(nombre)) > 0),
    fecha_hora_inicio INTEGER NOT NULL,
    fecha_hora_fin INTEGER,
    notas TEXT,
    estado INTEGER NOT NULL
        CHECK (estado IN (0, 1)),

    CHECK (
        (estado = 0 AND fecha_hora_fin IS NULL)
        OR
        (estado = 1 AND fecha_hora_fin IS NOT NULL
         AND fecha_hora_fin >= fecha_hora_inicio)
    )
);

-- Solo puede existir un entrenamiento activo a la vez.
-- estado: 0 = ACTIVO, 1 = FINALIZADO
CREATE UNIQUE INDEX IF NOT EXISTS idx_un_entrenamiento_activo
ON entrenamiento(estado)
WHERE estado = 0;

CREATE TABLE IF NOT EXISTS bloque_ejercicio (
    id_bloque_ejercicio INTEGER PRIMARY KEY,
    id_entrenamiento INTEGER NOT NULL,
    id_ejercicio INTEGER NOT NULL,
    orden INTEGER NOT NULL
        CHECK (orden > 0),

    UNIQUE (id_entrenamiento, orden),

    FOREIGN KEY (id_entrenamiento)
        REFERENCES entrenamiento(id_entrenamiento)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    FOREIGN KEY (id_ejercicio)
        REFERENCES ejercicio(id_ejercicio)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS serie (
    id_serie INTEGER PRIMARY KEY,
    id_bloque_ejercicio INTEGER NOT NULL,
    numero_serie INTEGER NOT NULL
        CHECK (numero_serie > 0),
    peso_kg REAL NOT NULL
        CHECK (peso_kg >= 0),
    repeticiones INTEGER NOT NULL
        CHECK (repeticiones > 0),

    UNIQUE (id_bloque_ejercicio, numero_serie),

    FOREIGN KEY (id_bloque_ejercicio)
        REFERENCES bloque_ejercicio(id_bloque_ejercicio)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Reglas que se controlarán desde Kotlin / lógica de la app:
--
-- 1. fecha_nacimiento se guarda como INTEGER con formato YYYYMMDD y se obtiene
--    mediante DatePicker.
-- 2. peso_kg = 0 solo se permite cuando el ejercicio relacionado tiene
--    usa_peso_corporal = 1.
-- 3. Un bloque sin series se elimina al finalizar el entrenamiento.
-- 4. Al eliminar una serie intermedia, las posteriores se renumeran para
--    mantener 1, 2, 3, ...
-- 5. Los ejercicios precargados no se editan ni se eliminan.
-- 6. Un ejercicio personalizado con historial no se elimina físicamente;
--    se desactiva. La FK RESTRICT protege el historial.
-- 7. Los grupos musculares serán precargados e inmutables desde la aplicación.
