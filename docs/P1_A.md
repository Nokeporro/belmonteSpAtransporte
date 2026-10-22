# P1 del integrante A

Alcance: HU01, HU06, HU02 y HU03. Android funciona sobre Room en la build `demo`;
la integración Retrofit corresponde a T14. Las historias del integrante B conservan su estado previo.

## Comportamiento

- Registro: normaliza correo, teléfono y RUT; rechaza datos inválidos y correo repetido.
  Una empresa nueva aprueba la cuenta; una existente conserva su razón social y deja la cuenta por aprobar.
- Login: las credenciales incorrectas y el Conductor inactivo muestran el mismo error.
  Una cuenta pendiente o rechazada recibe el aviso correspondiente. El backend distingue la contraseña
  temporal y restringe su JWT al perfil propio, cambio de contraseña y cierre de sesión.
  Para salir de ese flujo remoto se vuelve a iniciar sesión con la nueva contraseña.
- Nueva solicitud: origen y destino se eligen con Google Places, también en demo. La búsqueda está
  restringida a Chile y usa español; cada búsqueda comparte su token con la consulta de detalles.
  Se guardan coordenadas con seis decimales. El selector de región/comuna corrige el catálogo y conserva
  la dirección y las coordenadas de Places. No se puede escribir una dirección arbitraria.
- Pasajeros: valida RUT o pasaporte antes de guardar el hash. El backend responde 200 sin revelar el
  nombre cuando el documento ya existe; repetir la asociación a un viaje responde 200 sin duplicarla.
- Agenda: entre dos horas y noventa días desde el reloj inyectado, truncada al minuto; origen y destino
  distintos, pasajeros obligatorios y vuelo normalizado. La demo guarda todo en una transacción.
- Historial: solo solicitudes propias, con pasajeros, fecha/hora, trayecto y estado; filtros combinables
  de fecha y estado en la API. Los formularios bloquean envíos simultáneos y conservan sus datos ante fallos.

## Archivos principales

- Android: `util/Validaciones.kt`, `ui/pantallas/SelectorPlaces.kt`, `PantallaBaseViewModel.kt`,
  `ContenidoPantalla.kt`, `data/local/repository/AuthLocalRepository.kt` y `OperacionesViajeLocal.kt`.
- Backend: `AuthService`, `SolicitanteService`, `ViajeService.crear`, `UbicacionService`,
  `PasajeroService`, `CatalogoService` y `PasswordTemporalFilter`.

Las rutas Android anteriores son relativas a
`android/app/src/main/java/cl/belmontespa/myapplication/`; las del backend, a
`backend/src/main/java/cl/belmontespa/transporte/`.

## Preparación y comprobaciones

1. Configurar `PLACES_API_KEY` en `android/local.properties`, fuera de Git. Habilitar Places API (New)
   y restringir la clave al paquete `cl.belmontespa.myapplication` y los SHA-1 de las firmas utilizadas.
   Sin clave se muestra un aviso y no se reemplaza Places por direcciones ficticias.
2. En `android/`: `./gradlew testDebugUnitTest lintDebug assembleDemo`.
3. Con emulador: `./gradlew connectedDebugAndroidTest`. El recorrido del formulario simula el resultado
   de Places; no consulta Google ni comprueba las credenciales externas.
4. En `backend/`: `./gradlew test`, con `TEST_DB_URL`, `TEST_DB_USER` y `TEST_DB_PASSWORD` apuntando
   exclusivamente a una MariaDB de pruebas cuyo nombre termine en `_test`. La suite limpia ese esquema.

Pruebas añadidas: `P1AIT`, `ValidacionesTest`, `P1ViewModelTest` y escenarios de registro/login y rechazo
atómico de solicitudes inválidas en `RepositoriosT06Test`.

Verificación local del 22-10-2026:

- Backend: 204 pruebas aprobadas contra una MariaDB 11.4 temporal, eliminada al terminar.
  Se usó Gradle 9.8.1 instalado; no se cambió el wrapper 9.7.1 del backend.
  Tras la normalización final del documento se repitieron y aprobaron `P1AIT`, `PasajeroIT` y
  `PasajeroServiceTest` sobre una base temporal nueva.
- Android: 60 pruebas JVM aprobadas, `lintDebug` sin errores y `assembleDemo` correcto.
- API 36: 32 pruebas instrumentadas aprobadas, incluido el recorrido de las 46 variantes y el formulario
  al 200 % de texto. Dos pasadas previas fallaron por un diálogo de ANR de System UI del emulador;
  la pasada completa quedó en verde al cerrarlo. No se modificó la app para eludir esos fallos.

La implementación de las cuatro historias está marcada como lista en el README y el backlog.
Para cerrar la entrega quedan la comprobación con Places real, la revisión en API 26,
la pasada de accesibilidad, CI en verde, la aprobación del otro integrante y la fusión en `main`
exigidas por el README.

Referencia del SDK: [Autocomplete (New)](https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete).
