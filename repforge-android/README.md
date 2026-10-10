# RepForge Android

Primera pantalla Android con Kotlin, XML y View Binding. Consume `Usuario` y `ValidadorUsuario` desde el JAR de Maven, sin copiar sus fuentes. El formulario carga, valida, guarda y actualiza el perfil en SQLite local.

## Abrir y ejecutar

1. En `../repforge-core`, ejecutar `mvn clean install`.
2. En Android Studio, seleccionar **Open** y abrir esta carpeta (`repforge-android`).
3. Esperar a que termine Gradle Sync. El SDK local se configura en `local.properties`, archivo excluido de Git.
4. Elegir un emulador o teléfono y pulsar **Run** sobre `app`.

Para compilar y ejecutar el análisis estático desde PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

APK de desarrollo: `app/build/outputs/apk/debug/app-debug.apk`.

## Configuración

- Android Gradle Plugin 9.4.1 y Gradle Wrapper 9.6.0, disponibles en el entorno al crear el proyecto.
- AGP 9 integra Kotlin; no se aplica el plugin `org.jetbrains.kotlin.android` por separado. Es una adaptación de configuración respecto a la guía de Clase 12. Referencia: https://developer.android.com/build/migrate-to-built-in-kotlin
- JDK 17 o superior compatible con Gradle; bytecode de Android configurado para JVM 17. El core genera bytecode JVM 11.
- SDK de compilación y destino 35; mínimo 24. AndroidX AppCompat, Activity y Core.
- `mavenLocal()` está al final de `dependencyResolutionManagement`, limitado al grupo `edu.umg.repforge`.
- Dependencia: `edu.umg.repforge:repforge-core:1.0.0`. Si no se encuentra, volver a ejecutar la instalación Maven del core en esta computadora y sincronizar Android.

## Comprobación manual de la pantalla

- Peso o altura vacíos: mostrar error, sin cerrar la app.
- Sin fecha o sexo seleccionado: indicar el dato faltante.
- Nombre vacío con los demás campos completos: mostrar el error del core.
- Peso o altura cero: mostrar el error del core.
- Perfil válido: pulsar Guardar perfil y comprobar la confirmación. El botón cambia a Actualizar perfil.
- Cerrar y volver a abrir: recuperar todos los datos guardados, incluidas tildes y ñ.
- Editar y actualizar: modificar el mismo registro sin crear otro usuario.
- Comprobar selección de fecha, teclado, desplazamiento y rotación de pantalla. La fecha se conserva en el estado de la Activity; los campos con ID usan la restauración estándar de Android.

`UsuarioRepositorySqlite` valida mediante el core y guarda dentro de una transacción. La pantalla realiza las operaciones en `Dispatchers.IO`, deshabilita el formulario mientras trabaja y permite reintentar una carga fallida. La restauración de la Activity conserva las ediciones pendientes; solo se persisten al pulsar el botón.

## Base de datos inicial

`app/src/main/assets/databases/repforge.db` contiene una copia coherente de la base del proyecto: seis tablas, diez grupos y veinticuatro ejercicios, sin perfiles ni historial. Solo en la copia empaquetada se establece `PRAGMA user_version = 1`; la original de Windows conserva su contenido y versión.

`RepForgeDatabase` copia el asset a un archivo temporal, sincroniza el contenido, verifica integridad, tablas, versión y relaciones, y lo renombra en la misma carpeta al finalizar. Un error de copia no deja un archivo definitivo incompleto. Al reabrir, conserva la base instalada y activa claves foráneas. Una versión desconocida o un archivo corrupto producen un error sin reemplazar datos. Las futuras versiones requerirán migraciones explícitas.

`RepForgeApplication` mantiene una instancia compartida. `MainActivity` solicita su apertura en `Dispatchers.IO` al iniciarse. No utiliza la ruta de Windows: Android resuelve su propia ubicación con `getDatabasePath`. La preferencia de tema continúa en SharedPreferences.

Para ejecutar las pruebas de instalación, reapertura y fallos en un dispositivo/emulador conectado:

```powershell
.\gradlew.bat -PpruebasAisladas=true :app:connectedDebugAndroidTest
```

La propiedad `pruebasAisladas` utiliza el paquete `edu.umg.repforge.pruebas`, separado de la app habitual. Las pruebas de base trabajan en carpetas temporales; la prueba de pantalla guarda únicamente en ese paquete y se omite si no se activa la propiedad. El ejecutor puede retirar los paquetes probados al terminar. Compilar sin la propiedad para generar la aplicación normal. Una actualización del asset no se aplica automáticamente a instalaciones existentes.

Verificación realizada: doce pruebas instrumentadas aprobadas en API 35, sin fallos ni omisiones. Cubren instalación y protección de la base, guardado y actualización, validación, transacciones, concurrencia y flujo de pantalla con reapertura y conservación de borradores.

## Apariencia compartida

Imágenes en `app/src/main/res/drawable-nodpi/`: `repforge_icono.png` es el icono de launcher, utilizado por los recursos mipmap; `repforge_logo.png` se muestra antes del título en la cabecera común. Se trasladaron desde la raíz del repositorio conservando sus contenidos originales.

Tipografía: Roboto (familia Android `sans-serif`) para cuerpo/detalles, Oswald 600 para títulos y futuros cronómetros, Montserrat 500 para subtítulos y Montserrat 600 para botones, incluidos Aceptar/Cancelar del calendario. Estilos reutilizables en `res/values/typography.xml`; el estilo Timer está preparado, pero aún no existe una pantalla de cronómetro.

Oswald y Montserrat se incluyen como TTF estáticos en `res/font`, compatibles con la carga de AppCompat desde API 24 y sin descargas durante el uso. Proceden de https://github.com/google/fonts/tree/main/ofl/oswald y https://github.com/google/fonts/tree/main/ofl/montserrat; las licencias SIL OFL están incluidas en `assets/licenses`. Se verificaron glifos de tildes, ñ, ü, cifras y dos puntos.

- La cabecera con el switch «Tema oscuro» queda fija fuera del desplazamiento del formulario.
- Al primer inicio se respeta el tema del sistema. Al cambiar el switch, la elección se guarda en SharedPreferences y se aplica al volver a abrir la app. No se guarda en SQLite.
- Las nuevas Activities deben extender `PantallaBaseActivity` y llamar a `mostrarContenido(binding.root)` para conservar la misma cabecera. Las pantallas que se agreguen dentro de ese contenedor también la comparten.
- Los botones usan el estilo global `Widget.RepForge.Button`, con esquinas de 16dp y colores de día/noche.
- Comprobar manualmente: alternar ambos temas, desplazar el formulario, cerrar y abrir, y cambiar de tema con campos y fecha completos para verificar la restauración del formulario.
