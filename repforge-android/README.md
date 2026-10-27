# RepForge Android

Aplicación Android con Kotlin, XML y View Binding. Consume los modelos y validadores del core desde el JAR de Maven, sin copiar sus fuentes. Incluye Inicio, perfil, catálogo de ejercicios, registro de entrenamientos y consulta del historial en SQLite local.

## Inicio y navegación

- `InicioActivity` es la entrada de la aplicación. Consulta la base local: si no hay perfil, abre el formulario (`MainActivity`); si existe, muestra el saludo con el nombre guardado.
- Guardar el primer perfil lleva a Inicio y cierra el formulario inicial. La barra inferior compartida permite navegar entre Inicio, Entrenar, Historial, Ejercicios y Perfil. La pestaña activa tiene un icono circular elevado, y adapta sus colores al tema. La barra queda fuera del scroll y por encima de la navegación del sistema o del teclado.
- Entrenar abre la sesión en curso o el formulario para iniciarla. Historial muestra las sesiones terminadas y su detalle. Ejercicios abre el catálogo de SQLite; el botón Ir a entrenamiento de Inicio abre la misma sección Entrenar.
- Durante el registro inicial, la barra permanece visible en Perfil pero requiere guardar el perfil antes de cambiar de sección. Al editar un perfil, cambiar de pestaña o pulsar Atrás con cambios pendientes solicita confirmación para descartarlos.
- `PantallaBaseActivity` centraliza la barra y su navegación. Inicio permanece como raíz; al cambiar de una sección secundaria a otra, se reemplaza la pantalla anterior para evitar acumular un historial de pestañas. Las nuevas pantallas deben heredar esta clase y declarar `seccionActual`.
- Se reutilizan la cabecera, el tema y las fuentes. La carga ocurre fuera del hilo de interfaz; ante un error se muestra Reintentar sin borrar datos ni tratar el error como ausencia de perfil.

## Catálogo de ejercicios

La pestaña Ejercicios consulta los ejercicios activos de la base local y muestra nombre, grupo principal, grupo secundario (si existe) y tipo de carga. El selector filtra por grupo principal y conserva su selección al recrear la pantalla o cambiar de tema. Incluye carga, resultado vacío y reintento ante errores.

Los modelos `Ejercicio` y `GrupoMuscular` están en el core. `EjercicioRepositorySqlite` realiza las consultas con JOIN en Android; `EjerciciosActivity` las ejecuta en IO y presenta las filas mediante ListView con reutilización de vistas. Esta etapa es de consulta: no permite editar el catálogo ni registra entrenamientos.

Después de actualizar los modelos del core, ejecutar `mvn clean install` antes de compilar Android.

## Entrenamiento en curso

EntrenamientoActivity permite iniciar una sesión con nombre, recuperar la sesión activa, consultar el tiempo transcurrido y añadir ejercicios del catálogo mediante un selector. Cada ejercicio aparece una vez en la sesión; repetir su selección es idempotente. La lista conserva el orden de incorporación.

Cada ejercicio tiene «Añadir serie». El formulario registra peso externo en kg (acepta punto o coma decimal) y repeticiones enteras. «Guardar serie» persiste de inmediato en SQLite local. Pulsar una serie permite editarla o eliminarla con confirmación; al eliminar se renumeran las restantes. El borrador del diálogo se conserva al recrear la Activity. `ValidadorSerie`, en el core Maven, exige peso finito no negativo y repeticiones positivas; cero kg solo está permitido en ejercicios de peso corporal y representa ausencia de carga adicional.

«Finalizar entrenamiento» exige una serie guardada y pide confirmación. Una transacción elimina los bloques vacíos, ajusta el orden, guarda la fecha de fin y cambia el estado a terminado. Conserva las series y ya no permite modificar esa sesión. El mensaje de éxito confirma el guardado local; Historial permite consultarlo. Finalizar no equivale a descartar.

EntrenamientoRepositorySqlite usa transacciones para iniciar, añadir y descartar; el índice existente limita a una sesión activa. Repetir el inicio recupera la sesión existente. Las operaciones sobre IDs que ya no estén activos se rechazan. Descartar requiere confirmación en la pantalla y elimina por cascada los bloques y series de esa sesión; no modifica sesiones finalizadas.

El inicio se guarda como milisegundos Unix y permite recuperar el tiempo tras cerrar la app. Mientras la pantalla está abierta se utiliza un reloj monotónico; no hay un servicio ejecutándose en segundo plano. Las consultas y escrituras se ejecutan fuera del hilo de interfaz. No se modificó el esquema ni las bases original y plantilla.

Capturas del registro de series: [tema claro](app/img/series-claro.png) y [tema oscuro](app/img/series-oscuro.png). La lista se desplaza para acceder a Finalizar y Descartar; la cabecera y la barra inferior permanecen fijas.

## Historial

`HistorialActivity` consulta únicamente entrenamientos con estado terminado. La lista muestra nombre, fecha de inicio, duración y cantidades de ejercicios y series, ordenados por inicio descendente y por ID como desempate. La consulta agregada usa `COUNT(DISTINCT ...)` para no contar un ejercicio varias veces cuando tiene varias series. `ResumenEntrenamiento` vive en el core; la consulta SQLite permanece en Android según la guía de Clase 12.

Tocar una fila abre su detalle de solo lectura dentro de la misma Activity. Muestra inicio y fin en la zona horaria del dispositivo, duración fija calculada como fin menos inicio, ejercicios ordenados y sus series con carga externa y repeticiones. Los ejercicios desactivados siguen siendo visibles en sesiones anteriores. El historial no modifica datos, ni reinicia el cronómetro, ni afecta la sesión activa.

