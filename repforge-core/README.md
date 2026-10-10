# RepForge Core

Biblioteca Kotlin/JVM con Maven, siguiendo el esquema de integración de la guía de Clase 12. Contiene el modelo `Usuario` y `ValidadorUsuario`. No abre SQLite ni depende de Android.

## Compilar y publicar localmente

Desde esta carpeta, con JDK 11 o superior y Maven disponibles:

```powershell
mvn clean install
```

El comando ejecuta las pruebas y genera `target/repforge-core-1.0.0.jar`. También instala el JAR y su POM en el repositorio local de Maven. Para ejecutar solamente las pruebas: `mvn test`.

El proyecto fija Kotlin 2.2.21, bytecode JVM 11 y JUnit 5.11.4 (solo pruebas). Al crear Android se debe alinear su compilador Kotlin con el del core; no basta con cualquier versión 2.0. La integración Android aún no se ha probado.

## Primer uso

```kotlin
val usuario = Usuario(
    nombre = "Alex",
    fechaNacimiento = 20000514,
    pesoKg = 75.5,
    alturaCm = 170,
    esMasculino = true
)
val error = ValidadorUsuario().validar(usuario)
// null: perfil válido; String: primer error para mostrar al usuario.
```

Importar `edu.umg.repforge.modelo.Usuario` y `edu.umg.repforge.servicio.ValidadorUsuario`.

- `idUsuario` es `Long?`: null antes de guardar; se asignará al persistir.
- `fechaNacimiento` conserva YYYYMMDD. Se comprueban fechas reales, incluidos años bisiestos. Como regla inicial del perfil, tampoco se aceptan fechas futuras. No se impuso una edad mínima.
- Nombre con contenido, peso finito mayor que cero y altura mayor que cero.
- `esMasculino` es Boolean; la futura capa SQLite lo convertirá a 1 o 0.
- Construir o copiar `Usuario` no lo valida automáticamente: la futura operación de registro debe llamar al validador antes de guardar.
- `fechaActual` se puede pasar al validador para pruebas reproducibles; por defecto utiliza la fecha local del dispositivo.

## Conexión futura con Android

Después de `mvn clean install`, agregar `mavenLocal()` en los repositorios de `dependencyResolutionManagement` del proyecto Android y declarar:

```kotlin
implementation("edu.umg.repforge:repforge-core:1.0.0")
```

Cada equipo debe instalar el core en su repositorio Maven local. Este JAR es una biblioteca, no una aplicación ejecutable con `java -jar`. La persistencia y las pantallas corresponden a los próximos pasos.
