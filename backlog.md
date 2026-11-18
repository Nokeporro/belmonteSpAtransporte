# Backlog priorizado

**Proyecto:** Aplicación de Belmonte SpA · **Fecha:** 14-11-2026 · **Etapa 1 (Prueba 2):** entregada el
28-10-2026 · **Etapa 2:** backend completo el sábado 14-11-2026.

Fuente: [README.md](README.md) (la especificación; si este backlog difiere, gana el README). El
estado del backend sale de revisar `backend/` en esta rama.

## Cómo leer este backlog

- **Orden (#):** cada integrante toma su siguiente ítem en orden. Los ítems de A y de B avanzan en
  paralelo.
- **Prioridad:**
  - **P0:** base común, que bloquea a los demás.
  - **P1:** núcleo de la demostración.
  - **P2:** resto de lo de prioridad Alta.
  - **P3:** prioridad Media.
  - **P4:** prioridad Baja.
  - **E2:** Etapa 2 (Unidad 3).

  La columna "Prio. HU" es la prioridad de la historia en el README.
- **Criterios de aceptación:** los de cada HU en
  [Historias de usuario y backlog](README.md#historias-de-usuario-y-backlog); no se copian aquí. Las
  tareas `T` llevan su "listo cuando".
- **Una HU en la Etapa 1** se entrega con:
  - sus pantallas sobre el Repository (Room), recorribles en la [build `demo`](README.md#build-demo);
  - su parte de backend con pruebas.

  Las llamadas remotas del Repository (Retrofit) son T14, en la Etapa 2, que empieza el 12-11.
- **Estado del backend:**
  - **Hecha:** la ruta existe con su controlador y su regla, y tiene pruebas.
  - **Falta:** no existe.

  Lo que queda pendiente está en [Pendiente del backend](#pendiente-del-backend-etapa-2).
- **Android:** la app vive en `android/` y la build `demo` corre completa sobre Room, con las 46 pantallas
  (MVVM con Repository simple, sin Hilt, `view/` y `viewmodel/`). Cada pantalla tiene su ViewModel,
  salvo "Inicio de aplicación" y "Abordaje registrado", que no tienen estado propio. Las llamadas
  remotas del Repository (T14, desde el 12-11) y la cola sin conexión (HU11) están hechas; el push es la
  Etapa 2. La rama `android` es obsoleta y no se usa.

## Resumen

| | A | B | Total |
|---|---|---|---|
| Etapa 1 (SP) | 45 | 36 | 81 |
| Etapa 2 (SP) | 8 | 13 | 21 |
| Total (SP) | 53 | 49 | 102 |

**Backend:**
- **Compilación:** compila (`./gradlew compileJava compileTestJava`).
- **Pruebas:** 422 pruebas aprobadas, sin fallos ni omisiones, el 14-11 con `./gradlew test --offline`,
  incluidas las IT contra una MariaDB 11.4 temporal en el puerto 13307 (`belmonte_test`).
- **Rutas:** las 48 del README tienen controlador, incluido `POST /auth/token-fcm` (HU14 backend, 14-11).
  Ya no existen las 3 rutas que el README no admite (`POST /usuarios`, `PUT /solicitantes/{correo}` y
  `POST /empresas`).

| Rutas del README | Hechas | Faltan |
|---|---|---|
| 48 | 48 | 0 |

**Meta del 75 %:** al menos 36 de las 48 rutas del README (36 es el 75 % de 48). Hoy existen las 48,
incluida la de la Etapa 2 (HU14). **Meta de la Etapa 2:** las 48 rutas, con sus pruebas, el
sábado 14-11; las IT y los avisos del backend ya están verificados contra MariaDB. El job `backend` de CI ya está en `ci.yml`; falta verlo en verde en GitHub para cerrar T15. El
[mapa de rutas](#rutas-y-75-) indica a qué ítem pertenece cada una.

## Criterios de priorización

1. **Fecha:** la Etapa 1 (entregada el 28-10) iba antes que la Etapa 2.
2. **Dependencias:** la base común va primero, y una HU va después de las que necesita.
3. **Lo que evalúa la Prueba 2:**
   - UI al 100 %, navegable y coherente;
   - base de datos local;
   - lectura de QR con la cámara;
   - video;
   - 75 % del backend.

   Además, cada integrante explica y modifica en vivo su propio código Android.
4. **Prioridad de la HU en el README.** Única excepción: HU03 (Media) sube a P1, porque "Mis
   solicitudes" es la pantalla de inicio del Solicitante.

**Línea de corte:** si el tiempo no alcanza, se recorta primero P4 y luego P3. Esas pantallas igual
quedan navegables sobre `DatosDemo` gracias a T06, así que la exigencia de "UI al 100 %" se cumple
aunque falten reglas. En la Etapa 2 lo recortable es lo último de T14 (Pago y Flota); el backend completo
del 14-11 no se recorta.

**Hitos de la Etapa 2** (la Etapa 1 se entregó el 28-10 con T11, T12 y T13 en Listo):

| Fecha | Hito |
|---|---|
| Jueves 12-11 | B: el QR del Android vuelve a `{patente}:{codigo}` (HU29). A: `POST /auth/token-fcm` (HU14). Ambos: base de T14 (Retrofit/OkHttp, `ApiClient`, interceptor, `ErrorMapper`) y `modoLocal` en los Repository. |
| Viernes 13-11 | A: push de V1, de cuenta pendiente y de V3/V4; T14 de Auth, Catálogo y Cuenta. B: `database/demo.sql` (T16) y job `backend` de CI (T15); T14 de Viaje y Abordaje. |
| Sábado 14-11 (hoy) | Backend completo: 48 de 48 rutas, ITs corridas contra MariaDB (compose en el 3307) y CI en verde. T14 de Pago y Flota si alcanza. |

## Etapa 1: Prueba 2

| # | ID | Ítem | Resp. | Prio. | Prio. HU | SP | Depende de | Backend | Estado |
|---|---|---|---|---|---|---|---|---|---|
| 1 | T01 | Proyecto `android/`, versiones y CI | B | P0 | — | — | — | — | Listo |
| 2 | T02 | Tema, componentes y textos | A | P0 | — | — | T01 | — | Listo |
| 3 | T03 | Navegación por rol y sesión | A | P0 | — | — | T01 | — | Listo |
| 4 | T04 | Room v1 y DAO | B | P0 | — | — | T01 | — | Listo |
| 5 | T05 | `DatosDemo`, los Repository y build `demo` | B | P0 | — | — | T04 | — | Listo |
| 6 | T06 | Esqueleto navegable de las 46 pantallas, cada una con su ViewModel | A y B | P0 | — | — | T02, T03, T05 | — | Listo |
| 7 | T07 | Backend: migración de cuentas, flota y pagos | B | P0 | — | — | — | Hecho | Listo |
| 8 | T08 | Backend: migración de viajes y abordaje | B | P0 | — | — | T07 | Hecho | Listo |
| 9 | T09 | Backend: validaciones, errores y rutas que sobran | B | P0 | — | — | — | Hecho | Listo |
| 10 | T10 | Hora de Chile y orden de listas (backend y app) | B | P0 | — | — | T05 | Hecho | Listo |
| 11 | HU01 | Solicitante: registro y login | A | P1 | Alta | 3 | T03, T07 | Hecho; push de cuenta pendiente integrado el 14-11 | Listo |
| 12 | HU06 | Conductor: login con credenciales del Administrador | A | P1 | Alta | 2 | HU01 | En HU01 | Listo |
| 13 | HU02 | Solicitante: nueva solicitud con Google Places | A | P1 | Alta | 5 | T05, T09 | Hecho; push de V1 integrado el 14-11 | Listo; Places en Android el 15-11, sin clave real: sin `PLACES_API_KEY` y en `demo` sigue la lista ficticia (ver el [riesgo](#riesgos)) |
| 14 | HU03 | Solicitante: historial y estado | A | P1 | Media | 3 | HU02 | Hecho | Listo |
| 15 | HU04 | Administrador: pendientes | B | P1 | Alta | 3 | T05, T10 | Hecho | Listo |
| 16 | HU05 | Administrador: asignar | B | P1 | Alta | 5 | HU04, T08 | Hecho | Listo |
| 17 | HU07 | Conductor: viajes del día en tiempo real | B | P1 | Alta | 3 | T05, T10 | En HU04 | Listo |
| 18 | HU08 | Conductor: detalle del viaje | B | P1 | Alta | 3 | HU07 | Hecho | Listo |
| 19 | HU09 | Conductor: En Curso y Finalizado | B | P1 | Alta | 3 | HU08, T10 | Hecho | Listo |
| 20 | HU10 | Conductor: ruta en Google Maps o Waze | B | P1 | Alta | 3 | HU08 | Sin backend | Listo |
| 21 | HU29 | Pasajero: abordar con el código QR | B | P1 | Alta | 5 | T05, T08 | Hecho | Listo; rehacer el QR con código en el Android (12-11) |
| 22 | HU30 | Conductor: arqueo de abordaje | B | P1 | Alta | 3 | HU29, HU08 | En HU08 | Listo |
| 23 | HU15 | Administrador: alta de Conductor | A | P2 | Alta | 3 | T07 | Hecho | Listo |
| 24 | HU16 | Solicitante: cancelar | A | P2 | Alta | 3 | HU03, T07 | Hecho | Listo |
| 25 | HU24 | Administrador: cancelar un viaje | B | P2 | Alta | 3 | HU08, HU16 | En HU16 | Listo |
| 26 | HU17 | Administrador: alta de vehículo | A | P3 | Media | 3 | T07, T08 | Hecho | Listo |
| 27 | HU18 | Administrador: habilitaciones | A | P3 | Media | 2 | HU15, HU17 | Hecho | Listo |
| 28 | HU19 | Administrador: registrar pago | A | P3 | Media | 5 | T07 | Hecho | Listo |
| 29 | HU20 | Administrador: estado del pago | A | P3 | Media | 3 | HU19 | Hecho | Listo |
| 30 | HU22 | Administrador: restablecer la contraseña de un Solicitante o Administrador | A | P3 | Media | 3 | T07 | Hecho | Listo (la temporal la genera el Administrador y vence a las 24 h) |
| 31 | HU27 | Administrador: aprobar solicitantes | A | P3 | Media | 3 | HU01 | Hecho | Listo |
| 32 | HU26 | Administrador: dar de baja | A | P3 | Media | 3 | HU15, HU17 | Hecho | Listo |
| 33 | HU23 | Administrador: reasignar | B | P3 | Media | 3 | HU05 | En HU05 | Listo |
| 34 | HU25 | Conductor: rechazar una asignación | B | P3 | Media | 2 | HU08, T08 | Hecho | Listo |
| 35 | HU21 | Solicitante: estado de pago | A | P4 | Baja | 2 | HU19 | En HU19 | Listo |
| 36 | HU28 | Administrador: registrar otro Administrador | A | P4 | Baja | 2 | HU22 | Hecho | Listo |
| 37 | T11 | Video de hasta 7 minutos sin audio | A y B | Cierre | — | — | T06, P1 | — | Listo (28-10) |
| 38 | T12 | Ensayo de la evaluación | A y B | Cierre | — | — | T11 | — | Listo (28-10) |
| 39 | T13 | Entrega del 28-10 | A y B | Cierre | — | — | T11 | — | Listo (28-10) |

SP de la Etapa 1: A, 45 (P1 13, P2 6, P3 22, P4 4); B, 36 (P1 28, P2 3, P3 5).

**Listo cuando, por tarea:**
- **T01:** se cumple la aceptación de la base de [Tecnologías y código](README.md#tecnologías-y-código):
  - sincroniza con JDK 21;
  - pasan `lintDebug`, `testDebugUnitTest`, `assembleDebug` y `assembleDemo`;
  - la app abre en API 26 y 36.

  Además, el job `android` de CI está en verde.
- **T02:** existen los roles de color, los chips, la tipografía, `strings.xml` y los formatos de
  [Textos y tema](README.md#textos-y-tema), con `FormatosTest`.
- **T03:**
  - `MainActivity` con un flujo de acceso y uno por rol;
  - la sesión en DataStore y el inicio según la sesión guardada;
  - el cierre de sesión local;
  - `NavegacionTest` base.
- **T04:** existen las 8 tablas de [Base de datos local](README.md#base-de-datos-local), con
  sus DAO y `DaosTest`.
- **T05:**
  - los 7 Repository como clases sobre los DAO, sin interfaces;
  - `AppDatabase.getDatabase()` y los ViewModel creando sus Repository, sin Hilt, y `MODO_LOCAL` en
    `BuildConfig`;
  - la semilla `DatosDemo`;
  - el aviso en el Login;
  - `./gradlew installDemo` funcionando.
- **T06:** las 46 pantallas de los [mockups](mockups/wireframes-belmonte.html) se recorren con datos
  de `DatosDemo`. Cada integrante hace las pantallas de sus HU, aunque aún no tengan todas las
  reglas. Cada pantalla con estado tiene su ViewModel en `view/<rol>/` y `viewmodel/<rol>/` (no la tienen "Inicio de aplicación" ni "Abordaje registrado"). En "Nueva solicitud" el
  origen y el destino se eligen de una lista de lugares ficticios, porque Google Places llega en T14; esa lista sigue en la `demo` y en cualquier build sin `PLACES_API_KEY`.
- **T07 y T08:**
  - las migraciones de cuentas, flota, pagos, viajes y abordaje se aplican sobre una base existente y
    sobre una vacía;
  - las entidades JPA validan (`ddl-auto=validate`);
  - existe `MigracionesIT`.
- **T09 y T10:** validaciones, errores, hora de Chile y orden de las listas, con sus pruebas. En la app
  incluye `ValidacionesTest` (parte de T09) y `RetrasoTest` (T10).
- **T11:** el video muestra:
  - navegación y diseño;
  - Room: crear, cambiar y ver persistir;
  - la lectura de QR con la cámara;
  - el flujo del viaje.

  Dura 7 minutos o menos y no tiene audio.
- **HU29 (rehacer el QR, 12-11):** el Android vuelve al formato del README
  ([Abordaje con código QR](README.md#abordaje-con-código-qr)): lee `BELMONTE-ABORDAJE:{patente}:{codigo}`,
  «Ingresar código» pide patente y código, y `POST /abordajes` envía ambos. Desde el commit `6b38cda` el
  Android lee solo la patente y el backend, que exige `codigoAbordaje`, rechazaría el abordaje.
- **T12:** cada integrante, sin apuntes ni IA:
  - explica los paquetes, las dependencias, la navegación, Room y el QR de su parte;
  - ubica en qué archivo y función está cada funcionalidad suya;
  - hace un cambio pequeño en vivo.
- **T13:** se envían el código del backend, el código Android y el video antes de las 19:00, desde
  `main` congelado.

## Etapa 2: Unidad 3

| # | ID | Ítem | Resp. | Prio. | Prio. HU | SP | Depende de | Backend | Estado |
|---|---|---|---|---|---|---|---|---|---|
| 40 | T14 | Llamadas remotas en los Repository, `ErrorMapper` e interceptor | A y B | E2 | — | — | Etapa 1 | — | Hecho en Android el 12-11 (los 7 Repository, `ErrorMapper`, interceptor y pruebas JVM); probado contra el backend y en el emulador el 14-11, salvo lo que indica el [riesgo](#riesgos) (ver el [reparto](#reparto-de-la-etapa-2-12-al-14-11)) |
| 41 | HU14 | Todos: token de notificaciones | A | E2 | Alta | 3 | T14 | Hecho y probado el 14-11 | Backend listo; Android integra token, onNewToken, reintentos y permiso (510 JVM y 3 pruebas de HU14 en API 36); falta crear el proyecto Firebase, añadir google-services.json y probar un aviso real |
| 42 | HU13 | Conductor: push de asignación | B | E2 | Alta | 5 | HU14 | Hecho; integrado con HU12 el 15-11 | Aviso en primer plano, toque que abre el viaje según `data.tipo` y rol, recarga con la app abierta y primer detalle sin «No encontrado». La base común espera una sesión normal y preserva el aviso durante recreación; falta la entrega real de FCM, ver el [riesgo](#riesgos) |
| 43 | HU12 | Solicitante: push de estado | A | E2 | Media | 5 | HU14 | Hecho; integrado con HU13 el 15-11 | Android presenta el aviso en primer plano y abre el detalle por sesión y rol; consulta el estado al servidor. La validación y la navegación se unificaron con HU13. Pendiente la entrega real con Firebase |
| 44 | HU11 | Conductor: uso sin conexión | B | E2 | Media | 8 | T14 | Sin backend | Hecho el 14-11 (Iniciar y Finalizar se encolan solo ante un fallo de red y se envían con WorkManager; chip, banner, conflicto con «Descartar cambio», cierre de sesión y cambio de cuenta con pendientes); recorrido en el emulador contra el backend, ver el [riesgo](#riesgos) |
| 45 | T15 | Backend completo y pruebas al día | A y B | E2 | — | — | Etapa 1 | 48 rutas y suite local verificadas | 422 pruebas aprobadas el 14-11; job `backend` agregado a `ci.yml` el 15-11 y probado en local con sus mismos comandos; falta verlo en verde en GitHub (B), ver el [riesgo](#riesgos) |
| 46 | T16 | `database/demo.sql` | B | E2 | — | — | Etapa 1 | — | Hecho el 14-11 (las mismas cuentas, vehículos, códigos de abordaje, pasajeros, viajes y pagos de `DatosDemo`, con folios TR-1001 a TR-1011; probado en `belmonte_test` contra el backend, ver el [riesgo](#riesgos)) |
| 47 | T17 | `FlujosE2ETest` | A y B | E2 | — | — | T14, T16 | — | Hecho el 14-11 (5 pruebas sobre la interfaz real y el backend con `demo.sql` recién cargado: Solicitante, Administrador, Pasajero y Conductor sin conexión; ver el [riesgo](#riesgos) para ejecutarla) |
| 48 | T18 | Pasada completa con TalkBack | A y B | E2 | — | — | T14 | — | En curso el 17-11: auditoría estática sin advertencias de accesibilidad de lint; Login revisado con TalkBack y texto al 200 % (campos, botones y alternativa de abordaje sin truncarse). El resumen de arqueo queda como un único elemento para lector de pantalla; `ArqueoAbordajeTest` pasa 3/3 en API 36. Falta la pasada manual completa de las demás pantallas |
| 49 | T19 | APK release `v1.0.0` en GitHub Releases | A | E2 | — | — | T17, T18 | — | Después de la ventana |

SP de la Etapa 2: A, 8; B, 13. La «ventana» es el 12 al 14-11. T16 pasa a depender solo de la Etapa 1:
`demo.sql` necesita las migraciones, no que el backend esté completo.

**Listo cuando, por tarea:**
- **T14:**
  - cada Repository llama a la API en modo remoto (`MODO_LOCAL` falso), según
    [Sesión y conectividad](README.md#sesión-y-conectividad);
  - `ErrorMapper` existe con su `ErrorMapperTest`;
  - se agregan las dependencias que hoy no se usan y por eso no están declaradas: Retrofit y OkHttp,
    WorkManager, Firebase (BoM, `firebase-messaging` y el plugin `google-services`) y
    Google Places, que reemplaza la lista de lugares ficticios de "Nueva solicitud" (HU02). Cada una se
    declara cuando tiene un `import`, no antes ([Tecnologías y código](README.md#tecnologías-y-código)), así
    que en la ventana del 12 al 14-11 solo entran Retrofit, OkHttp y el conversor de `kotlinx-serialization`;
  - cada Repository recibe `modoLocal` por constructor (por defecto `BuildConfig.MODO_LOCAL`) y las
    pruebas con Room pasan `true`: `testDebugUnitTest` y las instrumentadas corren en `debug`, donde
    `MODO_LOCAL` es falso.

  Cada integrante hace las llamadas remotas de sus funcionalidades, y A hace además `ErrorMapper` y el
  interceptor.
- **HU11:** incluye `SincronizacionWorkerTest`.
- **T15:** las 48 rutas cumplen el README y todas las pruebas de [Pruebas](README.md#pruebas) pasan. Incluye
  el job `backend` de `.github/workflows/ci.yml` que define
  [Integración continua](README.md#integración-continua), con MariaDB, en verde.
- **T16:** el contenido es el de [Stack y entornos](README.md#stack-y-entornos). Hay que leer antes las
  migraciones `V2` a `V5`: `dni_hash` es un HMAC-SHA256 con `DNI_SECRET` en Base64url sin relleno, los folios
  salen de `seq_folio_viaje` y los triggers validan estados y habilitaciones, así que importa el orden de
  inserción. Las contraseñas llevan hash BCrypt.

## Reparto de la Etapa 2 (12 al 14-11)

Cada integrante implementa lo de sus funcionalidades, como en el README
([Plan de trabajo](README.md#plan-de-trabajo)), para poder explicar y modificar su propio código Android.
Quién es A y quién B lo acuerda el equipo (README). Para no pisarse, no se edita el mismo archivo el mismo
día: `ViajeService` lo toca solo A el 13-11, y `ci.yml` y `demo.sql` solo B.

| Resp. | Ítem | Día | Entregable | Depende de |
|---|---|---|---|---|
| A | HU14 backend: `POST /auth/token-fcm` | 12-11 | Implementado el 14-11: `TokenFcmDTORequest`, método en `AuthController`, `AuthService.registrarTokenFcm` (quita el token a cualquier otro usuario, con la misma lógica de `login`), regla `authenticated()` en `SecurityConfig`; `TokenFcmDTORequestTest`, `AuthServiceTest` y casos en `SeguridadIT` | — |
| B | HU29 Android: QR con código | 12-11 | `CodigoAbordaje(patente, codigo)`, `AbordajeRepository` compara `codigo_abordaje`, ingreso manual con patente y código, `strings.xml` y sus pruebas | — |
| A y B | T14 base | 12-11 | Retrofit y OkHttp en el catálogo, `ApiClient`, `AuthInterceptor`, `ErrorMapper` + `ErrorMapperTest` (A); `modoLocal` en los 7 Repository (B) | — |
| A | HU12 backend: push de V3 y V4 | 13-11 | Implementado el 14-11: `AvisosDeViaje.estadoDelViaje` (título «Viaje {folio}», «Estado: En curso/Finalizado», `data.tipo=VIAJE`) usado en `ViajeService`; `NotificacionesIT` verifica el destinatario, el token actualizado, los cambios rechazados y el rollback | HU14 |
| A | HU02 y HU01 backend: push de V1 y de cuenta pendiente | 13-11 | Hecho y probado el 14-11: «Nueva solicitud» (`data.tipo=VIAJE` y folio) y «Cuenta por aprobar» (`data.tipo=CUENTAS`) a los Administradores con token; `AvisosAdministracion` comparte la difusión con V8 y `SolicitanteService` recibe `FirebaseClient` y `AdministradorRepository` | HU14 |
| A | Pruebas de push | 13-11 | Aislamiento implementado y verificado el 14-11: `IntegrationTest.iniciarApi` deja `token_fcm` en NULL para que los dispositivos de otra prueba no alteren los conteos; pasan `NotificacionesAdministracionIT`, `NotificacionesIT`, `HU16IT` y `HU20IT` dentro de las 422 pruebas | HU12, HU02 |
| A | T14: Auth, Catálogo y Cuenta | 13-11 | Repository en modo remoto: login (`expiraEn = expiresIn × 1000`, `cambioPasswordRequerido`), registro, recuperación, logout, `token-fcm`, catálogos, perfil y aprobación de cuentas | T14 base |
| B | T16: `database/demo.sql` | 13-11 | El script de [Stack y entornos](README.md#stack-y-entornos), con los 4 códigos de abordaje fijos de la semilla Android | — |
| B | T15: job `backend` de CI | 13-11 | Job `backend` con `mariadb:11.4`, `./gradlew test` y subida de `build/reports/tests`, con las versiones de acciones del job `android` | — |
| B | T14: Viaje y Abordaje | 13-11 | Repository en modo remoto en línea y sin cola: lecturas con `refrescar*`, creación en 3 pasos, asignar, cancelar, rechazar, cambiar estado y `POST /abordajes` con patente y código | T14 base, HU29 |
| A y B | T15: backend completo | 14-11 | `./gradlew test` completo contra el compose (3307), con las IT que rompan los push corregidas; 48 de 48 rutas | HU12, HU02, T16 |
| A | T14: Pago y Flota | 14-11 | Repository en modo remoto (3 rutas de pagos y 11 de flota); es lo primero que se recorta | T14 base |

Si A termina antes, toma Pago y Catálogo; si B termina antes, apoya Flota.

**Siguiente integración:** HU14 Android ya tiene el servicio FCM para `onNewToken`, token persistido
y enviado en el login, registro con WorkManager y permiso solicitado una sola vez tras el login normal.
El plugin `google-services` se aplica únicamente si existe `android/app/google-services.json`; sin ese
archivo no se activa Firebase y `demo` lo mantiene desactivado siempre. El usuario todavía no tiene un
proyecto Firebase: falta crearlo y verificar la entrega real. No se agregaron credenciales ficticias al código.

**HU13 Android (14-11):** `BelmonteFirebaseService.onMessageReceived` (solo con la app en primer plano) publica
el aviso en el canal `avisos` con `PresentadorDeAvisos` y avisa a las pantallas con `AvisosEntrantes`; con la app
en segundo plano lo muestra FCM. En ambos casos el toque abre `MainActivity` (`singleTop`) con `tipo` y `folio`
como extras y `BelmonteApp` navega con `destinoDeAviso` tras comprobar sesión y rol (`VIAJE` al detalle, `AGENDA`
a Mis viajes, `CUENTAS` a Cuentas por aprobar); con sesión temporal o sin sesión espera al login normal, como HU12.
`DetalleViajeViewModel` ya no muestra «No encontrado» mientras llega el primer `GET /viajes/{folio}`.

**HU02 Android, Google Places (15-11):** con `PLACES_API_KEY` (en `android/local.properties`, o `-PPLACES_API_KEY` o la
variable de entorno del mismo nombre) «Nueva solicitud» abre el widget `PlaceAutocomplete` del Places SDK 6.0.2, restringido
a `CL` y en español (`es-CL`), con un token de sesión por búsqueda que el widget entrega y `LugaresPlaces` reutiliza en el
`FetchPlace` posterior. `LugarCrudo.aUbicacion()` arma la `Ubicacion` como dice el README (nombre del establecimiento o calle y
número, `administrative_area_level_3` o `locality`, `administrative_area_level_1`). Si falta la comuna o el servidor rechaza
`origen.comuna` o `destino.comuna`, la pantalla muestra el selector de región y comuna de `GET /catalogos` y solo corrige esos
dos campos; mientras falte, «Guardar» queda deshabilitado y `guardar()` no envía. Sin clave, o en la build `demo`, el campo es
el selector de lugares ficticios de siempre; `EntornoPruebas` y `EntornoE2E` lo fijan así, para que las pruebas no dependan de
la clave. El job de CI no la recibe todavía.

**Avance de A del 15-11:** HU12 Android integra la presentación en primer plano y la apertura del
detalle desde los extras del push, con la app abierta o al iniciarla. La base común admite `VIAJE`,
`AGENDA` y `CUENTAS` según el rol actual, y quedó integrada con la navegación y validación de HU13.
El aviso espera el login normal, se conserva durante la recreación y no salta el cambio obligatorio
de contraseña. Los detalles del Solicitante y del Administrador refrescan desde el servidor y ocultan
el contenido anterior ante un error de acceso. Pasan 557 pruebas JVM. `HU12AvisosTest` (6) volvió a
pasar en API 36 con un identificador de aplicación aislado para conservar los datos de la app
instalada. Queda pendiente una corrida conjunta limpia de todas las instrumentadas: el emulador dejó
el runner sin resumen al ejecutar el grupo completo. Falta verificar la entrega real con Firebase y
validar API 26; no se repitió toda la suite instrumentada ni las E2E.

**Pendientes:** configurar Firebase y verificar la entrega real de HU12/HU13; configurar los secretos de CI,
probar Google Places con una clave real, verificar el job backend en GitHub, T18 y T19.
- **T19:** según [Compilación y distribución](README.md#compilación-y-distribución).

## Pendiente del backend (Etapa 2)

Rutas de archivo relativas a `backend/src/main/java/cl/belmontespa/transporte/`. Las reglas están en el README
([Notificaciones](README.md#notificaciones) y [Autenticación](README.md#autenticación)).

- **HU14 backend implementado el 14-11:** `POST /auth/token-fcm` registra el token del usuario del JWT;
  exige un token no vacío de hasta 255 caracteres y lo quita de cualquier otro usuario. Conserva el
  bloqueo de sesiones temporales. Se agregaron pruebas de DTO, servicio y autorización en `SeguridadIT`.
  Pasaron las 8 unitarias nuevas y los 21 casos de `SeguridadIT`, incluidos en la suite actual de 422
  pruebas contra MariaDB. HU14 Android integra el registro y el permiso, con 510 pruebas JVM aprobadas;
  sus 3 pruebas instrumentadas pasan en API 36 con identificador separado: login con token, actualización
  por `/auth/token-fcm`, conservación del token al cerrar sesión y marca de permiso persistida.
  Se verificó el plugin de Google con una configuración ficticia temporal, retirada al terminar; no se
  creó un proyecto ni se enviaron mensajes reales. Faltan el proyecto Firebase, su configuración, la
  cuenta de servicio del backend y la prueba de entrega real. API 26 sigue pendiente de validación.
- **HU12 backend implementado el 14-11:** V3 y V4 avisan solo al Solicitante con el título "Viaje {folio}",
  "Estado: En curso" o "Estado: Finalizado", y `data` con `tipo = VIAJE` y `folio`. Se reutiliza el envío
  después del commit; los cambios rechazados o revertidos no envían push. Pasaron las 11 pruebas de
  `AvisosDeViajeTest` y las 18 de `NotificacionesIT`, dentro de la suite de 422 (también `HU16IT` y `HU20IT`).
  Android integra recepción en primer plano y apertura del detalle desde el 15-11; falta configurar
  Firebase y probar la entrega real.
- **HU02 y HU01 backend implementados el 14-11:** "Nueva solicitud" (V1, `data.tipo = VIAJE` y folio) y
  "Cuenta por aprobar" (`data.tipo = CUENTAS`) se difunden a todos los Administradores con token,
  mediante `AvisosAdministracion`, compartido con V8. Una cuenta aprobada automáticamente no genera
  el aviso de revisión; los registros rechazados o revertidos no notifican. Pasaron las 6 pruebas de
  `NotificacionesAdministracionIT` y las 2 de `AvisosAdministracionTest`, además del formato de V1 en
  `AvisosDeViajeTest`, dentro de la suite de 422. La base Android ya recibe estos tipos y abre su
  destino según el rol; falta verificar la entrega con Firebase real.
- **Job `backend` de CI (T15, B, 13-11):** agregado a `.github/workflows/ci.yml` el 15-11 (ver el [riesgo](#riesgos)).
- **Pruebas de integración (T15, A y B, 14-11):** `NotificacionesIT` verifica las etiquetas en español
  de V3/V4 y `data.tipo`; `NotificacionesAdministracionIT` cubre V1 y cuentas pendientes, varios
  Administradores, ausencia de dispositivos, otros roles y rollback. El token de todos los usuarios se
  limpia antes de cada prueba para aislar los conteos.
- **Ya llevan `data.tipo`:** los push de asignación y reasignación (`AvisosDeViaje`, con `VIAJE` y `AGENDA`),
  de nueva solicitud (V1), de cuenta pendiente (`CUENTAS`), de inicio y finalización (V3/V4), de rechazo
  (V8), de cancelación y de pago.

## Rutas y 75 %

Estado de cada ruta del README e ítem que la cumple. Las rutas son relativas a `/api/v1/`.

| Ruta | Estado | Ítem |
|---|---|---|
| `POST /auth/login` | Hecha | HU01 |
| `POST /auth/logout` | Hecha | HU14 |
| `POST /auth/token-fcm` | Hecha | HU14 (E2) |
| `POST /auth/recuperar-password` | Hecha | HU22 |
| `GET /catalogos` | Hecha | HU02 |
| `GET /usuarios` | Hecha | HU22 |
| `GET /usuarios/{correo}` | Hecha | HU22 |
| `PUT /usuarios/{correo}` | Hecha | HU22 |
| `GET /administradores` | Hecha | HU28 |
| `GET /administradores/{correo}` | Hecha | HU28 |
| `POST /administradores` | Hecha | HU28 |
| `GET /conductores` | Hecha | HU15 |
| `GET /conductores/{correo}` | Hecha | HU15 |
| `POST /conductores` | Hecha | HU15 |
| `PUT /conductores/{correo}` | Hecha | HU15 |
| `POST /conductores/{correo}/activo` | Hecha | HU26 |
| `GET /solicitantes` | Hecha | HU27 |
| `GET /solicitantes/{correo}` | Hecha | HU01 |
| `POST /solicitantes` | Hecha | HU01 |
| `POST /solicitantes/{correo}/aprobar` | Hecha | HU27 |
| `POST /solicitantes/{correo}/rechazar` | Hecha | HU27 |
| `GET /empresas` | Hecha | HU27 |
| `GET /empresas/{rut}` | Hecha | HU27 |
| `GET /moviles` | Hecha | HU17 |
| `GET /moviles/{patente}` | Hecha | HU17 |
| `POST /moviles` | Hecha | HU17 |
| `POST /moviles/{patente}/activo` | Hecha | HU26 |
| `GET /habilitaciones-movil-conductor` | Hecha | HU18 |
| `GET /habilitaciones-movil-conductor/{patente}/{correo}` | Hecha | HU18 |
| `POST /habilitaciones-movil-conductor` | Hecha | HU18 |
| `GET /pagos` | Hecha | HU19 |
| `GET /pagos/{hash}` | Hecha | HU19 |
| `POST /pagos` | Hecha | HU19 |
| `POST /pagos/{hash}/estado` | Hecha | HU20 |
| `GET /pasajeros` | Hecha | HU02 |
| `GET /pasajeros/{dni_hash}` | Hecha | HU02 |
| `POST /pasajeros` | Hecha | HU02 |
| `GET /pasajeros-viaje` | Hecha | HU02 |
| `GET /pasajeros-viaje/{folio}/{dni_hash}` | Hecha | HU02 |
| `POST /pasajeros-viaje` | Hecha | HU02 |
| `GET /viajes` | Hecha | HU04 |
| `GET /viajes/{folio}` | Hecha | HU08 |
| `POST /viajes` | Hecha | HU02 |
| `POST /viajes/{folio}/asignar` | Hecha | HU05 |
| `POST /viajes/{folio}/estado` | Hecha | HU09 |
| `POST /viajes/{folio}/cancelar` | Hecha | HU16 (V5) y HU24 (V7) |
| `POST /viajes/{folio}/rechazar` | Hecha | HU25 |
| `POST /abordajes` | Hecha | HU29 |

**Totales:**
- **Por estado:** 48 hechas y 0 faltantes (HU14 backend implementado el 14-11). El estado sale de comparar cada ruta de la
  tabla con los `@…Mapping` de los controladores, incluidas las heredadas de `LecturaController` y
  `CrudController`; las 422 pruebas actuales, incluidas las IT y los avisos a administración, pasan
  contra MariaDB. El job `backend` de CI ya existe; su cierre depende de verlo en verde en GitHub.
- **Etapa 1:** tiene 47 rutas (todas menos `POST /auth/token-fcm`); hay que dejar al menos 36 de las 48
  cumpliendo el README con pruebas.
- **Etapa 2:** las 48 tienen controlador y pruebas locales aprobadas el 14-11; el job `backend` de CI está agregado (15-11); falta verlo en verde en GitHub para cerrar T15.
- **Por integrante:** las HU de A tienen 42 rutas y las de B, 6. Las de B son las de reglas más
  complejas (V2, V3, V8 y el abordaje).

## Riesgos

| Riesgo | Mitigación |
|---|---|
| **Notificaciones Android integradas localmente el 15-11:** pasan 557 JVM; `HU12AvisosTest` (6) pasa de nuevo en API 36 con un identificador aislado. La recepción y el toque se prueban sin un proyecto Firebase; el intento de corrida conjunta quedó sin resumen por el emulador, por lo que no confirma entrega desde la nube. | Crear el proyecto, configurar Android y la cuenta de servicio del backend del mismo proyecto, y probar asignación, inicio y fin con la app abierta y cerrada. Repetir las instrumentadas en una corrida limpia y validar también API 26 antes del release. |
| **Plazo del 14-11:** quedan dos días (12 y 13-11, más el 14 para cerrar) para el backend completo y T14. | La Etapa 1 está entregada (28-10). Lo recortable es T14 de Pago y Flota; el backend completo no se recorta. Ver el [reparto](#reparto-de-la-etapa-2-12-al-14-11). |
| **Backend desigual:** A tiene 42 de las 48 rutas y la mayor parte de P3, y en la ventana concentra `token-fcm` y los push. | B hace la base del backend (T07 a T10) y en la Etapa 2 `demo.sql`, el job de CI y las IT. Si A se atrasa, B toma los push de V1 y de cuenta pendiente, previo acuerdo, sin afectar que cada uno explique su Android. |
| **Pruebas verificadas el 14-11:** las 422 del backend pasan sin fallos ni omisiones, incluidas las IT contra MariaDB 11.4 temporal en 13307 y los avisos de HU02/HU01. Android (15-11): 548 JVM y las 324 instrumentadas en API 37 aprobadas (incluidas las 3 de HU14, las 6 de HU13 y las 3 de Places; las 5 E2E de T17 se omiten sin `-e e2e true`). | Completar el job de CI del backend (T15), verificar Firebase real y repetir las pruebas necesarias antes del release, incluida API 26. Las instrumentadas normales comparten `applicationId` con la demo. Sin red, `connectedDebugAndroidTest` falla porque falta `utp:gradle-work-action` en caché: usar `installDebug installDebugAndroidTest` y `adb shell am instrument`. |
| **Divergencia del QR (resuelta el 12-11):** desde `6b38cda` el Android leía solo la patente; el README y el backend exigen `{patente}:{codigo}`. | El Android volvió a `BELMONTE-ABORDAJE:{patente}:{codigo}` con ingreso manual de patente y código; `POST /abordajes` los envía. |
| **Cambio de `debug` a remoto:** desde T14 la build `debug` llama al backend (`10.0.2.2:8080`) y no carga semilla; `demo` es la única local. `debug` y `demo` comparten `applicationId` y base: la semilla de `demo` queda en la caché de `debug`. | Correr `adb shell pm clear cl.belmontespa.myapplication` al pasar de una a otra (README, Build `demo`). El README ya describe el cambio. |
| **T14 probado en parte contra el backend (14-11):** con el compose (3307), `bootRun` y el emulador, la app inició sesión como Administrador y como Conductor, listó viajes y mostró el detalle con el abordaje y el pago. Con curl se recorrieron las rutas de T14 (login de los tres roles, cuenta pendiente y aprobación, flota, viaje en 3 pasos, asignar, pagar, iniciar, abordaje con patente y código, y los 400, 401, 404 y 409) y los contratos coinciden con los DTOs. Siguen sin recorrerse por la interfaz: nueva solicitud, asignar, cancelar, pagos, flota y el abordaje con QR. | Recorrerlos con un emulador y el backend en `10.0.2.2:8080` antes de cerrar T14. Un viaje solo se inicia a ±1 h de `fhAgenda` y esta debe estar entre 2 h y 90 días: para probar `EN_CURSO` y el abordaje hay que mover `fh_agenda` en la base. HU14 backend ya implementa `POST /auth/token-fcm`; falta la integración de Firebase en Android. |
| **HU11 probado en el emulador contra el backend (14-11):** con la red cortada, Iniciar y Finalizar quedan «Pendiente de sincronizar» y al reconectar el servidor pasa a `EN_CURSO` y `FINALIZADO`; con el viaje cancelado por el Administrador mientras tanto, el envío responde 409, el detalle muestra el mensaje del backend y «Descartar cambio» lo limpia; cerrar sesión con un cambio pendiente ofrece sincronizar o descartar (sin red «Sincronizar» no cierra). El banner de «Sin conexión» sale de un fallo de red en el envío o la consulta, no de `ConnectivityManager`. Sin recorrer por la interfaz: el aviso al entrar con otra cuenta (solo con pruebas instrumentadas) y el envío tras reiniciar el equipo. | Repetirlo con el emulador y el backend en `10.0.2.2:8080`: cortar la red con `adb shell svc wifi disable; adb shell svc data disable` y devolverla con `enable`. La cola solo existe con `modoLocal` falso (`debug` y `release`); `demo` no encola. |
| **`demo.sql` probado solo en `belmonte_test` (14-11):** se cargó varias veces sin errores, los conteos coinciden con el README y, con el backend apuntando a esa base, entraron las 7 cuentas activas (la por aprobar responde 403 y la inactiva 401), el Conductor vio sus 3 viajes y el abordaje con `KXJR42`, su código y un pasaporte respondió 200 (404 con un pasajero que no viaja). Los `dni_hash` se calculan en SQL con `@dni_secret` y coinciden con el HMAC del backend para el secreto de desarrollo, uno corto y uno de más de 64 bytes. No se cargó en la base de desarrollo `belmonte`, y `demo.sql` reemplaza al Administrador inicial `admin@belmontespa.cl`. | Cargarlo en el anfitrión de la demostración con el `DNI_SECRET` de `.env.demo` (`--init-command="SET @dni_secret='…'"`, ver la cabecera del script); con otro secreto el abordaje no encuentra a nadie. Los folios son TR-1001 a TR-1011, no los de la semilla Android, y `FlujosE2ETest` (T17) debe usarlos. |
| **`FlujosE2ETest` depende de datos y de la red del emulador (14-11):** las 5 pruebas pasaron dos veces seguidas contra el backend con `demo.sql` recién cargado: el Solicitante crea y cancela una solicitud, el Administrador asigna TR-1009 y cancela TR-1010 con motivo, el Pasajero aborda TR-1005 sin cuenta y el Conductor la inicia y la finaliza con la red cortada (`svc wifi` y `svc data`) hasta que el `SincronizacionWorker` la envía, y las cuentas por aprobar o inactivas no entran. Cada prueba confirma el estado en el servidor. Consumen los datos: sin recargar `demo.sql` fallan con un mensaje que lo dice, y TR-1005 solo se puede iniciar durante los 90 minutos siguientes a la carga. Se probó en `belmonte_test`; no cubre push, Google Places, TalkBack ni un dispositivo real. | Con el backend (`DB_URL=…/belmonte_test ./gradlew bootRun`) y el emulador: cargar `database/demo.sql` y correr `adb shell am instrument -w -e e2e true -e class cl.belmontespa.myapplication.e2e.FlujosE2ETest cl.belmontespa.myapplication.test/androidx.test.runner.AndroidJUnitRunner` tras `installDebug installDebugAndroidTest`. Solo en emulador: la prueba corta y devuelve la red del equipo. |
| **HU13 probado en el emulador contra el backend, sin Firebase real (14-11):** `HU13AvisoTest` (6 pruebas): el aviso abre el detalle al arrancar y con la app abierta (`onNewIntent`), «Atrás» vuelve a Mis viajes, la reasignación vuelve a la agenda, un aviso que no corresponde al rol, sin sesión o con contraseña temporal no navega, y el aviso en primer plano se publica en el canal `avisos` y al tocarlo abre el viaje. Con el backend en `belmonte_test`: TR-1009 asignado a Paula por la API y `adb shell am start -n cl.belmontespa.myapplication/.MainActivity --es tipo VIAJE --es folio TR-1009` con la app abierta y con el proceso detenido abre el detalle de un viaje que no estaba en Room, «Atrás» vuelve a Mis viajes y el viaje queda en su agenda del 15-11; con TR-1010, que no es suyo, el backend responde 403 y se ve «No tienes permiso para ver esto» con «Reintentar» (el README, en Refresco, dice que un 403 o 404 vuelve a la lista: hoy no lo hace). No hay proyecto Firebase ni `google-services.json`: el servicio solo actúa con `FIREBASE_CONFIGURADO`, así que no se probó la entrega real, ni el toque sobre el aviso que dibuja el sistema (se simuló con el mismo `Intent` que arma FCM), ni API 26. | Con el proyecto Firebase creado: agregar `google-services.json`, `FIREBASE_CREDENTIALS_FILE` en el backend, asignar un viaje a un Conductor con sesión y comprobar el aviso con la app abierta, en segundo plano y cerrada. Mientras tanto, repetir el `am start` de arriba tras `installDebug` y `installDebugAndroidTest`, y `adb shell am instrument -w -e class cl.belmontespa.myapplication.notificaciones.HU13AvisoTest cl.belmontespa.myapplication.test/androidx.test.runner.AndroidJUnitRunner`. |
| **Google Places sin clave real (15-11):** no hay proyecto de Google Cloud ni `PLACES_API_KEY`, así que solo se probó sin clave y con una inválida pasada por la línea de comandos (`-PPLACES_API_KEY=…`, nunca escrita en un archivo). Sin clave, y siempre en `demo`, «Nueva solicitud» usa la lista ficticia: pasan las 324 instrumentadas, las 5 de `FlujosE2ETest` contra el backend y el recorrido manual. Con la clave inválida el campo muestra «Busca una dirección o un lugar.», se abre el widget del SDK, al escribir dice «Unable to load results» y al salir la pantalla vuelve intacta con el aviso «No pudimos obtener el lugar»; salir sin buscar no avisa. `NuevaSolicitudPlacesTest` (3 pruebas) cubre la pantalla con un servicio falso: elegir origen y destino, una búsqueda fallida y una comuna fuera del catálogo corregida con el selector hasta crear la solicitud. No se probó: una búsqueda real, qué comuna y región devuelve Google para cada lugar de Chile (el backend coteja nombres normalizados y rechaza los que no están en el catálogo; entonces sale el selector), el costo por sesión, la restricción de la clave por paquete y SHA-1, ni el idioma del widget (sigue el del dispositivo; los resultados salen en español). | Con la clave real, restringida a la Places API, al paquete y a sus SHA-1: ponerla en `android/local.properties` como `PLACES_API_KEY=…`, `installDebug` y, con el backend, buscar un aeropuerto, una calle con número y un lugar sin comuna en «Nueva solicitud». Falta el secreto `PLACES_API_KEY` en CI (README, CI). |
| **Job `backend` de CI sin corrida en GitHub (15-11):** se agregó a `.github/workflows/ci.yml` (MariaDB 11.4 en 3307, `./gradlew test` y reporte si falla). Con esos comandos y la base de `database/docker-compose.yaml`, las 422 pruebas pasan sin fallos ni omisiones con `TZ=America/Santiago`; con `TZ=UTC`, la zona del runner, fallan 4: `P1AIT` y `NotificacionesAdministracionIT` (`@Future` de `fhAgenda` usa la zona de la JVM y no el `Clock` de Chile), `CuentasIT` (la prueba usa `LocalDateTime.now()` para una temporal vencida) y `HU27IT` (3 correos en vez de 1, causa sin investigar). Por eso el job fija `TZ`. No se vio correr en GitHub Actions: imagen, `healthcheck.sh`, tiempo y subida del reporte. | Ver la primera corrida del PR `chore/`; si falla, el log dice si es la imagen, el puerto o una prueba. Marcar `backend` como verificación requerida en la protección de `main` (README, Ramas y pull requests). Decidir si el backend valida `fhAgenda` con el `Clock` de Chile: un servidor en UTC rechaza con «Debe ser una fecha futura» los viajes a menos de 3 horas, o hay que fijar `TZ` al desplegar. |
| **ViewModels sin prueba:** `Conductores`, `DetalleSolicitudAdmin`, `MisSolicitudes`, `Notificacion` y `Vehiculos` no tienen `*ViewModelTest` (`Sesion` ya lo tiene). | Escribirlas con los dobles de `ReposFalsos` si alcanza el tiempo; cada integrante cubre las de sus pantallas. |
| **Evaluación individual:** cada uno debe explicar y modificar su Android sin apuntes ni IA. | Cada integrante escribe el Android de sus HU (README, [Plan de trabajo](README.md#plan-de-trabajo)), y T12 ensaya la explicación y un cambio en vivo. |