Atrás, «Volver al historial» o tocar la pestaña Historial estando en el detalle devuelve a la lista. El detalle seleccionado y la posición de desplazamiento se conservan al recrear la pantalla, incluido el cambio de tema. Las consultas se realizan en IO y se cancelan si una nueva selección las reemplaza. Se distinguen historial vacío, error con reintento y sesión no disponible; las otras pestañas mantienen la navegación compartida.

Capturas: lista en [claro](app/img/historial-claro.png) y [oscuro](app/img/historial-oscuro.png); detalle en [claro](app/img/historial-detalle-claro.png) y [oscuro](app/img/historial-detalle-oscuro.png).

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
- El daemon de Gradle utiliza Java 25, según `gradle/gradle-daemon-jvm.properties`, con enlaces de descarga para el JDK gestionado. Se desactiva la búsqueda automática de instalaciones externas para evitar que seleccione un runtime incompleto de otro editor (por ejemplo, el de VS Code sin `jlink`). No se incluyen rutas absolutas de la computadora. El bytecode de Android sigue configurado para JVM 17 y el core genera bytecode JVM 11.
- SDK de compilación y destino 35; mínimo 24. AndroidX AppCompat, Activity y Core.
- `mavenLocal()` está al final de `dependencyResolutionManagement`, limitado al grupo `edu.umg.repforge`.
- Dependencia: `edu.umg.repforge:repforge-core:1.0.0`. Si no se encuentra, volver a ejecutar la instalación Maven del core en esta computadora y sincronizar Android.

Si había un daemon activo antes de cambiar la selección de Java, detenerlo una vez con `.\gradlew.bat --stop` y volver a sincronizar Gradle en Android Studio. La configuración se aplica al proyecto Android; no modifica Maven ni el Java global del sistema. Referencia: https://docs.gradle.org/current/userguide/gradle_daemon.html

Si Android Studio muestra que `jlink.exe` no existe y la ruta pertenece a una extensión de VS Code, revisar también la selección local de JDK del IDE. Debe utilizar un **JDK completo de Java 25**, como la instalación Temurin 25 gestionada por Gradle, y no el runtime de la extensión. El nombre de una entrada como `25` no garantiza que apunte a un JDK completo: comprobar su ruta y que exista `bin/jlink.exe`. La selección local del IDE se guarda en `.idea`, que está excluida de Git, por lo que debe verificarse al abrir el proyecto en otra computadora.

## Comprobación manual de la pantalla

- Peso o altura vacíos: mostrar error, sin cerrar la app.
- Sin fecha o sexo seleccionado: indicar el dato faltante.
- Nombre vacío con los demás campos completos: mostrar el error del core.
- Peso o altura cero: mostrar el error del core.
- Primer perfil válido: pulsar Guardar perfil y comprobar que se abre Inicio con el nombre.
- Cerrar y volver a abrir: entrar directamente a Inicio; Mi perfil recupera todos los datos guardados, incluidas tildes y ñ.
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

Verificación realizada: 28 pruebas instrumentadas aprobadas en API 35 (19 de base y 9 de interfaz), sin fallos ni omisiones en esta ejecución. El core cuenta con 34 pruebas aprobadas. Cubren instalación y protección de la base, perfil, validaciones, transacciones, concurrencia, reapertura, borradores, navegación, catálogo y filtro. Incluyen inicio concurrente de entrenamiento, recuperación de sesión, incorporación idempotente de ejercicios, rechazo de ejercicios inactivos, protección de sesiones finalizadas y flujo de pantalla con cambio de tema y descarte confirmado. También se comprueban validación de series, edición, renumeración, pertenencia a la sesión, eliminación por cascada, recuperación del borrador y finalización con conservación de datos. El historial añade pruebas de orden, totales sin duplicación, exclusión de la sesión activa, reapertura, ejercicios desactivados, estado vacío, consulta del detalle, cambio de tema y retorno a la lista. Para las pruebas de interfaz el emulador debe estar desbloqueado y sin diálogos del sistema que cubran la aplicación.

## Apariencia compartida

Imágenes en `app/src/main/res/drawable-nodpi/`: `repforge_icono.png` es el icono de launcher, utilizado por los recursos mipmap; `repforge_logo.png` se muestra antes del título en la cabecera común. Se trasladaron desde la raíz del repositorio conservando sus contenidos originales.

Tipografía: Roboto (familia Android `sans-serif`) para cuerpo/detalles, Oswald 600 para títulos y el cronómetro de entrenamiento, Montserrat 500 para subtítulos y Montserrat 600 para botones, incluidos Aceptar/Cancelar del calendario. Estilos reutilizables en `res/values/typography.xml`.

Oswald y Montserrat se incluyen como TTF estáticos en `res/font`, compatibles con la carga de AppCompat desde API 24 y sin descargas durante el uso. Proceden de https://github.com/google/fonts/tree/main/ofl/oswald y https://github.com/google/fonts/tree/main/ofl/montserrat; las licencias SIL OFL están incluidas en `assets/licenses`. Se verificaron glifos de tildes, ñ, ü, cifras y dos puntos.

- La cabecera con el switch «Tema oscuro» queda fija fuera del desplazamiento del formulario.
- Al primer inicio se respeta el tema del sistema. Al cambiar el switch, la elección se guarda en SharedPreferences y se aplica al volver a abrir la app. No se guarda en SQLite.
- Las nuevas Activities deben extender `PantallaBaseActivity` y llamar a `mostrarContenido(binding.root)` para conservar la misma cabecera. Las pantallas que se agreguen dentro de ese contenedor también la comparten.
- Los botones usan el estilo global `Widget.RepForge.Button`, con esquinas de 16dp y colores de día/noche.
- Comprobar manualmente: alternar ambos temas, desplazar el formulario, cerrar y abrir, y cambiar de tema con campos y fecha completos para verificar la restauración del formulario.
