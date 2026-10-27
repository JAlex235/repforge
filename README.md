# RepForge

RepForge es una aplicación Android para registrar entrenamientos de fuerza. Permite guardar un perfil, consultar ejercicios, registrar pesos y repeticiones y revisar las sesiones finalizadas. Es un proyecto universitario desarrollado en Kotlin con almacenamiento local en SQLite.

**Funciona sin conexión a Internet, sin crear una cuenta y sin servicios en la nube.** Los datos se guardan en el dispositivo donde se utiliza.

## Contenido

- [Instalación](#instalación)
- [Primer inicio](#primer-inicio)
- [Pantallas y navegación](#pantallas-y-navegación)
- [Registrar un entrenamiento](#registrar-un-entrenamiento)
- [Consultar el historial](#consultar-el-historial)
- [Consultar ejercicios](#consultar-ejercicios)
- [Actualizar el perfil](#actualizar-el-perfil)
- [Tema claro y oscuro](#tema-claro-y-oscuro)
- [Datos y privacidad](#datos-y-privacidad)
- [Preguntas frecuentes](#preguntas-frecuentes)
- [Capturas de pantalla](#capturas-de-pantalla)
- [Información del proyecto](#información-del-proyecto)

## Instalación

### Requisitos

- Un teléfono o emulador con **Android 7.0 (API 24) o posterior**.
- El archivo APK de RepForge, proporcionado por el responsable del proyecto o generado desde el código fuente.
- Espacio disponible para instalar la aplicación y almacenar los registros.

Para usar la aplicación no necesitas instalar Java, Kotlin, SQLite, DataGrip ni Docker en el teléfono.

### Instalar desde un APK

1. Copia el APK de RepForge al teléfono.
2. Abre el archivo desde el administrador de archivos.
3. Si Android lo solicita, autoriza a esa aplicación para instalar el APK. La ubicación y el nombre del permiso dependen del dispositivo; instala únicamente archivos de una fuente de confianza.
4. Confirma la instalación y abre **RepForge**.

Los APK generados no están incluidos en los archivos de este repositorio. Si todavía no tienes uno, consulta las [instrucciones para compilar y ejecutar](repforge-android/README.md#abrir-y-ejecutar). El APK de desarrollo se genera en `repforge-android/app/build/outputs/apk/debug/app-debug.apk`.

Para actualizar conservando los datos, utiliza un APK compatible firmado con la misma clave e instálalo sobre la aplicación existente. No desinstales la app ni borres su almacenamiento para actualizarla.

## Primer inicio

La primera vez que abras RepForge se mostrará el formulario **Tu perfil**. Completa:

| Campo | Qué debes ingresar |
| --- | --- |
| Nombre | Tu nombre; admite tildes y ñ. |
| Fecha de nacimiento | Una fecha válida, no futura, seleccionada desde el calendario. |
| Peso | Tu peso en kilogramos, mayor que cero. |
| Altura | Tu altura en centímetros enteros, mayor que cero. |
| Sexo | Una de las opciones del formulario. |

Pulsa **Guardar perfil**. Si falta un dato o es inválido, la app mostrará un mensaje para corregirlo. Al guardar correctamente, se abrirá Inicio.

Es necesario guardar el perfil antes de navegar a las demás secciones. En las siguientes aperturas se recuperará el perfil local y entrarás directamente a Inicio. Este comportamiento no requiere iniciar sesión en un servidor.

## Pantallas y navegación

La barra inferior permite cambiar entre las cinco secciones:

| Sección | Para qué sirve |
| --- | --- |
| **Inicio** | Muestra tu saludo y el acceso **Ir a entrenamiento**. |
| **Entrenar** | Permite iniciar o continuar una sesión y registrar sus series. |
| **Historial** | Muestra las sesiones finalizadas y sus detalles. |
| **Ejercicios** | Permite consultar y filtrar el catálogo disponible. |
| **Perfil** | Permite consultar y actualizar tus datos personales. |

El encabezado incluye el logo y el interruptor de tema. En pantallas largas puedes desplazarte para ver el contenido y los botones restantes; la barra de navegación permanece disponible.

## Registrar un entrenamiento

### 1. Iniciar una sesión

1. Entra en **Entrenar**, o pulsa **Ir a entrenamiento** desde Inicio.
2. Escribe un nombre, por ejemplo, `Pecho y tríceps`.
3. Pulsa **Iniciar entrenamiento**.

La app guarda la sesión y comienza a mostrar el tiempo transcurrido. Solo puede existir **un entrenamiento en curso**. Si ya tienes uno, Entrenar lo recupera para que continúes.

### 2. Añadir ejercicios

Pulsa **Añadir ejercicio** y selecciona uno del catálogo. Repite el procedimiento para añadir otros ejercicios. Se muestran en el orden de incorporación; un mismo ejercicio no se añade dos veces a la misma sesión. Para registrar varias tandas del mismo ejercicio, añade sus series.

### 3. Registrar pesos y repeticiones

En el ejercicio correspondiente, pulsa **Añadir serie** e introduce:

- **Peso externo (kg):** la carga utilizada; acepta punto o coma decimal.
- **Repeticiones:** un número entero mayor que cero.

Pulsa **Guardar serie** para almacenarla en la base de datos local. Escribir en el formulario sin guardar no equivale a registrar una serie.

| Tipo de ejercicio | Cómo registrar el peso |
| --- | --- |
| Carga externa | Introduce un peso mayor que 0 kg. |
| Peso corporal | Introduce 0 kg si no utilizas carga adicional, o indica los kilogramos adicionales. |

Por ejemplo, una serie de un ejercicio de carga externa puede registrarse como **20 kg × 10 repeticiones**. En un ejercicio de peso corporal sin carga adicional, registra **0 kg × 10 repeticiones**: no debes introducir tu peso corporal en ese campo.

### 4. Editar o eliminar una serie

Toca una serie guardada para abrir su edición. Puedes modificar sus valores y guardarlos de nuevo, o pulsar **Eliminar serie** y confirmar. Al eliminar una serie, las restantes se renumeran automáticamente.

Estas operaciones están disponibles mientras el entrenamiento siga en curso.

### 5. Salir y continuar después

Puedes cambiar de pantalla o cerrar la aplicación y regresar a **Entrenar**. La sesión y las series que hayas guardado se recuperan desde SQLite.

El tiempo incluye lo transcurrido desde que iniciaste la sesión, aunque salgas de la pantalla o cierres la app. **No hay una función de pausa.** La aplicación conserva la fecha de inicio y calcula el tiempo al regresar; no necesita mantener un cronómetro ejecutándose permanentemente en segundo plano.

### 6. Finalizar o descartar

Desplázate por la pantalla si no ves los botones de cierre de la sesión.

| Acción | Resultado |
| --- | --- |
| **Finalizar entrenamiento** | Conserva las series, registra la fecha de fin y permite consultar la sesión en Historial. Requiere al menos una serie guardada y confirmación. Los ejercicios sin series se retiran de esa sesión. |
| **Descartar entrenamiento** | Elimina la sesión en curso junto con sus ejercicios y series, previa confirmación. No se puede deshacer y no elimina sesiones anteriores finalizadas. |

**Una sesión finalizada queda disponible únicamente para consulta.** No se puede reabrir ni editar desde Historial. Después de finalizar o descartar la sesión activa, puedes iniciar otra.

## Consultar el historial

Entra en **Historial** para ver los entrenamientos finalizados, ordenados desde la fecha de inicio más reciente.

Cada elemento muestra el nombre, la fecha de inicio, la duración y la cantidad de ejercicios y series. Toca una sesión para consultar:

- Fecha y hora de inicio y finalización.
- Duración total.
- Ejercicios en el orden registrado.
- Número de serie, peso en kilogramos y repeticiones.

La duración se calcula con las fechas guardadas de inicio y fin; ya no sigue aumentando. En ejercicios de peso corporal, los kilogramos mostrados representan la carga adicional.

Para regresar a la lista, pulsa **Volver al historial**, el botón Atrás de Android o nuevamente la pestaña Historial. Si aún no has finalizado ninguna sesión, aparecerá un mensaje indicándolo. Un entrenamiento en curso se consulta desde Entrenar y todavía no aparece en esta lista.

## Consultar ejercicios

La sección **Ejercicios** muestra el catálogo disponible. La plantilla inicial incluye **24 ejercicios y 10 grupos musculares**.

Cada ejercicio indica su nombre, grupo muscular principal, grupo secundario cuando corresponde y tipo de carga. Usa el selector **Grupo muscular principal** para filtrar, o selecciona **Todos los grupos** para recuperar la lista completa.

Esta pantalla es de consulta: no permite crear, editar ni eliminar ejercicios del catálogo. Para incorporar uno a tu sesión, utiliza **Añadir ejercicio** en Entrenar.

## Actualizar el perfil

Entra en **Perfil**, modifica los campos y pulsa **Actualizar perfil**. Se actualiza el mismo perfil local, sin crear otro usuario.

Si intentas salir con cambios pendientes, la app te permite seguir editando o descartarlos. Solo los cambios guardados se conservan en la base de datos.

## Tema claro y oscuro

Utiliza el interruptor **Tema oscuro** del encabezado para alternar la apariencia. La primera vez se respeta el tema del sistema; al elegir uno manualmente, la preferencia se guarda y se recupera al volver a abrir la aplicación.

Cambiar el tema no elimina tus entrenamientos ni tu perfil. El detalle seleccionado en Historial y su posición de desplazamiento se conservan al recrearse la pantalla por el cambio de apariencia.

## Datos y privacidad

RepForge funciona con un perfil por instalación. No solicita una cuenta, no sincroniza datos entre dispositivos y no incluye exportación ni copias de seguridad en la nube.

Para distinguir los archivos del proyecto de tus registros:

| Base de datos | Función |
| --- | --- |
| **Original** | Base de referencia utilizada en la computadora durante el desarrollo. |
| **Plantilla** | Base incluida en la aplicación, con el catálogo inicial y sin datos personales ni entrenamientos. |
| **Local** | Copia privada del dispositivo donde se guardan tu perfil, sesiones y series. Se crea desde la plantilla cuando todavía no existe. |

La base local se conserva al cerrar la app y al reiniciar el teléfono. No se reemplaza por la plantilla en cada apertura. La preferencia de tema se guarda aparte, en las preferencias locales de la aplicación.

**Borrar el almacenamiento de RepForge o desinstalarla elimina sus datos locales.** Si vuelves a comenzar, se crea una base nueva desde la plantilla, pero tus sesiones anteriores no se recuperan. Instalar la aplicación en otro teléfono tampoco transfiere el historial.

## Preguntas frecuentes

### ¿Necesito Internet para entrenar?

No. El perfil, el catálogo y los entrenamientos se consultan y guardan localmente.

### ¿Por qué mi sesión no aparece en Historial?

Revisa Entrenar: si la sesión sigue activa, debes guardar al menos una serie y confirmar **Finalizar entrenamiento** para que aparezca en Historial.

### ¿Por qué no puedo guardar una serie?

Comprueba que las repeticiones sean enteras y mayores que cero. El peso debe ser válido y no negativo; 0 kg solo se permite en ejercicios de peso corporal.

### ¿Qué hago si aparece un error de carga?

Pulsa **Reintentar**. Si continúa, cierra y vuelve a abrir la aplicación. Conserva el mensaje de error para reportarlo al responsable del proyecto. No borres el almacenamiento como primer intento de solución, porque perderías tus registros.

### ¿Por qué el cronómetro avanzó mientras la aplicación estaba cerrada?

Porque mide el tiempo desde el inicio guardado del entrenamiento. Salir de la app no finaliza ni pausa la sesión.

### ¿Cómo puedo probar la aplicación desde cero?

Solo si aceptas perder el perfil y los entrenamientos de esa instalación, abre la información de RepForge en los ajustes de Android y utiliza la opción para borrar su almacenamiento o datos. Los nombres de los menús varían según el dispositivo. Borrar únicamente la caché no equivale a reiniciar los datos de la app.

### ¿Android Studio se queda en “Waiting for all target devices to come online”?

Ese mensaje corresponde a la conexión con el emulador. Espera a que Android termine de iniciar; si el dispositivo permanece desconectado, reinícialo con un arranque completo (**Cold Boot**) desde el administrador de dispositivos de Android Studio. No utilices **Wipe Data** para este caso: esa opción borra los datos del dispositivo virtual.

## Capturas de pantalla

Las imágenes muestran ejemplos de uso; los datos visibles son ilustrativos.

| Pantalla | Tema claro | Tema oscuro |
| --- | --- | --- |
| Catálogo | [Ver captura](repforge-android/app/img/catalogo-claro.png) | [Ver captura](repforge-android/app/img/catalogo-oscuro.png) |
| Entrenamiento | [Ver captura](repforge-android/app/img/entrenamiento-claro.png) | [Ver captura](repforge-android/app/img/entrenamiento-oscuro.png) |
| Registro de series | [Ver captura](repforge-android/app/img/series-claro.png) | [Ver captura](repforge-android/app/img/series-oscuro.png) |
| Historial | [Ver captura](repforge-android/app/img/historial-claro.png) | [Ver captura](repforge-android/app/img/historial-oscuro.png) |
| Detalle del historial | [Ver captura](repforge-android/app/img/historial-detalle-claro.png) | [Ver captura](repforge-android/app/img/historial-detalle-oscuro.png) |

## Información del proyecto

RepForge se enfoca en el registro manual de entrenamientos de fuerza. No incluye cardio, rutinas automáticas, gráficas de progreso, sincronización entre dispositivos ni edición de sesiones finalizadas.

El repositorio se organiza en:

- **`repforge-core/`:** modelos y validaciones en Kotlin, compilados con Maven.
- **`repforge-android/`:** aplicación Android, pantallas XML y acceso a SQLite; se compila con Gradle y consume el core desde Maven local.
- **`repforge_schema.sql`:** definición de las tablas y restricciones.
- **`repforge_seed.sql`:** datos iniciales del catálogo.

Para instalación del entorno, compilación, pruebas y detalles de implementación, consulta la [documentación técnica de Android](repforge-android/README.md). Esta guía principal describe el uso de la aplicación.
