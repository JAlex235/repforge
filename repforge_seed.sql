-- RepForge: catalogo inicial de fuerza.
-- Ejecutar despues de repforge_schema.sql, en una conexion sin transaccion abierta.
-- No crea perfiles, entrenamientos, bloques ni series.
-- Los nombres identifican los registros: no se presuponen IDs numericos.
-- Al repetir la carga se conservan intactos los registros que ya existen.

PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;

WITH catalogo(nombre) AS (
    VALUES
        ('Pecho'),
        ('Espalda'),
        ('Hombros'),
        ('Bíceps'),
        ('Tríceps'),
        ('Cuádriceps'),
        ('Isquiotibiales'),
        ('Glúteos'),
        ('Pantorrillas'),
        ('Abdomen')
)
INSERT INTO grupo_muscular (nombre)
SELECT c.nombre
FROM catalogo AS c
WHERE NOT EXISTS (
    SELECT 1 FROM grupo_muscular AS g WHERE g.nombre = c.nombre
);

-- El grupo secundario es opcional. Se registra solo uno, segun el modelo.
-- Esta clasificacion simplificada no enumera todos los musculos participantes.
-- usa_peso_corporal = 1 permite registrar series sin carga externa (peso_kg = 0).
-- Todos los ejercicios de este catalogo son predeterminados y estan activos.
WITH catalogo(nombre, principal, secundario, corporal) AS (
    VALUES
        ('Press de banca con barra',       'Pecho',          'Tríceps',  0),
        ('Press inclinado con mancuernas', 'Pecho',          'Tríceps',  0),
        ('Aperturas con mancuernas',       'Pecho',          NULL,       0),
        ('Flexiones de brazos',           'Pecho',          'Tríceps',  1),
        ('Jalón al pecho',                'Espalda',        'Bíceps',   0),
        ('Remo con barra',                'Espalda',        'Bíceps',   0),
        ('Remo con mancuerna',            'Espalda',        'Bíceps',   0),
        ('Dominadas',                     'Espalda',        'Bíceps',   1),
        ('Press militar con barra',       'Hombros',        'Tríceps',  0),
        ('Elevaciones laterales',         'Hombros',        NULL,       0),
        ('Curl de bíceps con barra',      'Bíceps',         NULL,       0),
        ('Curl de bíceps con mancuernas', 'Bíceps',         NULL,       0),
        ('Extensión de tríceps en polea', 'Tríceps',        NULL,       0),
        ('Press francés con barra',       'Tríceps',        NULL,       0),
        ('Sentadilla con barra',          'Cuádriceps',     'Glúteos',  0),
        ('Prensa de piernas',             'Cuádriceps',     'Glúteos',  0),
        ('Extensión de cuádriceps',       'Cuádriceps',     NULL,       0),
        ('Sentadilla sin carga',          'Cuádriceps',     'Glúteos',  1),
        ('Peso muerto rumano con barra',  'Isquiotibiales', 'Glúteos',  0),
        ('Curl femoral en máquina',       'Isquiotibiales', NULL,       0),
        ('Hip thrust con barra',          'Glúteos',        NULL,       0),
        ('Elevación de talones en máquina', 'Pantorrillas', NULL,       0),
        ('Crunch abdominal',              'Abdomen',        NULL,       1),
        ('Crunch abdominal en polea',     'Abdomen',        NULL,       0)
)
INSERT INTO ejercicio (
    nombre,
    id_grupo_principal,
    id_grupo_secundario,
    es_personalizado,
    activo,
    usa_peso_corporal
)
SELECT c.nombre, principal.id_grupo, secundario.id_grupo, 0, 1, c.corporal
FROM catalogo AS c
JOIN grupo_muscular AS principal ON principal.nombre = c.principal
LEFT JOIN grupo_muscular AS secundario ON secundario.nombre = c.secundario
WHERE NOT EXISTS (
    SELECT 1 FROM ejercicio AS e WHERE e.nombre = c.nombre
);

COMMIT;

-- La inmutabilidad de estos catalogos se aplica en Kotlin, segun el esquema.
-- SQLite NOCASE no normaliza todas las letras acentuadas: conservar los nombres
-- canonicos y validar duplicados con acentos en la aplicacion.
