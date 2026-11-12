# Backlog priorizado

**Proyecto:** Aplicación de Belmonte SpA · **Fecha:** 26-10-2026 · **Entrega de la Prueba 2:** miércoles
28-10-2026 antes de las 19:00.

Fuente: [README.md](README.md) (la especificación; si este backlog difiere, gana el README) y
[docs/informacion_importante.md](docs/informacion_importante.md) (requisitos de la Prueba 2). El
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

  Las llamadas remotas del Repository (Retrofit) son T14, en la Etapa 2.
- **Estado del backend:**
  - **Hecha:** la ruta existe con su controlador y su regla, y tiene pruebas.
  - **Falta:** no existe.

  Lo que queda pendiente está en [Pendiente del backend](#pendiente-del-backend-etapa-2).
- **Android:** la app vive en `android/` y la build `demo` corre completa sobre Room, con las 46 pantallas
  (MVVM con Repository simple, sin Hilt, `view/` y `viewmodel/`). Cada pantalla tiene su ViewModel,
  salvo "Inicio de aplicación" y "Abordaje registrado", que no tienen estado propio. Las llamadas
  remotas del Repository, el push y la cola sin conexión son la Etapa 2. La rama `android` es obsoleta
  y no se usa.

## Resumen

| | A | B | Total |
|---|---|---|---|
| Etapa 1 (SP) | 45 | 36 | 81 |
| Etapa 2 (SP) | 8 | 13 | 21 |
| Total (SP) | 53 | 49 | 102 |

**Backend:**
- **Compilación:** compila (`./gradlew compileJava compileTestJava`).
- **Pruebas:** hay 399 anotaciones de prueba. Las IT no se corrieron en esta revisión, porque no hubo
  MariaDB.
- **Rutas:** 47 de las 48 del README tienen controlador; solo falta `POST /auth/token-fcm` (HU14, Etapa 2).
  Ya no existen las 3 rutas que el README no admite (`POST /usuarios`, `PUT /solicitantes/{correo}` y
  `POST /empresas`).

| Rutas del README | Hechas | Faltan |
|---|---|---|
| 48 | 47 | 1 |

**Meta del 75 %:** al menos 36 de las 48 rutas del README (36 es el 75 % de 48). Hoy existen 47, todas
las de la Etapa 1; la que falta es de la Etapa 2. Lo que queda por confirmar es que sus IT pasen contra
MariaDB. El [mapa de rutas](#rutas-y-75-) indica a qué ítem
pertenece cada una.

## Criterios de priorización

1. **Fecha:** la Etapa 1 (28-10) va antes que la Etapa 2.
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
aunque falten reglas. El backend se completa en el orden de este backlog hasta pasar las 36 rutas.

**Hitos sugeridos:**

| Fecha | Hito |
|---|---|
| Miércoles 21-10 | T01 a T04 listos y T05 en curso; T07 y T08 fusionados en `main`. |
| Jueves 22-10 | T06: las 46 pantallas navegables (red de seguridad de la "UI al 100 %"). |
| Domingo 25-10 | P1 completo, incluido el QR. |
| Lunes 26-10 | P2 y la mayor parte de P3. |
| Martes 27-10 | P4, video grabado (T11) y ensayo (T12). |
| Miércoles 28-10 | `main` congelado en la mañana; entrega (T13) antes de las 19:00. |

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
| 11 | HU01 | Solicitante: registro y login | A | P1 | Alta | 3 | T03, T07 | Hecho; falta el push de cuenta pendiente | Listo |
| 12 | HU06 | Conductor: login con credenciales del Administrador | A | P1 | Alta | 2 | HU01 | En HU01 | Listo |
| 13 | HU02 | Solicitante: nueva solicitud con Google Places | A | P1 | Alta | 5 | T05, T09 | Hecho; falta el push de V1 | Listo (lugares ficticios; Places en T14) |
| 14 | HU03 | Solicitante: historial y estado | A | P1 | Media | 3 | HU02 | Hecho | Listo |
| 15 | HU04 | Administrador: pendientes | B | P1 | Alta | 3 | T05, T10 | Hecho | Listo |
| 16 | HU05 | Administrador: asignar | B | P1 | Alta | 5 | HU04, T08 | Hecho | Listo |
| 17 | HU07 | Conductor: viajes del día en tiempo real | B | P1 | Alta | 3 | T05, T10 | En HU04 | Listo |
| 18 | HU08 | Conductor: detalle del viaje | B | P1 | Alta | 3 | HU07 | Hecho | Listo |
| 19 | HU09 | Conductor: En Curso y Finalizado | B | P1 | Alta | 3 | HU08, T10 | Hecho | Listo |
| 20 | HU10 | Conductor: ruta en Google Maps o Waze | B | P1 | Alta | 3 | HU08 | Sin backend | Listo |
| 21 | HU29 | Pasajero: abordar con el código QR | B | P1 | Alta | 5 | T05, T08 | Hecho | Listo |
| 22 | HU30 | Conductor: arqueo de abordaje | B | P1 | Alta | 3 | HU29, HU08 | En HU08 | Listo |
| 23 | HU15 | Administrador: alta de Conductor | A | P2 | Alta | 3 | T07 | Hecho | Listo |
| 24 | HU16 | Solicitante: cancelar | A | P2 | Alta | 3 | HU03, T07 | Hecho | Listo |
| 25 | HU24 | Administrador: cancelar un viaje | B | P2 | Alta | 3 | HU08, HU16 | En HU16 | Listo |
| 26 | HU17 | Administrador: alta de vehículo | A | P3 | Media | 3 | T07, T08 | Hecho | Listo |
| 27 | HU18 | Administrador: habilitaciones | A | P3 | Media | 2 | HU15, HU17 | Hecho | Listo |
| 28 | HU19 | Administrador: registrar pago | A | P3 | Media | 5 | T07 | Hecho | Listo |
| 29 | HU20 | Administrador: estado del pago | A | P3 | Media | 3 | HU19 | Hecho | Listo |
| 30 | HU22 | Administrador: restablecer la contraseña de una cuenta | A | P3 | Media | 3 | T07 | Hecho | Listo (la temporal la genera el Administrador y vence a las 24 h) |
| 31 | HU27 | Administrador: aprobar solicitantes | A | P3 | Media | 3 | HU01 | Hecho | Listo |
| 32 | HU26 | Administrador: dar de baja | A | P3 | Media | 3 | HU15, HU17 | Hecho | Listo |
| 33 | HU23 | Administrador: reasignar | B | P3 | Media | 3 | HU05 | En HU05 | Listo |
| 34 | HU25 | Conductor: rechazar una asignación | B | P3 | Media | 2 | HU08, T08 | Hecho | Listo |
| 35 | HU21 | Solicitante: estado de pago | A | P4 | Baja | 2 | HU19 | En HU19 | Listo |
| 36 | HU28 | Administrador: registrar otro Administrador | A | P4 | Baja | 2 | HU22 | Hecho | Listo |
| 37 | T11 | Video de hasta 7 minutos sin audio | A y B | Cierre | — | — | T06, P1 | — | — |
| 38 | T12 | Ensayo de la evaluación | A y B | Cierre | — | — | T11 | — | — |
| 39 | T13 | Entrega del 28-10 | A y B | Cierre | — | — | T11 | — | — |

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
  origen y el destino se eligen de una lista de lugares ficticios, porque Google Places llega en T14.
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
- **T12:** cada integrante, sin apuntes ni IA:
  - explica los paquetes, las dependencias, la navegación, Room y el QR de su parte;
  - ubica en qué archivo y función está cada funcionalidad suya;
  - hace un cambio pequeño en vivo.
- **T13:** se envían el código del backend, el código Android y el video antes de las 19:00, desde
  `main` congelado.

## Etapa 2: Unidad 3

| # | ID | Ítem | Resp. | Prio. | Prio. HU | SP | Depende de | Backend | Estado |
|---|---|---|---|---|---|---|---|---|---|
| 40 | T14 | Llamadas remotas en los Repository, `ErrorMapper` e interceptor | A y B | E2 | — | — | Etapa 1 | — | — |
| 41 | HU14 | Todos: token de notificaciones | A | E2 | Alta | 3 | T14 | 1 falta | — |
| 42 | HU13 | Conductor: push de asignación | B | E2 | Alta | 5 | HU14 | Hecho; falta el token | — |
| 43 | HU12 | Solicitante: push de estado | A | E2 | Media | 5 | HU14 | Ajustar V3 y V4 | — |
| 44 | HU11 | Conductor: uso sin conexión | B | E2 | Media | 8 | T14 | Sin backend | — |
| 45 | T15 | Backend completo y pruebas al día | A y B | E2 | — | — | Etapa 1 | Rutas restantes | — |
| 46 | T16 | `database/demo.sql` | B | E2 | — | — | T15 | — | — |
| 47 | T17 | `FlujosE2ETest` | A y B | E2 | — | — | T14, T16 | — | — |
| 48 | T18 | Pasada completa con TalkBack | A y B | E2 | — | — | T14 | — | — |
| 49 | T19 | APK release `v1.0.0` en GitHub Releases | A | E2 | — | — | T17, T18 | — | — |

SP de la Etapa 2: A, 8; B, 13.

**Listo cuando, por tarea:**
- **T14:**
  - cada Repository llama a la API en modo remoto (`MODO_LOCAL` falso), según
    [Sesión y conectividad](README.md#sesión-y-conectividad);
  - `ErrorMapper` existe con su `ErrorMapperTest`;
  - se agregan las dependencias que hoy no se usan y por eso no están declaradas: Retrofit y OkHttp,
    WorkManager, Firebase (BoM, `firebase-messaging` y el plugin `google-services`) y
    Google Places, que reemplaza la lista de lugares ficticios de "Nueva solicitud" (HU02).

  Cada integrante hace las llamadas remotas de sus funcionalidades, y A hace además `ErrorMapper` y el
  interceptor.
- **HU11:** incluye `SincronizacionWorkerTest`.
- **T15:** las 48 rutas cumplen el README y todas las pruebas de [Pruebas](README.md#pruebas) pasan.
- **T16:** el contenido es el de [Stack y entornos](README.md#stack-y-entornos).
- **T19:** según [Compilación y distribución](README.md#compilación-y-distribución).

## Pendiente del backend (Etapa 2)

Rutas de archivo relativas a `backend/src/main/java/cl/belmontespa/transporte/`. Las reglas están en el README
([Notificaciones](README.md#notificaciones) y [Autenticación](README.md#autenticación)).

- **`POST /auth/token-fcm` (HU14):** falta. El token es único: al guardarlo se quita de cualquier otro
  usuario. `POST /auth/logout` ya existe.
- **Push de V3 y V4 al Solicitante (HU12):** `ViajeService` envía `data` solo con `folio` y el mensaje con el
  código ("Estado: EN_CURSO"). Debe llevar `data.tipo = VIAJE`, el título "Viaje {folio}" y la etiqueta
  ("En curso" o "Finalizado").
- **Push de V1 y de cuenta pendiente (HU02 y HU01):** no se envían. Faltan "Nueva solicitud" (V1) y "Cuenta por
  aprobar" (con `data.tipo = CUENTAS`), ambos a los Administradores con token.
- **Ya llevan `data.tipo`:** los push de asignación y reasignación (`AvisosDeViaje`, con `VIAJE` y `AGENDA`),
  de rechazo (V8), de cancelación y de pago.

## Rutas y 75 %

Estado de cada ruta del README e ítem que la cumple. Las rutas son relativas a `/api/v1/`.

| Ruta | Estado | Ítem |
|---|---|---|
| `POST /auth/login` | Hecha | HU01 |
| `POST /auth/logout` | Hecha | HU14 |
| `POST /auth/token-fcm` | Falta | HU14 (E2) |
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
- **Por estado:** 47 hechas y 1 falta (`POST /auth/token-fcm`). El estado sale de comparar cada ruta de la
  tabla con los `@…Mapping` de los controladores, incluidas las heredadas de `LecturaController` y
  `CrudController`; no incluye el comportamiento, que las IT aún no se corrieron.
- **Etapa 1:** tiene 47 rutas (todas menos `POST /auth/token-fcm`); hay que dejar al menos 36 de las 48
  cumpliendo el README con pruebas.
- **Por integrante:** las HU de A tienen 42 rutas y las de B, 6. Las de B son las de reglas más
  complejas (V2, V3, V8 y el abordaje).

## Riesgos

| Riesgo | Mitigación |
|---|---|
| **Plazo:** la entrega es el 28-10 y quedan el video (T11), el ensayo (T12) y la entrega (T13). | Las HU de la Etapa 1 figuran en Listo; el video (T11) lo edita el compañero. |
| **Backend desigual:** A tiene 42 de las 48 rutas y la mayor parte de P3. | B hace la base del backend (T07 a T10). Como el backend no se presenta ni se pregunta en la Prueba 2, B puede tomar rutas de A, previo acuerdo, sin afectar que cada uno explique su Android. |
| **Pruebas sin verificar:** las IT del backend no se corrieron en esta revisión (sin MariaDB) y las instrumentadas de Android tampoco. | Antes de congelar `main`, levantar el compose y correr `./gradlew test`; en Android, `connectedDebugAndroidTest` en un emulador aparte (comparte el `applicationId` de la demo y borra sus datos). |
| **Variante `debug`:** carga la misma semilla que `demo` y abre sin backend, pero no muestra los avisos de demostración ni usa `MODO_LOCAL`. Los Repository solo usan Room hasta T14. | Mostrar siempre la build `demo` (`./gradlew installDemo`). El modo remoto es T14. |
| **CI sin job `backend`:** `.github/workflows/ci.yml` solo corre `android`; el README define también `backend` con MariaDB. | Agregar el job (PR `chore/`) antes de congelar `main`, o correr `./gradlew test` en local. |
| **ViewModels sin prueba:** `Sesion`, `Perfil`, `Notificacion`, `Conductores`, `Vehiculos`, `MisSolicitudes` y `DetalleSolicitudAdmin` no tienen `*ViewModelTest`. | Escribirlas con los dobles de `ReposFalsos` si alcanza el tiempo; cada integrante cubre las de sus pantallas. |
| **Evaluación individual:** cada uno debe explicar y modificar su Android sin apuntes ni IA. | Cada integrante escribe el Android de sus HU (README, [Plan de trabajo](README.md#plan-de-trabajo)), y T12 ensaya la explicación y un cambio en vivo. |
