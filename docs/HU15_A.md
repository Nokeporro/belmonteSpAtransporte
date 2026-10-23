# HU15 de A: alta y edición de conductores

Alcance: HU15/RF14 en Android con Room/demo y en el backend. La integración Retrofit corresponde
a T14; desactivar o reactivar sigue siendo HU26 y las reglas de habilitación corresponden a HU18.

## Comportamiento

- El Administrador registra nombre, correo, teléfono opcional y contraseña definitiva.
  Se validan los largos, el formato del correo, el teléfono chileno y la contraseña de 8 a 72
  caracteres con letra y dígito, sin superar los 72 bytes UTF-8.
- Se normalizan nombre, correo y teléfono antes de guardar. Un correo ocupado por cualquier rol
  se rechaza sin modificar esa cuenta. El conductor nace activo y Disponible y puede iniciar
  sesión con la contraseña indicada; para asignarle viajes necesita un vehículo habilitado.
- La lista y el detalle muestran el estado, la cantidad de viajes `ASIGNADO` y el folio `EN_CURSO`,
  si existe. El detalle muestra las habilitaciones y permite editar nombre y teléfono.
- El correo es inmutable. El Administrador puede reemplazar la contraseña sin conocer la actual;
  si omite la nueva, se conserva la anterior. El backend invalida la contraseña temporal al
  reemplazarla. No se devuelven contraseñas ni hashes en la API.
- Los formularios evitan envíos simultáneos, conservan los datos ante fallos y borran la contraseña
  de su estado después de guardar. La lista se recarga al regresar del alta o la edición.

## Código y pruebas

- Android: `FlotaLocalRepository`, `Validaciones.conductor`, `PantallaBaseViewModel`,
  `ContenidoPantalla` y `PantallaT06`.
- Backend: `ConductorService`, `ConductorDTOResponse`, `ConductorRepository` y el resumen agregado
  en `ViajeRepository`. El listado obtiene los resúmenes de todos los conductores en una sola
  consulta, sin cargar sus viajes ni pasajeros.
- `HU15IT`: alta, normalización, duplicados entre roles, credenciales definitivas, edición,
  resúmenes de agenda y permisos.
- `HU15ViewModelTest`: errores por campo, doble envío, reintento, conservación de datos y limpieza
  de contraseña.
- `RepositoriosT06Test`: persistencia en Room, login, edición, duplicados, permisos y asignación con
  un vehículo habilitado.
- `HU15ScreenTest`: alta desde la lista, edición en el detalle y regreso a la lista; errores y
  formulario con texto al 200 %.

## Verificación

Verificado el 22-10-2026:

- Backend: 248 pruebas aprobadas, incluidas las siete de `HU15IT`, contra MariaDB 11.4 temporal.
  El contenedor y su base se eliminaron al terminar. Se usó Gradle 9.8.1 instalado, sin cambiar el wrapper.
- Android: 139 pruebas JVM aprobadas, `lintDebug` correcto y `assembleDemo` compilado.
- API 36: 28 pruebas aprobadas: 23 de `RepositoriosT06Test`, dos de `HU15ScreenTest` y tres de
  `RecorridoT06Test`, incluido el recorrido de las 46 pantallas y los formularios al 200 %.
- API 26 y TalkBack manual: pendientes; el emulador disponible para esta ejecución es API 36.

En `android/`: `./gradlew testDebugUnitTest lintDebug assembleDemo connectedDebugAndroidTest`.
Las instrumentadas deben comprobarse en API 26 y 36. Las pruebas usan Room en memoria.

En `backend/`: `./gradlew test`, exclusivamente con una base MariaDB de pruebas cuyo nombre termine
en `_test`, mediante `TEST_DB_URL`, `TEST_DB_USER` y `TEST_DB_PASSWORD`. La suite limpia ese esquema.

Para cerrar la entrega se requiere además la pasada manual de TalkBack, CI en verde y la revisión
del otro integrante según el README.
