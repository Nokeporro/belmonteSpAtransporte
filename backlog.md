# Backlog priorizado

**Proyecto:** Aplicación de Belmonte SpA · **Fecha:** 20-10-2026 · **Entrega de la Prueba 2:** miércoles
28-10-2026 antes de las 19:00.

Fuente: [README.md](README.md) (la especificación; si este backlog difiere, gana el README) y
[docs/informacion_importante.md](docs/informacion_importante.md) (requisitos de la Prueba 2). El
estado del backend sale de revisar `backend/` en esta rama, que es igual a `main` y a `backend`.

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
  - sus pantallas sobre el repositorio local, recorribles en la [build `demo`](README.md#build-demo);
  - su parte de backend con pruebas.

  El repositorio remoto (Retrofit) es T14, en la Etapa 2.
- **Estado del backend:**
  - **Hecha:** cumple el README.
  - **Ajustar:** existe pero difiere del README.
  - **Falta:** no existe.

  El detalle de cada caso está en [Brechas del backend](#brechas-del-backend).
- **Android:** no hay código. No existe `android/`, y la rama `android` es obsoleta y no se usa.

## Resumen

| | A | B | Total |
|---|---|---|---|
| Etapa 1 (SP) | 45 | 36 | 81 |
| Etapa 2 (SP) | 8 | 13 | 21 |
| Total (SP) | 53 | 49 | 102 |

**Backend:**
- **Compilación:** compila (`./gradlew compileJava compileTestJava`).
- **Pruebas:** las IT no se corrieron, porque no hubo acceso a Docker.
- **Base existente:** está hecho para una versión anterior de la especificación (sin abordaje QR, aprobación de cuentas, `activo`, rechazo del Conductor ni reembolsos).

| Rutas del README | Hechas | Ajustar | Faltan |
|---|---|---|---|
| 48 | 7 | 32 | 9 |

Además, existen 3 rutas que el README no admite: `POST /usuarios`, `PUT /solicitantes/{correo}` y
`POST /empresas`.

**Meta del 75 %:** al menos 36 de las 48 rutas deben cumplir el README y tener sus pruebas. Hoy hay
7, así que en la Etapa 1 faltan al menos 29. El [mapa de rutas](#rutas-y-75-) indica a qué ítem
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
| Miércoles 21-10 | T01 a T05 listos; T07 y T08 fusionados en `main`. |
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
| 5 | T05 | `DatosDemo`, `RepositorioModule` y build `demo` | B | P0 | — | — | T04 | — | Listo |
| 6 | T06 | Esqueleto navegable de las 46 pantallas | A y B | P0 | — | — | T02, T03, T05 | — | listo |
| 7 | T07 | Backend: migración de cuentas, flota y pagos | B | P0 | — | — | — | Falta | Listo |
| 8 | T08 | Backend: migración de viajes y abordaje | B | P0 | — | — | T07 | Falta | Listo |
| 9 | T09 | Backend: validaciones, errores y rutas que sobran | B | P0 | — | — | — | Ajustar | Listo |
| 10 | T10 | Hora de Chile y orden de listas (backend y app) | B | P0 | — | — | T05 | Ajustar | Listo |
| 11 | HU01 | Solicitante: registro y login | A | P1 | Alta | 3 | T03, T07 | 3 ajustar | — |
| 12 | HU06 | Conductor: login con credenciales del Administrador | A | P1 | Alta | 2 | HU01 | En HU01 | — |
| 13 | HU02 | Solicitante: nueva solicitud con Google Places | A | P1 | Alta | 5 | T05, T09 | 3 hechas, 4 ajustar, 1 falta | — |
| 14 | HU03 | Solicitante: historial y estado | A | P1 | Media | 3 | HU02 | Usa HU04 y HU08 | — |
| 15 | HU04 | Administrador: pendientes | B | P1 | Alta | 3 | T05, T10 | 1 ajustar | Listo |
| 16 | HU05 | Administrador: asignar | B | P1 | Alta | 5 | HU04, T08 | 1 ajustar | Listo |
| 17 | HU07 | Conductor: viajes del día en tiempo real | B | P1 | Alta | 3 | T05, T10 | En HU04 | Listo |
| 18 | HU08 | Conductor: detalle del viaje | B | P1 | Alta | 3 | HU07 | 1 ajustar | Listo |
| 19 | HU09 | Conductor: En Curso y Finalizado | B | P1 | Alta | 3 | HU08, T10 | 1 ajustar | — |
| 20 | HU10 | Conductor: ruta en Google Maps o Waze | B | P1 | Alta | 3 | HU08 | Sin backend | — |
| 21 | HU29 | Pasajero: abordar con el código QR | B | P1 | Alta | 5 | T05, T08 | 1 falta | — |
| 22 | HU30 | Conductor: arqueo de abordaje | B | P1 | Alta | 3 | HU29, HU08 | En HU08 | — |
| 23 | HU15 | Administrador: alta de Conductor | A | P2 | Alta | 3 | T07 | 4 ajustar | — |
| 24 | HU16 | Solicitante: cancelar | A | P2 | Alta | 3 | HU03, T07 | 1 ajustar | — |
| 25 | HU24 | Administrador: cancelar un viaje | B | P2 | Alta | 3 | HU08, HU16 | En HU16 | — |
| 26 | HU17 | Administrador: alta de vehículo | A | P3 | Media | 3 | T07, T08 | 3 ajustar | — |
| 27 | HU18 | Administrador: habilitaciones | A | P3 | Media | 2 | HU15, HU17 | 3 ajustar | — |
| 28 | HU19 | Administrador: registrar pago | A | P3 | Media | 5 | T07 | 3 ajustar | — |
| 29 | HU20 | Administrador: estado del pago | A | P3 | Media | 3 | HU19 | 1 ajustar | — |
| 30 | HU22 | Solicitante/Administrador: recuperar contraseña | A | P3 | Media | 3 | T07 | 2 hechas, 2 ajustar | — |
| 31 | HU27 | Administrador: aprobar solicitantes | A | P3 | Media | 3 | HU01 | 1 hecha, 2 ajustar, 2 faltan | — |
| 32 | HU26 | Administrador: dar de baja | A | P3 | Media | 3 | HU15, HU17 | 2 faltan | — |
| 33 | HU23 | Administrador: reasignar | B | P3 | Media | 3 | HU05 | En HU05 | — |
| 34 | HU25 | Conductor: rechazar una asignación | B | P3 | Media | 2 | HU08, T08 | 1 falta | — |
| 35 | HU21 | Solicitante: estado de pago | A | P4 | Baja | 2 | HU19 | En HU19 | — |
| 36 | HU28 | Administrador: registrar otro Administrador | A | P4 | Baja | 2 | HU22 | 1 hecha, 2 ajustar | — |
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
  - las interfaces de los 7 repositorios;
  - `RepositorioModule` con `MODO_LOCAL`;
  - la semilla `DatosDemo`;
  - el aviso en el Login;
  - `./gradlew installDemo` funcionando.
- **T06:** las 46 pantallas de los [mockups](mockups/wireframes-belmonte.html) se recorren con datos
  de `DatosDemo`. Cada integrante hace las pantallas de sus HU, aunque aún no tengan todas las
  reglas.
- **T07 y T08:**
  - las migraciones de [T07](#t07-migración-de-cuentas-flota-y-pagos) y
    [T08](#t08-migración-de-viajes-y-abordaje) se aplican sobre una base existente y sobre una vacía;
  - las entidades JPA validan (`ddl-auto=validate`);
  - existe `MigracionesIT`.
- **T09 y T10:** se cumplen las listas de [T09](#t09-validaciones-errores-y-rutas-que-sobran) y
  [T10](#t10-hora-de-chile-y-orden), con sus pruebas. En la app incluye `ValidacionesTest`
  (parte de T09) y `RetrasoTest` (T10).
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
| 40 | T14 | Repositorios remotos, `ErrorMapper` e interceptor | A y B | E2 | — | — | Etapa 1 | — | — |
| 41 | HU14 | Todos: token de notificaciones | A | E2 | Alta | 3 | T14 | 2 faltan | — |
| 42 | HU13 | Conductor: push de asignación | B | E2 | Alta | 5 | HU14 | Ajustar push | — |
| 43 | HU12 | Solicitante: push de estado | A | E2 | Media | 5 | HU14 | Ajustar push | — |
| 44 | HU11 | Conductor: uso sin conexión | B | E2 | Media | 8 | T14 | Sin backend | — |
| 45 | T15 | Backend completo y pruebas al día | A y B | E2 | — | — | Etapa 1 | Rutas restantes | — |
| 46 | T16 | `database/demo.sql` | B | E2 | — | — | T15 | — | — |
| 47 | T17 | `FlujosE2ETest` | A y B | E2 | — | — | T14, T16 | — | — |
| 48 | T18 | Pasada completa con TalkBack | A y B | E2 | — | — | T14 | — | — |
| 49 | T19 | APK release `v1.0.0` en GitHub Releases | A | E2 | — | — | T17, T18 | — | — |

SP de la Etapa 2: A, 8; B, 13.

**Listo cuando, por tarea:**
- **T14:**
  - cada repositorio tiene su implementación remota, según
    [Sesión y conectividad](README.md#sesión-y-conectividad);
  - `ErrorMapper` existe con su `ErrorMapperTest`.

  Cada integrante hace los repositorios de sus funcionalidades, y A hace además `ErrorMapper` y el
  interceptor.
- **HU11:** incluye `SincronizacionWorkerTest`.
- **T15:** las 48 rutas cumplen el README y todas las pruebas de [Pruebas](README.md#pruebas) pasan.
- **T16:** el contenido es el de [Stack y entornos](README.md#stack-y-entornos).
- **T19:** según [Compilación y distribución](README.md#compilación-y-distribución).

## Brechas del backend

Rutas de archivo relativas a `backend/src/main/java/cl/belmontespa/transporte/` y a
`backend/src/main/resources/db/migration/`. Las reglas están en el README:
- [Máquinas de estado](README.md#máquinas-de-estado);
- [Validaciones](README.md#validaciones);
- [Contratos JSON](README.md#contratos-json).

Las pruebas IT existentes prueban el comportamiento anterior. Por ejemplo, `ViajeIT.ocupados`
espera que asignar deje al conductor Ocupado, y `PagoIT.anulaSegunEstado` espera que cancelar
anule cualquier pago. Por eso, cada ítem ajusta sus pruebas junto con el código.

### T07 Migración de cuentas, flota y pagos

- **`usuario`:**
  - agregar `password_temporal_hash`; hoy la temporal se escribe sobre `password_hash`, según el
    comentario de `V1__esquema.sql`;
  - `token_fcm` pasa a ser UNIQUE.
- **Solicitante:** crear `ESTADO_SOLICITANTE` (`PENDIENTE`, `APROBADO`, `RECHAZADO`) y
  `solicitante.estado_solicitante_id`. Los Solicitantes existentes quedan `APROBADO`.
- **`activo`:** agregar `conductor.activo` y `movil.activo`, con valor por defecto verdadero.
- **`pago`:**
  - `fecha_pago` admite NULL;
  - nueva columna `monto_reembolsado`;
  - `ESTADO_PAGO` suma `REEMBOLSADO`;
  - `bu_pago` admite `PAGADO → REEMBOLSADO`.
- **`viaje`:** `pago_id` pasa a ser UNIQUE.
- **`estado_movil`:** quitar `MANTENCION`, que el README no tiene.
- **Administrador inicial:** su contraseña de desarrollo pasa a `password_temporal_hash`, y
  `password_hash` queda inutilizable (`V3__datos_iniciales.sql`).
- **Entidades:** actualizar `Usuario`, `Solicitante`, `Conductor`, `Movil` y `Pago`.

### T08 Migración de viajes y abordaje

- **`viaje.motivo`:** nueva columna `VARCHAR(200)`.
- **`folio`:** debe ser `TR-` + un correlativo desde 1001, generado por trigger. Hoy es `VJ` + UUID,
  tanto en `bi_viaje` como en `model/Viaje.java` (`@PrePersist`); el `@PrePersist` se quita.
- **`bu_viaje`:** admitir V8 (`ASIGNADO → PENDIENTE`).
- **`au_viaje`:** auditar solo `estado`, `conductor`, `movil`, `cantidad_pasajeros`, `pago` y
  `motivo`. Hoy también audita `fh_*`, `origen`, `destino` y `numero_vuelo`.
- **`detalle_viaje_pasajero.fh_abordaje`:** nueva columna `DATETIME NULL`.
- **`movil.codigo_abordaje`:** se llena con `HEX(RANDOM_BYTES(8))` y después pasa a `NOT NULL` y
  UNIQUE.
- **`MigracionesIT`:** nueva. Cubre la cadena completa sobre una base vacía.

### T09 Validaciones, errores y rutas que sobran

- **Normalización antes de validar:** correo en minúsculas, teléfono como `+56…`, patente,
  RUT (sin puntos, con guion y `K` mayúscula) y número de vuelo. Hoy no se normaliza nada.
- **Reglas de la tabla de Validaciones que faltan o difieren:**
  - patente `^([A-Z]{2}[0-9]{4}|[A-Z]{4}[0-9]{2})$`; hoy `^[A-Z]{2}[A-Z0-9]{2}[0-9]{2}$`;
  - `password` de 8 a 72 caracteres, con una letra y un dígito; hoy de 8 a 100 sin más reglas;
  - `nombre` y `razonSocial` con al menos 2 caracteres;
  - dígito verificador del RUT;
  - `capacidad` hasta 20 y `gpsSerial` mayor que 0;
  - formato de `numeroVuelo`, `folio`, `hash` y `dniHash`.
- **400 de validación:** las líneas `campo: motivo` se unen con `\n`; hoy con `; `
  (`exception/ApiExceptionHandler.java`).
- **Rutas que sobran:** quitar `POST /usuarios`, `PUT /solicitantes/{correo}` (y
  `SolicitanteService.reemplazar`) y `POST /empresas` (y `EmpresaDTORequest`), en sus controladores y
  en `config/SecurityConfig.java`.
- **`SecurityConfig`:** quitar al Conductor de `PUT /usuarios/*` y agregar las rutas nuevas de
  cada ítem.
- **JWT:** `JWT_EXPIRATION_SECONDS` pasa a 28800 por defecto; hoy es 3600 en
  `application.properties`.
- **Opcional:** quitar `application-dev.properties`. Usa H2, que no está en las dependencias.

### T10 Hora de Chile y orden

- **`config/ClockConfig.java`:** bean `Clock` en `America/Santiago`.
- **"Ahora":** reemplazar `LocalDateTime.now()` y `LocalDate.now()` en `ViajeService`,
  `ViajeController.fecha`, `AuthService` y `PagoService`.
- **Orden fijo de las listas:** hoy `CrudService.listar` usa `findAll()` sin orden.
  - viajes según su estado;
  - cuentas y pasajeros por `nombre`;
  - `empresas` por `razonSocial`;
  - `moviles` por `patente`;
  - habilitaciones por `patente` y `nombreConductor`;
  - `pagos` por `folio` descendente.

  El `folio` se ordena por su número.
- **App:** cálculo de la ventana de inicio, Atrasado y Vencido con un `Clock` inyectado.

### HU01 y HU06

- **`AuthService.login`:**
  - responde `cambioPasswordRequerido` y emite un JWT con `temporal: true` cuando se entra con la
    temporal;
  - responde 401 a un Conductor inactivo;
  - responde 403 a un Solicitante no aprobado.
- **`SecurityConfig`:** con el JWT temporal, permite solo `GET` y `PUT /usuarios/{correo}` propios y
  `POST /auth/logout`.
- **`POST /solicitantes`:** la cuenta queda `PENDIENTE` si la empresa ya existía y `APROBADO` si es
  nueva.
- **DTO:**
  - `LoginDTOResponse` suma `cambioPasswordRequerido`;
  - `SolicitanteDTOResponse` suma `estadoSolicitante`.

### HU02

- **`POST /pasajeros`:**
  - suma `tipoDocumento`: `RUT` con dígito verificador, o `PASAPORTE` de 5 a 20 caracteres;
  - con un documento ya registrado responde `200` con `nombre` `null`; hoy responde `201` con el
    nombre guardado.
- **`POST /viajes`:**
  - `fhAgenda` de 2 horas a 90 días, truncada al minuto; hoy solo `@Future`;
  - origen distinto del destino;
  - solo un Solicitante aprobado;
  - push a los Administradores, que se completa en la Etapa 2.
- **Cotejo de comuna:** se normaliza y se busca por nombre, y el error es `origen.comuna: No existe
  en el catálogo.` Hoy `UbicacionService` exige el nombre exacto de la comuna y de la región.
- **`POST /pasajeros-viaje`:** un par existente responde `200`; hoy `201`.
- **`GET /catalogos`:** falta; hay que crear `CatalogoController` y `CatalogosDTOResponse`.
- **`GET /pasajeros`:** se ordena por `nombre` (T10).

### HU04, HU07 y HU08

- **`GET /viajes`:**
  - `estado` y `fecha` combinables para los tres roles; hoy el Solicitante ignora los filtros y el
    Administrador solo acepta `estado`;
  - un filtro mal formado responde 400;
  - la agenda del Conductor suma su `EN_CURSO` de un día anterior.
- **`ViajeDTOResponse`:**
  - suma `nombreSolicitante`, `telefonoSolicitante`, `razonSocial`, `motivo` y `pasajeros[].fhAbordaje`;
  - `hashPago` y `estadoPago` van en `null` para el Conductor.

### HU05 y HU23

- **V2 no cambia el estado del conductor ni del vehículo.** Hoy `ViajeService.asignar` los marca
  Ocupado y En uso, y `liberar` los devuelve a Disponible.
- **Validaciones que faltan:**
  - viaje no vencido;
  - al menos un pasajero;
  - conductor y vehículo activos;
  - sin otro viaje `ASIGNADO` o `EN_CURSO` a menos de 2 horas.

  Al reasignar se valida solo lo que cambia.
- **Cambio de vehículo:** vuelve `fh_abordaje` a NULL.
- **Push:** al conductor reemplazado, con `data.tipo` `AGENDA`.

### HU09

- **V3:** ventana de ± 1 hora, y el conductor y el vehículo sin otro viaje `EN_CURSO`. V3 marca
  Ocupado y En uso, y V4 los libera. Hoy no hay ventana.

### HU29 y HU30

- **`POST /abordajes`:** falta, y es público. Hay que crear `AbordajeController`, `AbordajeService`,
  `AbordajeDTORequest` y `AbordajeDTOResponse`, y declarar la ruta pública en `SecurityConfig`.
  - **Candidatos:** viajes `ASIGNADO` dentro de la ventana de V3, o `EN_CURSO`.
  - **Errores:** el mismo 404 en todos los casos.
  - **Repetición:** repetir devuelve la misma hora.
  - **Pruebas:** `AbordajeIT`.
- **Arqueo:** `fhAbordaje` va en el viaje (ver HU08).

### HU15

- **`ConductorDTOResponse`:** suma `activo`, `viajesAsignados` y `folioEnCurso`.
- **`PUT /conductores/{correo}`:** el `correo` del cuerpo debe coincidir con el de la ruta (400). Hoy
  `UsuarioService.reemplazarDatos` permite cambiarlo.

### HU16 y HU24

- **V5 (Solicitante):**
  - solo sin vencer;
  - un pago `PAGADO` pasa a `REEMBOLSADO` por el 50 %, redondeado hacia abajo;
  - un pago `PENDIENTE` pasa a `ANULADO`;
  - no toca `fh_fin` ni el estado del conductor o del vehículo.

  Hoy `ViajeService.cancelar` anula cualquier pago, llama a `liberar` y fija `fh_fin`.
- **V7 (Administrador) en la misma ruta:**
  - nuevo `CancelarViajeDTORequest`, con `motivo` y `montoReembolso`;
  - reembolso del 100 %, o del monto fijado si el viaje está vencido;
  - `SecurityConfig` debe permitir al Administrador; hoy solo al Solicitante.

### HU17 y HU18

- **`MovilDTOResponse`:** suma `activo` y `codigoAbordaje`.
- **`MovilService`:** genera `codigo_abordaje` con `SecureRandom`.
- **Habilitaciones:**
  - filtros `?patente=` y `?correo=`;
  - `DetalleMovilConductorDTOResponse` suma `capacidad`, `estadoMovil`, `activoMovil`,
    `estadoConductor` y `activoConductor`;
  - un par existente responde `200`; hoy `201`.

### HU19, HU20 y HU21

- **`fecha_pago`:** se fija al pasar a `PAGADO`; hoy se fija al crear.
- **`PagoDTOResponse`:** suma `folio` y `montoReembolsado`.
- **`CambiarEstadoPagoDTORequest`:** acepta `ANULADO`; hoy solo `PAGADO` o `RECHAZADO`.
- **`PagoService.cambiarEstado`:**
  - admite P3 (`PAGADO → ANULADO`);
  - anular cancela el viaje (V6).

### HU22

- **Recuperar contraseña:**
  - la temporal se guarda en `password_temporal_hash`, sin reemplazar la actual;
  - el correo usa el texto del README; hoy `client/EmailClient.java` usa otro.
- **`PUT /usuarios/{correo}`:**
  - exige `passwordActual` si viene `password`;
  - el `correo` no cambia.

### HU26

- **Rutas nuevas:** `POST /conductores/{correo}/activo` y `POST /moviles/{patente}/activo`, con
  `ActivoDTORequest`.
- **Reglas:** 409 si hay viajes `ASIGNADO` o `EN_CURSO`. Un inactivo no se asigna ni inicia sesión.

### HU27

- **Rutas nuevas:** `POST /solicitantes/{correo}/aprobar` y `.../rechazar`, con 409 si la cuenta ya
  se revisó.
- **`GET /solicitantes`:** filtro `?estado=PENDIENTE`.
- **Correos:** los de cuenta aprobada y rechazada, que no existen en `EmailClient`.

### HU28

- **`POST /administradores`:** la contraseña inicial es temporal, con vencimiento a
  `ALTA_TEMPORAL_HORAS`; la variable aún no existe.

### HU14, HU13 y HU12 (Etapa 2)

- **`POST /auth/logout`:** falta.
- **`POST /auth/token-fcm`:** falta. El token es único: al guardarlo se quita de cualquier otro
  usuario.
- **Push:** debe llevar `data.tipo` (`VIAJE`, `AGENDA` o `CUENTAS`) y los textos del README. Hoy
  `data` solo trae `folio`, y el mensaje usa el código ("Estado: EN_CURSO").

## Rutas y 75 %

Estado de cada ruta del README e ítem que la cumple. Las rutas son relativas a `/api/v1/`.

| Ruta | Estado | Ítem |
|---|---|---|
| `POST /auth/login` | Ajustar | HU01 |
| `POST /auth/logout` | Falta | HU14 (E2) |
| `POST /auth/token-fcm` | Falta | HU14 (E2) |
| `POST /auth/recuperar-password` | Ajustar | HU22 |
| `GET /catalogos` | Falta | HU02 |
| `GET /usuarios` | Hecha | HU22 |
| `GET /usuarios/{correo}` | Hecha | HU22 |
| `PUT /usuarios/{correo}` | Ajustar | HU22 |
| `GET /administradores` | Ajustar (orden) | HU28 |
| `GET /administradores/{correo}` | Hecha | HU28 |
| `POST /administradores` | Ajustar | HU28 |
| `GET /conductores` | Ajustar | HU15 |
| `GET /conductores/{correo}` | Ajustar | HU15 |
| `POST /conductores` | Ajustar | HU15 |
| `PUT /conductores/{correo}` | Ajustar | HU15 |
| `POST /conductores/{correo}/activo` | Falta | HU26 |
| `GET /solicitantes` | Ajustar | HU27 |
| `GET /solicitantes/{correo}` | Ajustar | HU01 |
| `POST /solicitantes` | Ajustar | HU01 |
| `POST /solicitantes/{correo}/aprobar` | Falta | HU27 |
| `POST /solicitantes/{correo}/rechazar` | Falta | HU27 |
| `GET /empresas` | Ajustar (orden) | HU27 |
| `GET /empresas/{rut}` | Hecha | HU27 |
| `GET /moviles` | Ajustar | HU17 |
| `GET /moviles/{patente}` | Ajustar | HU17 |
| `POST /moviles` | Ajustar | HU17 |
| `POST /moviles/{patente}/activo` | Falta | HU26 |
| `GET /habilitaciones-movil-conductor` | Ajustar | HU18 |
| `GET /habilitaciones-movil-conductor/{patente}/{correo}` | Ajustar | HU18 |
| `POST /habilitaciones-movil-conductor` | Ajustar | HU18 |
| `GET /pagos` | Ajustar | HU19 |
| `GET /pagos/{hash}` | Ajustar | HU19 |
| `POST /pagos` | Ajustar | HU19 |
| `POST /pagos/{hash}/estado` | Ajustar | HU20 |
| `GET /pasajeros` | Ajustar (orden) | HU02 |
| `GET /pasajeros/{dni_hash}` | Hecha | HU02 |
| `POST /pasajeros` | Ajustar | HU02 |
| `GET /pasajeros-viaje` | Hecha | HU02 |
| `GET /pasajeros-viaje/{folio}/{dni_hash}` | Hecha | HU02 |
| `POST /pasajeros-viaje` | Ajustar | HU02 |
| `GET /viajes` | Ajustar | HU04 |
| `GET /viajes/{folio}` | Ajustar | HU08 |
| `POST /viajes` | Ajustar | HU02 |
| `POST /viajes/{folio}/asignar` | Ajustar | HU05 |
| `POST /viajes/{folio}/estado` | Ajustar | HU09 |
| `POST /viajes/{folio}/cancelar` | Ajustar | HU16 (V5) y HU24 (V7) |
| `POST /viajes/{folio}/rechazar` | Falta | HU25 |
| `POST /abordajes` | Falta | HU29 |

**Totales:**
- **Por estado:** 7 hechas, 32 por ajustar y 9 faltan.
- **Etapa 1:** tiene 46 rutas, de las que hay que dejar al menos 36 cumpliendo el README con
  pruebas.
- **Por integrante:** las HU de A tienen 42 rutas y las de B, 6. Las de B son las de reglas más
  complejas (V2, V3, V8 y el abordaje).

## Riesgos

| Riesgo | Mitigación |
|---|---|
| **Plazo:** 8 días para 81 SP, la base común y el 75 % del backend. | Seguir los hitos: T06 deja la UI navegable el 22-10 aunque falten reglas. La línea de corte recorta P4 y luego P3. |
| **Backend desigual:** A tiene 42 de las 48 rutas y la mayor parte de P3. | B hace la base del backend (T07 a T10). Como el backend no se presenta ni se pregunta en la Prueba 2, B puede tomar rutas de A, previo acuerdo, sin afectar que cada uno explique su Android. |
| **Especificación más reciente que el código:** el backend es anterior al README, y sus pruebas esperan el comportamiento viejo. | Cada ítem ajusta código y pruebas juntos. Primero T07 y T08, porque casi todo depende del modelo nuevo. |
| **Pruebas sin verificar:** las IT no se corrieron en esta revisión. | Antes de empezar, levantar el compose y correr `./gradlew test` para saber qué pruebas fallan hoy. |
| **Evaluación individual:** cada uno debe explicar y modificar su Android sin apuntes ni IA. | Cada integrante escribe el Android de sus HU (README, [Plan de trabajo](README.md#plan-de-trabajo)), y T12 ensaya la explicación y un cambio en vivo. |
