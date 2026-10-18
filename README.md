# Aplicacion de Belmonte Spa.
## Especificaciones del proyecto.

**Ultima modificacion:** 18-10-2026

## Integrantes:
* Daniel Muñoz
* Michel Sanhueza

## Indice

- [Introduccion](#introduccion)
  - [Problemática y actores](#problemática-y-actores)
- [Máquinas de estado](#máquinas-de-estado)
- [Requisitos](#requisitos)
  - [Casos de uso](#casos-de-uso)
  - [Requerimientos funcionales](#requerimientos-funcionales)
  - [Requisitos no funcionales](#requisitos-no-funcionales)
  - [Historias de usuario y Backlog](#historias-de-usuario-y-backlog)
- [Modelamiento del problema](#modelamiento-del-problema)
  - [Modelo de datos](#modelo-de-datos)
  - [Decisiones de modelo](#decisiones-de-modelo)
  - [Modelo relacional](#modelo-relacional)
- [Arquitectura](#arquitectura)
  - [Stack](#stack)
  - [Servicios locales](#servicios-locales)
  - [Entornos](#entornos)
- [API REST](#api-rest)
  - [Convenciones](#convenciones)
  - [Recursos y roles](#recursos-y-roles)
  - [Consultas y alcance por rol](#consultas-y-alcance-por-rol)
  - [Flujo de una solicitud (CU-01 / RF02)](#flujo-de-una-solicitud-cu-01--rf02)
  - [Ubicaciones desde Google Places](#ubicaciones-desde-google-places)
  - [Acciones de negocio](#acciones-de-negocio)
  - [Autenticación](#autenticación)
  - [Contratos JSON](#contratos-json)
  - [Validaciones](#validaciones)
  - [Errores](#errores)
- [Estructura del backend](#estructura-del-backend)
  - [Paquetes y capas](#paquetes-y-capas)
  - [Persistencia MariaDB](#persistencia-mariadb)
  - [Configuración](#configuración)
  - [Notificaciones externas](#notificaciones-externas)
  - [Pruebas](#pruebas)
- [Frontend (Android)](#frontend-android)
  - [Arquitectura y responsabilidades](#arquitectura-y-responsabilidades)
  - [Tecnologías](#tecnologías)
  - [Organización del código](#organización-del-código)
  - [Navegación y pantallas](#navegación-y-pantallas)
  - [Textos y tema](#textos-y-tema)
  - [API y sesión](#api-y-sesión)
  - [Persistencia y conectividad](#persistencia-y-conectividad)
  - [Base de datos local](#base-de-datos-local)
  - [Notificaciones y navegación externa](#notificaciones-y-navegación-externa)
  - [Compilación, firma y distribución](#compilación-firma-y-distribución)
  - [Pruebas Android](#pruebas-android)
  - [Decisiones abiertas](#decisiones-abiertas)

## Introduccion

Especificación de referencia del proyecto: requisitos, arquitectura, contratos y
tecnologías. No registra el avance de implementación ni certifica que el código cumpla lo
descrito; su revisión evalúa la coherencia y suficiencia de la especificación, y la
conformidad del código se revisa aparte.

Belmonte SpA hace transporte privado corporativo (traslados ejecutivos y aeropuerto). El MVP
móvil centraliza el flujo: captura de solicitudes, asignación a conductores y enlace con
navegación externa (Google Maps/Waze).

### Problemática y actores

Hoy las solicitudes se coordinan a mano (teléfono, correo, WhatsApp): datos críticos
erróneos (horarios, direcciones), respuestas lentas y retraso en que el conductor reciba su ruta.

| ID | Actor | Rol |
|---|---|---|
| ACT-01 | Cliente corporativo / Solicitante | Agenda el traslado (datos del viaje y del pasajero). |
| ACT-02 | Pasajero | Quien viaja; puede no ser el solicitante. |
| ACT-03 | Recepcionista / Administrador | Asigna solicitudes a conductores; gestiona flota (vehículos y habilitaciones) y pagos. |
| ACT-04 | Conductor | Ejecuta el traslado; ve sus viajes del día y abre la ruta en un navegador externo. |
| ACT-05 | Sistema de navegación externo (Google Maps / Waze) | Recibe las coordenadas por Intent y traza la ruta. Sin tabla: no hay datos propios que persistir. |

El Anexo de Requerimiento (§2.4) agrupa Solicitante y Pasajero; aquí se separan porque el
Pasajero no pertenece necesariamente a la empresa que paga (`PASAJERO` no tiene FK a
`EMPRESA`) y el Solicitante puede no viajar.

## Máquinas de estado

Fuente única de las transiciones; los CU, RF, HU y la API remiten aquí. Toda transición
inválida responde 409 y los triggers de `db/migration` la vuelven a validar.

### Viaje

`ESTADO_SOLICITUD`: `PENDIENTE → ASIGNADO → EN_CURSO → FINALIZADO`, más `CANCELADO` desde
`PENDIENTE` o `ASIGNADO`. Sin reapertura; el único retroceso es el rechazo del Conductor (V8,
`ASIGNADO` → `PENDIENTE`). `EN_CURSO` y `FINALIZADO` no se cancelan, reasignan ni rechazan. Cada
cambio registra su fecha/hora.

| ID | Transición | Quién | Precondiciones | Efectos |
|---|---|---|---|---|
| V1 | alta → `PENDIENTE` | Solicitante aprobado (CU-01/RF02) | `fh_agenda` de 2 horas a 90 días desde ahora ([Validaciones](#validaciones); si no, 400) | La base genera el `folio` (`TR-` y un correlativo, inmutable; ver `VIAJE.folio`); `fh_creacion`. Push a los Administradores. |
| V2 | `PENDIENTE`/`ASIGNADO` → `ASIGNADO` (asignar o reasignar) | Administrador (CU-04/RF05/RF22) | Al menos un pasajero (`cantidad_pasajeros` ≥ 1). Conductor y vehículo activos y Disponibles (sin otro viaje `EN_CURSO`); conductor habilitado para el vehículo; pasajeros ≤ capacidad. Al reasignar, la disponibilidad se valida solo para lo que cambia y la habilitación para el par resultante. Sin reasignación automática. | `fh_asignacion`. No cambia el estado del conductor ni del vehículo: ambos pueden tener varios viajes `ASIGNADO`. Al reasignar puede cambiar solo el conductor, solo el vehículo o ambos; el viaje sale de la agenda de los reemplazados. Push al conductor (nuevo), al solicitante y, si cambia el conductor, al reemplazado. |
| V3 | `ASIGNADO` → `EN_CURSO` | Conductor asignado (RF09) | Dentro de la ventana de inicio: de `fh_agenda` − 1 hora a `fh_agenda` + 1 hora. Conductor y vehículo Disponibles (sin otro viaje `EN_CURSO`). | `fh_inicio`; conductor → Ocupado, vehículo → En uso. Push al solicitante. |
| V4 | `EN_CURSO` → `FINALIZADO` | Conductor asignado (RF09) | — | `fh_fin`; conductor y vehículo → Disponible. Push al solicitante. |
| V5 | `PENDIENTE`/`ASIGNADO` → `CANCELADO` (cancelar) | Solicitante dueño (CU-08/RF15) | Viaje no vencido (ver Retraso). | Si había conductor: push al conductor y el viaje sale de su agenda ("Viajes del día" si es de hoy). No cambia el estado de conductor ni vehículo (en `PENDIENTE`/`ASIGNADO` este viaje no los ocupa). Pago `PAGADO` → `REEMBOLSADO` con `monto_reembolsado` = 50 % del monto, redondeado hacia abajo (P4); en otro estado el pago no cambia. No modifica `fh_fin`: el momento queda en `HISTORIAL_VIAJE`. |
| V6 | `PENDIENTE`/`ASIGNADO` → `CANCELADO` (por anulación del pago) | Sistema, al anular el Administrador el pago (P2 o P3; CU-12/RF19) | — | Mismos efectos de V5 (agenda y push al conductor) sin reembolso, y push al solicitante. |
| V7 | `PENDIENTE`/`ASIGNADO` → `CANCELADO` (por el Administrador) | Administrador (CU-15/RF23) | `motivo` obligatorio. | Guarda el `motivo`; agenda y push al conductor como en V5, y push al solicitante. Pago `PAGADO` → `REEMBOLSADO` (P4): con el viaje no vencido, `monto_reembolsado` = monto (100 %); vencido, el monto que fije el Administrador entre 0 y el monto (con 0 el pago no cambia). |
| V8 | `ASIGNADO` → `PENDIENTE` (rechazo) | Conductor asignado (CU-16/RF24) | `motivo` obligatorio; solo con conexión (no va a la cola sin conexión). | Guarda el `motivo`; deja en NULL conductor, móvil y `fh_asignacion`; el viaje sale de su agenda. Push a los Administradores y al solicitante. El pago no cambia. |

`CANCELADO` se suma a los 4 estados del Anexo (§3.2) para conservar la auditoría: la
solicitud no se borra.

**Retraso.** Se calcula a partir de `fh_agenda` y no es un estado: lo calculan el backend al
validar y la app al mostrar, incluso sin conexión. Un viaje `PENDIENTE` o `ASIGNADO` está:

- **Atrasado**, si ya pasó `fh_agenda` y no pasó `fh_agenda` + 1 hora: todavía puede iniciarse.
- **Vencido**, si pasó `fh_agenda` + 1 hora sin iniciarse: no se inicia ni lo cancela el
  Solicitante; nada cambia solo, y el Administrador decide (reasignarlo no reabre la ventana:
  lo cancela con V7 y decide el reembolso).

### Pago

`ESTADO_PAGO`. Relación 1:1 con el viaje: un viaje tiene a lo más un pago y un pago pertenece a un solo viaje.

| ID | Desde | Hacia | Quién / cómo |
|---|---|---|---|
| P1 | alta | `PENDIENTE` | Administrador (CU-11/RF18), para un viaje en cualquier estado distinto de `CANCELADO`; el backend genera su `hash` (único; ver `PAGO.hash`). |
| P2 | `PENDIENTE` | `PAGADO`, `RECHAZADO` o `ANULADO` | Manual, Administrador (CU-12/RF19). Al pasar a `PAGADO`, el backend fija `fecha_pago`. Push al solicitante. |
| P3 | `PAGADO` | `ANULADO` | Manual, Administrador. Push al solicitante. |
| P4 | `PAGADO` | `REEMBOLSADO` | Automático al cancelar el viaje: en V5 (Solicitante, 50 %) o en V7 (Administrador, 100 % o el monto que fije si el viaje está vencido). Fija `monto_reembolsado`; nunca manual. Push al solicitante. |

- `RECHAZADO`, `ANULADO` y `REEMBOLSADO` son terminales.
- `ANULADO` por P2 o P3 (problema con el banco u otro motivo) cancela el viaje si está `PENDIENTE` o `ASIGNADO` (V6).
- Sin re-cobro: el pago no se reemplaza ni se registra otro. Un viaje con su pago `RECHAZADO`, o
  `ANULADO` estando `EN_CURSO` o `FINALIZADO`, queda sin cobro (`POST /pagos` responde 409 porque ya tiene pago).

### Conductor y vehículo

- **Conductor** (`ESTADO_CONDUCTOR`): nace Disponible (CU-07); Ocupado solo mientras tiene un
  viaje `EN_CURSO` (de V3 a V4).
- **Vehículo** (`ESTADO_MOVIL`): nace Disponible (CU-09); En uso solo mientras tiene un viaje
  `EN_CURSO` (de V3 a V4).
- Disponible = sin viaje `EN_CURSO`. Asignar (V2) no cambia estos estados: ambos pueden tener
  varios viajes `ASIGNADO` (su agenda), pero a lo más uno `EN_CURSO`.
- **Activo** (`activo`, independiente de lo anterior): nacen activos. El Administrador los
  desactiva (baja de un conductor desvinculado o de un vehículo en taller) solo si no tienen
  viajes `ASIGNADO` ni `EN_CURSO` (409), y los puede reactivar (CU-07/CU-09/RF25). Un inactivo no
  es asignable (V2) ni aparece en "Asignar"; un Conductor inactivo no inicia sesión (mismo 401
  genérico) y al desactivarlo se revocan sus sesiones. Las habilitaciones no cambian.

## Requisitos

### Casos de uso

| CU | Actor | Flujo | Excepciones |
|---|---|---|---|
| CU-01 Registrar solicitud de traslado | ACT-01 | Ingresa pasajero(s), fecha/hora, origen, destino y n.º de vuelo (si aplica) → viaje `PENDIENTE` con sus pasajeros, en tres llamadas ([Flujo de una solicitud](#flujo-de-una-solicitud-cu-01--rf02)). | Datos incompletos o inválidos: 400 según [Validaciones](#validaciones). |
| CU-02 Visualizar viajes asignados | ACT-04 | Lista sus viajes asignados del día, actualizados desde el backend. | Sin conectividad: muestra los viajes ya sincronizados (RF11/HU11). Sin viajes: lista vacía. |
| CU-03 Navegar a la ruta del viaje | ACT-04 | "Navegar" en un viaje con coordenadas → Intent con origen y destino → el SO lo entrega a una app compatible, que navega. | No hay app compatible o el Intent no puede procesarse. |
| CU-04 Gestionar y distribuir solicitudes | ACT-03 | Elige una solicitud y un conductor disponible (se sugieren sus vehículos habilitados) y confirma: [V2](#viaje) `PENDIENTE` → `ASIGNADO`. | Sin conductores disponibles. Sin reasignación automática; la manual de un `ASIGNADO` sigue el mismo flujo (RF22). |
| CU-05 Actualizar estado del traslado | ACT-03, ACT-04 | Ciclo [V2](#viaje) `PENDIENTE` → `ASIGNADO` (el Administrador asigna), [V3](#viaje) `ASIGNADO` → `EN_CURSO` y [V4](#viaje) `EN_CURSO` → `FINALIZADO` (el Conductor inicia y finaliza). | Sin retroceso ni reapertura; la cancelación es CU-08 (o la anulación del pago, CU-12). |
| CU-06 Trazar ruta óptima hacia el destino | ACT-05 | Con coordenadas válidas, calcula la ruta con el tráfico disponible y la muestra al conductor. | Algoritmo, proveedor de tráfico y criterios son del sistema externo. |
| CU-07 Registrar conductor | ACT-03 | El Administrador ingresa nombre, correo (único), contraseña inicial (se guarda como hash) y teléfono opcional; la cuenta queda activa para autenticarse y, una vez habilitada para un vehículo (CU-10), para ser asignada (CU-04). Sin autorregistro. Desde su ficha el Administrador lo desactiva o reactiva ([Conductor y vehículo](#conductor-y-vehículo), RF25). | Correo existente o campo faltante: rechazo. Desactivar con viajes `ASIGNADO` o `EN_CURSO`: 409. El Conductor no tiene "cambiar contraseña": el Administrador la reemplaza en su ficha. |
| CU-08 Cancelar solicitud de traslado | ACT-01 | Solicitud propia en `PENDIENTE` o `ASIGNADO`: "Cancelar" en el detalle → [V5](#viaje) `PENDIENTE`/`ASIGNADO` → `CANCELADO`, con fecha/hora. | En `EN_CURSO` o `FINALIZADO`, o con el viaje vencido, no hay botón y se rechaza (409). |
| CU-09 Registrar vehículo | ACT-03 | Patente (única), capacidad y serial GPS opcional (dato de flota; el MVP no registra ubicación en vivo); queda Disponible, activo, habilitable y asignable. Desde su ficha el Administrador lo desactiva (taller) o reactiva (RF25). | Patente existente o campo faltante: rechazo. Desactivar con viajes `ASIGNADO` o `EN_CURSO`: 409. |
| CU-10 Habilitar conductor para vehículo | ACT-03 | Relaciona un conductor (CU-07) con uno o más vehículos (CU-09); M:N. | Relación existente: se ignora sin duplicar. |
| CU-11 Registrar pago de un viaje | ACT-03 | En un viaje no `CANCELADO`: monto, banco, medio de pago y tipo de recibo → pago `PENDIENTE` asociado al viaje. | Viaje `CANCELADO`: no se registra. |
| CU-12 Actualizar estado de un pago | ACT-03 | Sobre un pago `PENDIENTE` (o `PAGADO`, solo para anularlo) marca `PAGADO`, `RECHAZADO` o `ANULADO` ([P2](#pago) desde `PENDIENTE`; [P3](#pago) `PAGADO` → `ANULADO`); anular cancela el viaje `PENDIENTE` o `ASIGNADO` ([V6](#viaje)). | Estado terminal: no cambia. |
| CU-13 Recuperar contraseña | ACT-01, ACT-03 | Ingresa su correo en "Recuperar contraseña". Si es de un Solicitante o Administrador: contraseña temporal aleatoria, guardada hasheada en `password_temporal_hash` con `password_temporal_expira`, sin tocar la contraseña actual, y enviada en texto plano solo a ese correo vía `EmailClient` (único momento en claro; nunca se persiste así, RNF01). Sirve para el login (CU-14) hasta expirar y obliga a cambiar la contraseña al entrar. | Correo inexistente o de un Conductor: mismo mensaje genérico. Expirada sin uso: pedir otra. Pedidos repetidos: [límite](#autenticación) por correo, sin aviso. |
| CU-14 Iniciar sesión | ACT-01, ACT-03, ACT-04 | Cuenta autorregistrada (Solicitante, RF01), creada por el Administrador (Conductor, RF14; Administrador, RF27) o por migración Flyway (Administrador inicial). Envía correo, contraseña y el token FCM si existe (RF13). Se valida contra `USUARIO.password_hash` o contra la temporal vigente (`password_temporal_hash`, RF21), que obliga a cambiar la contraseña. El rol es la tabla (`SOLICITANTE`/`ADMINISTRADOR`/`CONDUCTOR`) donde existe el `id_usuario`, único por `USUARIO_ROL_ACTIVO`. Se guarda o reemplaza `USUARIO.token_fcm` y se emiten un JWT (`sub` = correo, `rol`, `exp`, `sid`; RNF01) y un refresh token ([Autenticación](#autenticación)); el cliente adjunta el JWT en cada request y el `SecurityFilterChain` lo valida aplicando el mapeo rol→endpoint. | Correo inexistente, contraseña incorrecta, temporal expirada o Conductor inactivo: mismo error genérico. Solicitante no aprobado: 403 "Cuenta pendiente de aprobación" o "Cuenta rechazada" (CU-17). 5 fallos en 15 minutos bloquean el correo (429). |
| CU-15 Cancelar un viaje (Administrador) | ACT-03 | En el detalle de un viaje `PENDIENTE` o `ASIGNADO`: "Cancelar viaje", motivo y, si el viaje está vencido, monto a reembolsar → [V7](#viaje), con reembolso según [P4](#pago). | `EN_CURSO`, `FINALIZADO` o `CANCELADO`: no hay botón y se rechaza (409). Sin motivo: 400. |
| CU-16 Rechazar una asignación | ACT-04 | En el detalle de un viaje propio `ASIGNADO`, antes de iniciarlo: "Rechazar", motivo → [V8](#viaje); el viaje vuelve a `PENDIENTE` y sale de su agenda. | Sin conexión: no se ofrece (no se encola). Sin motivo: 400. |
| CU-17 Aprobar un Solicitante | ACT-03 | Quien se registra con el RUT de una empresa existente queda `PENDIENTE` (RF01). El Administrador lo ve en "Cuentas por aprobar" y lo aprueba (puede crear solicitudes) o rechaza (no inicia sesión). | Cuenta ya revisada: 409. |
| CU-18 Registrar un Administrador | ACT-03 | Un Administrador ingresa nombre, correo, teléfono opcional y una contraseña inicial, que queda como temporal por `ALTA_TEMPORAL_HORAS` y obliga a cambiarla al entrar. | Correo existente o campo faltante: rechazo. |

El Conductor queda fuera de CU-13 a propósito: solo el Administrador reemplaza su contraseña
(CU-07/RF14), para que un desvinculado no recupere acceso por su cuenta.

### Requerimientos funcionales

"Sistema" = requisito sin actor que lo inicie.

| ID | Actor | Requisito |
|---|---|---|
| **RF01** | Solicitante | Registrarse (razón social y RUT de su empresa crean o reutilizan una fila en `EMPRESA`) y autenticarse (CU-14). Con un RUT nuevo la cuenta queda aprobada; con uno existente, `PENDIENTE` hasta que la apruebe un Administrador (CU-17/RF26), y se conserva la razón social registrada. |
| **RF02** | Solicitante | Registrar una solicitud: pasajero, fecha, hora, origen y destino (autocompletado de Google Places, con coordenadas) y n.º de vuelo si aplica. |
| **RF03** | Solicitante | Ver el historial y estado de sus solicitudes. |
| **RF04** | Administrador | Ver las solicitudes pendientes de asignación. |
| **RF05** | Administrador | Asignar a un viaje con al menos un pasajero un conductor activo y disponible y un vehículo activo habilitado para él: la solicitud pasa a Asignado ([V2](#viaje) `PENDIENTE` → `ASIGNADO`); conductor y vehículo pasan a Ocupado/En uso recién al iniciar ([V3](#viaje)). |
| **RF06** | Conductor | Autenticarse con credenciales asignadas por el Administrador, sin autorregistro (CU-14). |
| **RF07** | Conductor | Ver en tiempo real los viajes asignados del día. |
| **RF08** | Conductor | Ver el detalle de un viaje asignado (pasajero, origen, destino). |
| **RF09** | Conductor | Pasar un traslado a En Curso, dentro de la ventana de una hora antes a una hora después de la agendada, y a Finalizado; al finalizar, conductor y vehículo vuelven a Disponible. |
| **RF10** | Sistema | Abrir por Intents de Android una app de navegación externa (Google Maps/Waze) que trace la ruta desde las coordenadas. |
| **RF11** | Sistema | Persistir localmente (Room/SQLite) solicitudes y viajes ya consultados para verlos sin conexión; los cambios offline (p. ej. de estado) van a una cola local y se sincronizan por API REST al reconectar. |
| **RF12** | Sistema | Push (FCM) a los actores afectados ante cambios relevantes (nueva solicitud, asignación o reasignación, rechazo, En Curso, Finalizado, cancelación, cambios del pago y cuentas por aprobar), sin recarga manual. |
| **RF13** | Sistema | Registrar y mantener actualizado el token FCM de cada usuario autenticado. |
| **RF14** | Administrador | Registrar una cuenta de Conductor (nombre, correo, contraseña inicial; teléfono opcional), utilizable de inmediato. |
| **RF15** | Solicitante | Cancelar una solicitud propia en Pendiente o Asignado: el viaje sale de la agenda del conductor, si había uno, y se le avisa por push; un pago Pagado pasa a Reembolsado con devolución del 50 % del monto. No se puede con el viaje vencido ([V5](#viaje) `PENDIENTE`/`ASIGNADO` → `CANCELADO`; [P4](#pago) `PAGADO` → `REEMBOLSADO`). |
| **RF16** | Administrador | Registrar un vehículo con patente y capacidad; serial GPS opcional, como dato de flota (el MVP no registra ubicación en vivo). |
| **RF17** | Administrador | Habilitar uno o más conductores para un vehículo. |
| **RF18** | Administrador | Registrar el pago de un viaje en cualquier estado previo a Cancelado (monto, banco, medio de pago, tipo de recibo); nace en Pendiente. |
| **RF19** | Administrador | Marcar un pago Pagado, Rechazado o Anulado (p. ej. problema con el banco); Anulado cancela el viaje en Pendiente o Asignado. Reembolsado lo fija el sistema (RF15). |
| **RF20** | Solicitante | Ver el estado de pago de sus viajes en el detalle de la solicitud. |
| **RF21** | Solicitante, Administrador | Recuperar la contraseña con su correo (CU-13): temporal enviada al correo, con expiración, que no reemplaza la actual y obliga a cambiarla al entrar. El Conductor no tiene este autoservicio (solo RF14). |
| **RF22** | Administrador | Reasignar una solicitud Asignado (conductor, vehículo o ambos) con las reglas de RF05; sigue Asignado, el viaje sale de la agenda de los reemplazados y se avisa por push al nuevo conductor, al conductor reemplazado y al solicitante (RF12). |
| **RF23** | Administrador | Cancelar un viaje Pendiente o Asignado con motivo: un pago Pagado se reembolsa al 100 %, o en el monto que decida si el viaje está vencido ([V7](#viaje), [P4](#pago)). |
| **RF24** | Conductor | Rechazar con motivo un viaje Asignado antes de iniciarlo: vuelve a Pendiente y se avisa a los Administradores y al solicitante ([V8](#viaje)). |
| **RF25** | Administrador | Desactivar y reactivar conductores y vehículos sin viajes Asignado ni En Curso ([Conductor y vehículo](#conductor-y-vehículo)). |
| **RF26** | Administrador | Aprobar o rechazar a los Solicitantes que se registran en una empresa existente (CU-17). |
| **RF27** | Administrador | Registrar otra cuenta de Administrador con contraseña inicial temporal (CU-18). |

### Requisitos no funcionales

- **RNF01 Seguridad:** contraseñas solo como hash; app↔backend con JWT de acceso de 1 hora y refresh token
  rotativo y revocable (CU-14); límites de intentos en las rutas públicas; autorización por rol en un `SecurityFilterChain` centralizado. Única excepción al
  "nunca en texto plano": la contraseña temporal de RF21, enviada una vez por correo y nunca
  guardada en claro.
- **RNF02 Conectividad:** Internet continuo (Anexo §3.3); no es offline-first y crear o
  recibir una solicitud nueva exige conexión. Tolera cortes breves para datos ya
  sincronizados (Room/SQLite) y para los cambios de estado del Conductor (En Curso/Finalizado)
  de un viaje ya asignado, que se encolan y reintentan al reconectar (RF11).
- **RNF03 Tiempo de respuesta:** consultas (solicitudes, viajes del día) < 2 s con 4G/Wi-Fi;
  un push (RF12/RF13) se refleja en segundos, no minutos.
- **RNF04 Usabilidad en terreno:** pantallas del Conductor con botones grandes y legibles y
  mínimos pasos para En Curso/Finalizado o abrir la navegación.
- **RNF05 Compatibilidad:** Android 8.0 (API 26) o superior. El backend expone solo contratos
  JSON REST, sin acoplarse a un cliente.
- **RNF06 Mantenibilidad:** capas estrictas (`Controller-Service-Repository` en Spring Boot,
  `View-ViewModel-Repository` en Android): una regla de negocio se cambia en un solo lugar.
- **RNF07 Servicios externos:** tres dependencias con cuota gratuita limitada (Google Places
  API, Firebase Cloud Messaging y un proveedor de correo transaccional por SMTP, para RF21/CU-13
  y CU-17); el uso se monitorea para no exceder el nivel gratuito.
- **RNF08 Privacidad:** nombre, teléfono y dirección de pasajeros y solicitantes no van a
  logs, y solo los ven el Solicitante, el Administrador y el Conductor asignado de cada
  solicitud.
- **RNF09 Datos académicos:** solo datos ficticios, sintéticos, agregados o públicos
  anonimizados (Anexo §4; Ley N.º 21.719 y Ley N.º 19.628); los antecedentes se comparten por
  canales institucionales con acceso restringido.
- **RNF10 Entregable:** colaboración y documentación en Git/GitHub; el ejecutable final es un
  APK firmado en modo release (Anexo, "Alcance tecnológico comprometido"), publicado en GitHub
  Releases ([Compilación, firma y distribución](#compilación-firma-y-distribución)).

### Historias de usuario y Backlog

Los efectos de estado se citan por el ID de su transición en [Máquinas de estado](#máquinas-de-estado) (V1–V6 viaje, P1–P4 pago) y forman parte del criterio.

| HU | RF | Prio. | SP | Historia | Criterios de aceptación |
|---|---|---|---|---|---|
| HU01 | RF01 | Alta | 3 | Solicitante: registro y login | Rechazo si el correo existe o falta un obligatorio (empresa, nombre de contacto, correo, contraseña); teléfono opcional. Login válido → "Mis solicitudes"; inválido → error sin indicar qué falló. La empresa (razón social + RUT) se crea o reutiliza en `EMPRESA`, sin duplicar; con un RUT existente la cuenta queda pendiente de aprobación (HU27) y el login responde 403 hasta entonces. |
| HU02 | RF02 | Alta | 5 | Solicitante: nueva solicitud con Google Places | Origen y destino solo desde las sugerencias (nunca texto libre), capturando latitud y longitud; sin eso no se envía. Obligatorios: al menos un pasajero (nombre y documento, RUT o pasaporte, con su `tipoDocumento`), fecha, hora, origen y destino; vuelo opcional. La app los exige antes de enviar y registra viaje y pasajeros en llamadas separadas. |
| HU03 | RF03 | Media | 3 | Solicitante: historial y estado | Por solicitud propia: pasajero, fecha/hora, origen→destino y estado (el del último cambio). |
| HU04 | RF04 | Alta | 3 | Administrador: pendientes | Solo `PENDIENTE`; cada fila lleva a "Asignar conductor". |
| HU05 | RF05 | Alta | 5 | Administrador: asignar | Solo conductores disponibles (sin viaje en curso). Al confirmar, [V2](#viaje) `PENDIENTE` → `ASIGNADO` con un vehículo habilitado para él, y el viaje aparece en la agenda del conductor ("Viajes del día" si es de hoy). |
| HU06 | RF06 | Alta | 2 | Conductor: login con credenciales del Administrador | Sin "crear cuenta"; credenciales inválidas no revelan si el correo existe. |
| HU07 | RF07 | Alta | 3 | Conductor: viajes del día en tiempo real | Solo los suyos, de la fecha actual, por hora; con pasajero, hora y estado. |
| HU08 | RF08 | Alta | 3 | Conductor: detalle del viaje | Pasajero, vuelo (si aplica), origen y destino completos; "Navegar" y controles de estado. |
| HU09 | RF09 | Alta | 3 | Conductor: En Curso y Finalizado | "Iniciar viaje" ([V3](#viaje)) solo en `ASIGNADO` y dentro de la ventana de `fh_agenda` ± 1 hora, "Finalizar viaje" ([V4](#viaje)) solo en `EN_CURSO`; cada cambio registra fecha/hora; al finalizar, conductor y vehículo → Disponible. |
| HU10 | RF10 | Alta | 3 | Conductor: ruta en Google Maps o Waze | "Navegar" los abre con el destino precargado; sin app instalada, aviso en vez de fallar en silencio. |
| HU11 | RF11 | Media | 8 | Conductor: uso sin conexión | Lo ya cargado se consulta offline; un cambio de estado offline va a una cola local marcado "pendiente de sincronizar" y se envía solo al reconectar. |
| HU12 | RF12 | Media | 5 | Solicitante: push de estado | Push al pasar a `ASIGNADO`, `EN_CURSO` o `FINALIZADO`; al abrirlo o abrir "Mis solicitudes", el estado ya está actualizado. |
| HU13 | RF12 | Alta | 5 | Conductor: push de asignación | Push al asignársele; al abrirlo, el viaje está en su agenda ("Viajes del día" si es de hoy) con datos completos. |
| HU14 | RF13 | Alta | 3 | Todos: token de notificaciones | El login registra o actualiza el token FCM; si cambia (reinstalación, otro dispositivo), reemplaza al anterior sin duplicar. |
| HU15 | RF14 | Alta | 3 | Administrador: alta de Conductor | Rechazo si el correo existe o falta un obligatorio; teléfono opcional. El Administrador define la contraseña (no se genera sola), guardada como hash. Aparece en "Asignar conductor" (HU05) en cuanto se lo habilita para un vehículo (HU18). Sin "cambiar contraseña" para el Conductor: la reemplaza el Administrador en su ficha. |
| HU16 | RF15 | Alta | 3 | Solicitante: cancelar | "Cancelar" solo en `PENDIENTE` o `ASIGNADO` y con el viaje no vencido; [V5](#viaje) `PENDIENTE`/`ASIGNADO` → `CANCELADO` registra fecha/hora, avisa al conductor (RF12), saca el viaje de su agenda y reembolsa el 50 % de un pago `PAGADO` ([P4](#pago); otro estado no cambia). |
| HU17 | RF16 | Media | 3 | Administrador: alta de vehículo | Patente obligatoria y única; capacidad > 0; GPS serial opcional, como dato de flota; nace Disponible y activo. |
| HU18 | RF17 | Media | 2 | Administrador: habilitaciones | Solo conductores registrados (HU15); M:N; una relación existente no se duplica. |
| HU19 | RF18 | Media | 5 | Administrador: registrar pago | Viaje en cualquier estado salvo `CANCELADO`; hash único; banco, medio de pago y tipo de recibo desde catálogos; nace `PENDIENTE`. |
| HU20 | RF19 | Media | 3 | Administrador: estado del pago | Transiciones [P2](#pago) `PENDIENTE` → `PAGADO`/`RECHAZADO`/`ANULADO`, [P3](#pago) `PAGADO` → `ANULADO` y [P4](#pago) `PAGADO` → `REEMBOLSADO` (solo automática, HU16); `RECHAZADO`, `ANULADO` y `REEMBOLSADO` terminales. Anular cancela el viaje `PENDIENTE` o `ASIGNADO` ([V6](#viaje)) con los efectos de HU16 (agenda y push al conductor). |
| HU21 | RF20 | Baja | 2 | Solicitante: estado de pago | En "Detalle de solicitud" (HU03), solo si el viaje tiene pago. |
| HU22 | RF21 | Media | 3 | Solicitante/Administrador: recuperar contraseña | Solo esos roles; cualquier otro correo recibe el mismo mensaje genérico. La temporal no reemplaza la actual y expira; usarla obliga a cambiar la contraseña; sin uso, se pide otra. |
| HU23 | RF22 | Media | 3 | Administrador: reasignar | [V2](#viaje) `ASIGNADO` → `ASIGNADO`, solo en `ASIGNADO`. Cambia conductor, vehículo o ambos con las reglas de HU05; sigue `ASIGNADO`; el viaje sale de la agenda de los reemplazados; push al nuevo conductor, al reemplazado y al solicitante (RF12); queda en `HISTORIAL_VIAJE`. |
| HU24 | RF23 | Alta | 3 | Administrador: cancelar un viaje | Solo en `PENDIENTE` o `ASIGNADO`; motivo obligatorio. [V7](#viaje): push al conductor y al solicitante; pago `PAGADO` reembolsado al 100 % o, con el viaje vencido, en el monto que se indique (0 lo deja `PAGADO`). |
| HU25 | RF24 | Media | 2 | Conductor: rechazar una asignación | Solo `ASIGNADO`, antes de iniciar y con conexión; motivo obligatorio. [V8](#viaje): vuelve a `PENDIENTE`, sale de su agenda y avisa a los Administradores y al solicitante. |
| HU26 | RF25 | Media | 3 | Administrador: dar de baja | Desactivar o reactivar un conductor o un vehículo desde su ficha; 409 con viajes `ASIGNADO` o `EN_CURSO`. Inactivo: no aparece en "Asignar"; un Conductor inactivo no inicia sesión. |
| HU27 | RF26 | Media | 3 | Administrador: aprobar solicitantes | "Cuentas por aprobar" lista los `PENDIENTE` con su empresa; aprobar o rechazar avisa por correo al Solicitante. |
| HU28 | RF27 | Baja | 2 | Administrador: registrar otro Administrador | Nombre, correo y contraseña inicial; teléfono opcional. Al entrar debe cambiar la contraseña. |

## Modelamiento del problema

### Modelo de datos

MariaDB; el esquema lo crean las migraciones Flyway de `backend/src/main/resources/db/migration`.
Las PK simples usan `AUTO_INCREMENT`. `Nulo` es explícito en cada atributo; `UQ` = UNIQUE.

| Entidad | Atributo | Tipo | Nulo | Clave | Descripción |
|---|---|---|---|---|---|
| REGION | id_region | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| REGION | nombre | VARCHAR(60) | NOT NULL | UQ |  |
| COMUNA | id_comuna | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| COMUNA | nombre | VARCHAR(80) | NOT NULL | UQ compuesto (nombre, id_region) |  |
| COMUNA | id_region | BIGINT UNSIGNED | NOT NULL | FK → REGION |  |
| UBICACION | id_ubicacion | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| UBICACION | direccion | VARCHAR(200) | NOT NULL | UQ compuesto (direccion, id_comuna, latitud, longitud) | Dirección georreferenciada reutilizable como origen o destino (RF10). |
| UBICACION | id_comuna | BIGINT UNSIGNED | NOT NULL | FK → COMUNA |  |
| UBICACION | latitud | DECIMAL(9,6) | NOT NULL | UQ compuesto (direccion, id_comuna, latitud, longitud) |  |
| UBICACION | longitud | DECIMAL(9,6) | NOT NULL | UQ compuesto (direccion, id_comuna, latitud, longitud) |  |
| USUARIO | id_usuario | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| USUARIO | nombre | VARCHAR(120) | NOT NULL | — |  |
| USUARIO | correo | VARCHAR(150) | NOT NULL | UQ | Login. |
| USUARIO | telefono | VARCHAR(20) | NULL | — | Opcional, para todos los roles. |
| USUARIO | password_hash | VARCHAR(200) | NOT NULL | — | Contraseña normal (BCrypt). Nunca texto plano; recuperar la contraseña no la reemplaza. |
| USUARIO | password_temporal_hash | VARCHAR(200) | NULL | — | Contraseña temporal pendiente (RF21), BCrypt; NULL si no hay. Siempre NULL para CONDUCTOR. |
| USUARIO | password_temporal_expira | DATETIME | NULL | — | Vencimiento de `password_temporal_hash`; NULL junto con ella (RF21). Siempre NULL para CONDUCTOR. |
| USUARIO | fecha_registro | DATETIME | NOT NULL | DEFAULT CURRENT_TIMESTAMP |  |
| USUARIO | token_fcm | VARCHAR(255) | NULL | UQ | Token push del último dispositivo del usuario; NULL hasta el primer login desde la app o tras cerrar sesión. Se reemplaza en el login o con `POST /auth/token-fcm` y, al guardarlo, se quita de cualquier otro usuario (RF13). |
| USUARIO_ROL_ACTIVO | id_usuario | BIGINT UNSIGNED | NOT NULL | PK, FK → USUARIO | Exclusividad de rol: triggers `BEFORE INSERT` en `SOLICITANTE`/`ADMINISTRADOR`/`CONDUCTOR` insertan aquí primero; la PK rechaza un segundo rol. Sin columnas propias. |
| SESION | id_sesion | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT | Una por login con la contraseña normal o por renovación ([Autenticación](#autenticación)). |
| SESION | id_usuario | BIGINT UNSIGNED | NOT NULL | FK → USUARIO |  |
| SESION | refresh_hash | CHAR(64) | NOT NULL | UQ | SHA-256 hexadecimal del refresh token; el token nunca se guarda. |
| SESION | fh_creacion | DATETIME | NOT NULL | DEFAULT CURRENT_TIMESTAMP |  |
| SESION | fh_expira | DATETIME | NOT NULL | — | `fh_creacion` + `JWT_REFRESH_DIAS`. |
| SESION | fh_revocada | DATETIME | NULL | — | Rotación, logout, cambio de contraseña o reuso; NULL mientras está vigente. |
| EMPRESA | id_empresa | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| EMPRESA | razon_social | VARCHAR(50) | NOT NULL | — |  |
| EMPRESA | rut | VARCHAR(15) | NOT NULL | UQ | Identificador legal único. |
| SOLICITANTE | id_usuario | BIGINT UNSIGNED | NOT NULL | PK, FK → USUARIO | Extensión 1:1 (RF01). |
| SOLICITANTE | empresa_id_empresa | BIGINT UNSIGNED | NOT NULL | FK → EMPRESA |  |
| SOLICITANTE | estado_solicitante_id | BIGINT UNSIGNED | NOT NULL | FK → ESTADO_SOLICITANTE | `APROBADO` si creó la empresa; `PENDIENTE` si se unió a una existente, hasta que un Administrador lo apruebe o rechace (CU-17/RF26). |
| ESTADO_SOLICITANTE | id | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| ESTADO_SOLICITANTE | nombre | VARCHAR(20) | NOT NULL | UQ | PENDIENTE, APROBADO o RECHAZADO. |
| ADMINISTRADOR | id_usuario | BIGINT UNSIGNED | NOT NULL | PK, FK → USUARIO | Extensión 1:1, sin atributos propios. Se crea por migración Flyway de carga inicial; no hay autorregistro ni endpoint que lo cree. |
| ESTADO_CONDUCTOR | id_estado | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| ESTADO_CONDUCTOR | estado | VARCHAR(20) | NOT NULL | UQ | Disponible u Ocupado (ver [Conductor y vehículo](#conductor-y-vehículo)). |
| CONDUCTOR | id_usuario | BIGINT UNSIGNED | NOT NULL | PK, FK → USUARIO | Extensión 1:1. |
| CONDUCTOR | estado_conductor_id_estado | BIGINT UNSIGNED | NOT NULL | FK → ESTADO_CONDUCTOR | Estado operativo del Conductor: nace Disponible (CU-07); Ocupado solo con un viaje `EN_CURSO` (ver [Conductor y vehículo](#conductor-y-vehículo)). |
| CONDUCTOR | activo | BOOLEAN | NOT NULL | DEFAULT TRUE | Baja de un desvinculado (RF25): inactivo no inicia sesión ni es asignable. |
| ESTADO_MOVIL | id_estado_mov | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| ESTADO_MOVIL | nombre | VARCHAR(20) | NOT NULL | UQ | Disponible o En uso (ver [Conductor y vehículo](#conductor-y-vehículo)). |
| MOVIL | id_movil | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| MOVIL | patente | VARCHAR(6) | NOT NULL | UQ |  |
| MOVIL | gps_serial | INT | NULL | — | Opcional (RF16). |
| MOVIL | estado_movil_id_estado_mov | BIGINT UNSIGNED | NOT NULL | FK → ESTADO_MOVIL |  |
| MOVIL | capacidad | INT | NOT NULL | — | Pasajeros. |
| MOVIL | activo | BOOLEAN | NOT NULL | DEFAULT TRUE | Baja de un vehículo en taller o retirado (RF25): inactivo no es asignable. |
| DETALLE_MOVIL_CONDUCTOR | id_movil | BIGINT UNSIGNED | NOT NULL | PK, FK → MOVIL | Habilitación vehículo↔conductor. |
| DETALLE_MOVIL_CONDUCTOR | id_usuario | BIGINT UNSIGNED | NOT NULL | PK, FK → CONDUCTOR |  |
| PASAJERO | id_pasajero | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| PASAJERO | dni_hash | VARCHAR(50) | NOT NULL | UQ | Hash del documento de identidad: RUT chileno o pasaporte extranjero. Es el HMAC-SHA256 del documento normalizado (sin puntos, guion ni espacios, en mayúsculas), en Base64url sin relleno (43 caracteres). El cliente envía el documento en `dni` y el backend calcula el hash; nunca se guarda en claro. |
| PASAJERO | nombre | VARCHAR(120) | NOT NULL | — | Puede no coincidir con el solicitante (CU-01). |
| ESTADO_SOLICITUD | id_estado | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| ESTADO_SOLICITUD | nombre_estado | VARCHAR(20) | NOT NULL | UQ | PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO (RF05/RF09) y CANCELADO (RF15/CU-08). |
| BANCO | id | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| BANCO | nombre | VARCHAR(50) | NOT NULL | — |  |
| BANCO | cod_sbif | INT | NOT NULL | UQ | Código SBIF; identificador externo único. |
| ESTADO_PAGO | id | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| ESTADO_PAGO | estado | VARCHAR(20) | NOT NULL | UQ | Pendiente, Pagado, Rechazado, Anulado o Reembolsado (RF18/RF19). Anulado lo genera el pago (banco u otro motivo) y cancela el viaje si está Pendiente o Asignado (RF19, V6); Reembolsado es automático cuando el Solicitante cancela con el pago Pagado (RF15). |
| MEDIO_PAGO | id | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| MEDIO_PAGO | nombre | VARCHAR(20) | NOT NULL | UQ | P. ej. transferencia, tarjeta. |
| TIPO_RECIBO | id | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| TIPO_RECIBO | nombre | VARCHAR(20) | NOT NULL | UQ |  |
| PAGO | id | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| PAGO | hash | VARCHAR(50) | NOT NULL | UQ | Generado por el backend al crear el pago, sin pasarela externa: 32 caracteres hexadecimales en minúsculas, aleatorios (UUID v4 sin guiones). Es la clave de ruta y no se muestra al usuario: la app identifica el pago por el folio de su viaje. |
| PAGO | monto | INT | NOT NULL | — | Monto cobrado. |
| PAGO | estado_pago_id | BIGINT UNSIGNED | NOT NULL | FK → ESTADO_PAGO |  |
| PAGO | banco_id | BIGINT UNSIGNED | NOT NULL | FK → BANCO |  |
| PAGO | empresa_id_empresa | BIGINT UNSIGNED | NOT NULL | FK → EMPRESA | Empresa que paga. |
| PAGO | medio_pago_id | BIGINT UNSIGNED | NOT NULL | FK → MEDIO_PAGO |  |
| PAGO | tipo_recibo_id | BIGINT UNSIGNED | NOT NULL | FK → TIPO_RECIBO |  |
| PAGO | fecha_pago | DATETIME | NULL | — | Fecha de cobro: NULL hasta que el pago pasa a Pagado (P2), cuando la fija el backend. |
| PAGO | monto_reembolsado | INT | NULL | — | Monto devuelto al pasar a Reembolsado (P4): 50 % redondeado hacia abajo en V5; el monto completo o el que fije el Administrador en V7. NULL en otro estado. |
| VIAJE | id_viaje | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| VIAJE | folio | VARCHAR(20) | NOT NULL | UQ | Identificador público y clave de ruta en la API: `TR-` seguido de un correlativo decimal sin ceros a la izquierda que parte en 1001 (`TR-1001`, `TR-1002`…). Lo genera la base al insertar (trigger de `V2` con una secuencia): crece, nunca se reutiliza y puede tener huecos si una inserción se revierte. Inmutable. |
| VIAJE | id_solicitante | BIGINT UNSIGNED | NOT NULL | FK → SOLICITANTE |  |
| VIAJE | id_origen | BIGINT UNSIGNED | NOT NULL | FK → UBICACION |  |
| VIAJE | id_destino | BIGINT UNSIGNED | NOT NULL | FK → UBICACION |  |
| VIAJE | id_estado | BIGINT UNSIGNED | NOT NULL | FK → ESTADO_SOLICITUD |  |
| VIAJE | movil_id_movil | BIGINT UNSIGNED | NULL | FK → MOVIL | NULL hasta la asignación. |
| VIAJE | pago_id | BIGINT UNSIGNED | NULL | FK → PAGO, UQ | NULL si no hay pago. Único: un pago pertenece a un solo viaje (1:1). |
| VIAJE | cantidad_pasajeros | INT | NOT NULL | DEFAULT 0 | Mantenido por trigger desde `DETALLE_VIAJE_PASAJERO` (nunca lo escribe el cliente); no puede superar la capacidad del móvil asignado. |
| VIAJE | fh_creacion | DATETIME | NOT NULL | DEFAULT CURRENT_TIMESTAMP | Registro de la solicitud. Inmutable. |
| VIAJE | fh_agenda | DATETIME | NOT NULL | — | Fecha y hora agendada del traslado (RF02), ingresada por el Solicitante. |
| VIAJE | fh_asignacion | DATETIME | NULL | — |  |
| VIAJE | fh_inicio | DATETIME | NULL | — | Paso a En Curso. |
| VIAJE | fh_fin | DATETIME | NULL | — | Paso a Finalizado. |
| VIAJE | conductor_id_usuario | BIGINT UNSIGNED | NULL | FK → CONDUCTOR | NULL hasta la asignación (RF05). Por ser FK a `CONDUCTOR`, solo referencia usuarios con ese rol. |
| VIAJE | numero_vuelo | VARCHAR(20) | NULL | — | Opcional. |
| VIAJE | motivo | VARCHAR(200) | NULL | — | Motivo de la cancelación por el Administrador (V7) o del rechazo del Conductor (V8); el trigger lo registra en `HISTORIAL_VIAJE`. |
| DETALLE_VIAJE_PASAJERO | id_viaje | BIGINT UNSIGNED | NOT NULL | PK, FK → VIAJE |  |
| DETALLE_VIAJE_PASAJERO | id_pasajero | BIGINT UNSIGNED | NOT NULL | PK, FK → PASAJERO |  |
| HISTORIAL_VIAJE | id_historial | BIGINT UNSIGNED | NOT NULL | PK, AUTO_INCREMENT |  |
| HISTORIAL_VIAJE | id_viaje | BIGINT UNSIGNED | NOT NULL | FK → VIAJE |  |
| HISTORIAL_VIAJE | campo_modificado | VARCHAR(30) | NOT NULL | — | Campo de `VIAJE` que cambió (conductor, móvil, estado, cantidad de pasajeros, etc.). |
| HISTORIAL_VIAJE | valor | VARCHAR(200) | NOT NULL | — | Valor nuevo, como texto. |
| HISTORIAL_VIAJE | fecha_cambio | DATETIME | NOT NULL | DEFAULT CURRENT_TIMESTAMP |  |

El actor ACT-05 no tiene tabla: no hay datos propios que persistir.

### Decisiones de modelo

- **`CANCELADO`** extiende los 4 estados del Anexo (§3.2): auditoría sin borrar la solicitud ni
  reutilizar otro estado (RF15/CU-08).
- **`HISTORIAL_VIAJE`**: una fila por campo cambiado en cada `UPDATE` de `VIAJE` (conductor,
  móvil, estado, cantidad de pasajeros; p. ej. una reasignación). La escriben los triggers de
  `VIAJE`, no la aplicación; `folio` y `fh_creacion` son inmutables y nunca generan fila.
- **Roles como tabla por subtipo** (extensiones 1:1 de `USUARIO`) en vez de `TIPO_USUARIO` con
  `CHECK`; `USUARIO_ROL_ACTIVO` impone la exclusividad que el subtipo no garantiza.
- **Catálogos en vez de `CHECK`** (`ESTADO_SOLICITUD`, `ESTADO_SOLICITANTE`, `ESTADO_CONDUCTOR`,
  `ESTADO_MOVIL`, `ESTADO_PAGO`, `MEDIO_PAGO`, `TIPO_RECIBO`): agregar o renombrar un valor no altera el DDL.
- **`VIAJE.cantidad_pasajeros`**: denormalización deliberada mantenida por trigger; evita un
  `COUNT(*)` en cada consulta de "viajes del día" (RNF03).
- **3FN:** `UBICACION` separada de `VIAJE` (las coordenadas dependen de la dirección);
  `PASAJERO` + `DETALLE_VIAJE_PASAJERO` evitan el grupo repetitivo; `id_comuna` es atributo
  propio de `UBICACION`, así que `COMUNA`/`REGION` es una jerarquía geográfica normal.

### Modelo relacional

Solo relaciones; las columnas están en [Modelo de datos](#modelo-de-datos).

```mermaid
erDiagram
    REGION ||--o{ COMUNA : tiene
    COMUNA ||--o{ UBICACION : tiene
    USUARIO ||--o| SOLICITANTE : extiende
    USUARIO ||--o| ADMINISTRADOR : extiende
    USUARIO ||--o| CONDUCTOR : extiende
    USUARIO ||--o| USUARIO_ROL_ACTIVO : "tiene rol activo"
    USUARIO ||--o{ SESION : abre
    EMPRESA ||--o{ SOLICITANTE : emplea
    EMPRESA ||--o{ PAGO : paga
    ESTADO_SOLICITANTE ||--o{ SOLICITANTE : clasifica
    ESTADO_CONDUCTOR ||--o{ CONDUCTOR : clasifica
    ESTADO_MOVIL ||--o{ MOVIL : clasifica
    MOVIL ||--o{ DETALLE_MOVIL_CONDUCTOR : habilita
    CONDUCTOR ||--o{ DETALLE_MOVIL_CONDUCTOR : habilita
    ESTADO_SOLICITUD ||--o{ VIAJE : clasifica
    SOLICITANTE ||--o{ VIAJE : agenda
    UBICACION ||--o{ VIAJE : origen
    UBICACION ||--o{ VIAJE : destino
    MOVIL |o--o{ VIAJE : asignado
    CONDUCTOR |o--o{ VIAJE : asignado
    PAGO |o--o| VIAJE : asociado
    VIAJE ||--o{ DETALLE_VIAJE_PASAJERO : incluye
    PASAJERO ||--o{ DETALLE_VIAJE_PASAJERO : viaja
    VIAJE ||--o{ HISTORIAL_VIAJE : registra
    ESTADO_PAGO ||--o{ PAGO : clasifica
    BANCO ||--o{ PAGO : interviene
    MEDIO_PAGO ||--o{ PAGO : utiliza
    TIPO_RECIBO ||--o{ PAGO : emite
```

## Arquitectura

### Stack

| Capa | Decisión |
|---|---|
| Base de datos | **MariaDB** (oficial): `InnoDB` (ACID), soporte directo en Spring (`MariaDBDialect`, `mariadb-java-client`), GPL v2, bajo consumo en desarrollo local. |
| Backend | **Spring Boot 4.1**, Java 21, Gradle; API REST JSON para Android (Retrofit); `Controller` ➔ `Service` ➔ `Repository`; Spring Data JPA; Flyway. |
| Seguridad | JWT stateless (Spring Security *resource server*, HS256). Un único `SecurityFilterChain` con `denyAll` por defecto; sin JWT solo `/api/v1/auth/login`, `/refresh` y `/recuperar-password`, y `POST /api/v1/solicitantes` (autorregistro, RF01). |
| Integraciones | Push con Firebase Admin SDK (FCM) desde la capa `Service`; correo con `spring-boot-starter-mail` a un SMTP configurable. Ambos después del commit, en otro hilo y sin afectar la operación ([Notificaciones externas](#notificaciones-externas)). |
| Android | Kotlin + Jetpack Compose, MVVM con Repository ([Frontend (Android)](#frontend-android)). |

### Servicios locales

Docker Compose en `database/docker-compose.yaml`:

| Servicio | Contenedor | Puertos | Uso |
|---|---|---|---|
| `mariadb` | `belmonte-mariadb` | `3307` | Base de datos de la API. |
| `mailpit` | `belmonte-mailpit` | `1025` (SMTP), `8025` (web) | SMTP de desarrollo: recibe los correos del backend (RF21) y los muestra en <http://localhost:8025> sin entregarlos. Escucha solo en `127.0.0.1` porque no tiene contraseña. |

Desde la raíz del repositorio:

```bash
docker compose -f database/docker-compose.yaml up -d
# valores propios (puertos, credenciales): .env fuera de Git, pasado explícitamente porque
# el compose está en database/ y no encuentra solo el .env de la raíz
cp .env.example .env            # en PowerShell: Copy-Item .env.example .env
docker compose --env-file .env -f database/docker-compose.yaml up -d
docker compose -f database/docker-compose.yaml down      # conserva los datos; down -v los borra
```

El `.env` solo lo lee Docker Compose. La API recibe `DB_URL`, `DB_USER` y `DB_PASSWORD`
(valores locales en `.env.example`) del entorno o de la configuración del IDE, y Flyway migra
al iniciar. Las credenciales se cambian en cualquier entorno compartido.

### Entornos

No hay entorno publicado: el sistema se entrega como demostración en un computador anfitrión.

| Entorno | Dónde | Backend y base | Correo | Push | App |
|---|---|---|---|---|---|
| Desarrollo | Equipo de cada integrante | Compose local y `./gradlew bootRun` | Mailpit | Opcional (sin `FIREBASE_CREDENTIALS_FILE` queda desactivado) | Debug contra `10.0.2.2` (emulador) |
| Demostración | Un computador anfitrión en una red controlada (sala o hotspot) | El mismo compose y el backend con las variables de `.env.demo` | Proveedor transaccional ([Notificaciones externas](#notificaciones-externas)) | Firebase real | APK release contra la IP del anfitrión |

- **Anfitrión:** IP fija en la red de la demo; el puerto `8080` se abre solo en esa red. MariaDB
  y Mailpit siguen escuchando solo en `127.0.0.1`.
- **Datos de demostración:** antes de cada presentación se carga `database/demo.sql` con
  conductores, vehículos, habilitaciones y solicitudes ficticias (RNF09). Ese script no es una
  migración de Flyway y nunca corre en las pruebas.
- **Respaldo:** `mariadb-dump` de la base antes de cada demostración, guardado fuera del
  repositorio.
- **Sin TLS solo en la demo:** el backend responde por HTTP dentro de la red de la demostración.
  Es una limitación aceptada: el JWT viaja sin cifrar dentro de esa red. Publicar el sistema
  exige HTTPS detrás de un proxy y quitar las excepciones de HTTP de la app.
- **Secretos:** `JWT_SECRET`, `DNI_SECRET`, las credenciales SMTP y el JSON de Firebase viven
  solo en el anfitrión. `.env.demo` y el JSON quedan fuera de Git y legibles solo por su usuario;
  `.env.demo.example` documenta las variables sin valores. La demostración nunca usa los valores
  de desarrollo.

## API REST

### Convenciones

- Prefijo `/api/v1/`, JSON. El `id` interno nunca se expone: las rutas usan la columna `UNIQUE`
  de negocio.

  | Método | Ruta | Acción |
  |---|---|---|
  | `GET` | `/api/v1/{recurso}` | Listar |
  | `GET` | `/api/v1/{recurso}/{valor_unique_key}` | Consultar uno |
  | `POST` | `/api/v1/{recurso}` | Crear (`201` con el recurso) |
  | `PUT` | `/api/v1/{recurso}/{valor_unique_key}` | Solo `usuarios`, `conductores` y `solicitantes` (`200`), p. ej. el Administrador reemplazando la contraseña de un Conductor (CU-07) |

- **Sin `DELETE`:** ninguna fila se destruye por API; a lo más cambia de estado (`CANCELADO`,
  `HISTORIAL_VIAJE`).
- **`viajes` y `pagos` no se mutan por `PUT`:** conductor, móvil, estado y cantidad de pasajeros
  de `VIAJE`, y el estado de `PAGO`, cambian solo por [Acciones de negocio](#acciones-de-negocio)
  (`200`) y los validan y auditan los triggers.
- **No son recursos:** los 10 catálogos (`bancos`, `comunas`, `estados-conductor`,
  `estados-movil`, `estados-pago`, `estados-solicitante`, `estados-solicitud`, `medios-pago`,
  `regiones`, `tipos-recibo`), que cambian solo por migración y se leen juntos, sin CRUD, con
  `GET /api/v1/catalogos`; ni `ubicaciones`, que se crea y consulta solo como parte de `viajes`.
- **Tablas puente:** clave de negocio de dos columnas, se crean como unidad y no tienen `PUT`:
  `/api/v1/habilitaciones-movil-conductor/{patente}/{correo}` y
  `/api/v1/pasajeros-viaje/{folio}/{dni_hash}`.

### Recursos y roles

| Recurso | Clave de ruta | Roles | Alcance |
|---|---|---|---|
| `auth` | — | Público; `logout` y `token-fcm` con JWT de cualquier rol | Login (CU-14), renovación y cierre de sesión, token FCM (RF13) y recuperación de contraseña (CU-13/RF21). |
| `catalogos` | — | Solicitante, Administrador, Conductor (lectura) | Solo `GET /api/v1/catalogos`: bancos, medios de pago, tipos de recibo, regiones y comunas. |
| `usuarios` | `correo` | Solicitante, Administrador, Conductor | Cada uno solo su cuenta (RNF08). `POST` (cuenta sin rol): solo Administrador. El Conductor solo consulta; su cuenta y contraseña las edita el Administrador (`PUT /conductores`). |
| `administradores` | `correo` | Administrador | Consulta y alta (`POST`, CU-18/RF27); el inicial se crea por migración. |
| `conductores` | `correo` | Administrador (crea, edita y activa o desactiva; CU-07/RF25); Conductor | El Conductor solo su cuenta. |
| `solicitantes` | `correo` | Público (`POST`, autorregistro RF01); Solicitante (su cuenta: lectura y `PUT`); Administrador (lectura, aprobar y rechazar; CU-17/RF26) | El Solicitante solo su cuenta. |
| `empresas` | `rut` | Administrador (lectura) | Se crean o reutilizan dentro de `POST /solicitantes` (autorregistro público, RF01), no por esta ruta. |
| `moviles` | `patente` | Administrador | Gestión de flota y bajas (CU-09, CU-10, RF25). |
| `habilitaciones-movil-conductor` | `{patente}/{correo}` | Administrador | Habilitar conductores para un vehículo (CU-10). |
| `pagos` | `hash` | Administrador (gestión, CU-11/CU-12); Solicitante (lectura) | El Solicitante solo los pagos de sus viajes (RF20). |
| `pasajeros` | `dni_hash` | Solicitante (los registra, CU-01); Administrador (lectura) | Solicitante y Conductor ven los pasajeros de sus viajes dentro de `viajes` (`ViajeDTOResponse`). |
| `pasajeros-viaje` | `{folio}/{dni_hash}` | Solicitante (asocia pasajeros a su viaje, CU-01); Administrador (lectura) | El Solicitante solo en viajes propios; la lectura de pasajeros va en `viajes`. |
| `viajes` | `folio` | Solicitante (propios, RF02/RF03); Administrador (todos, CU-04); Conductor (asignados, RF07/RF08) | Ver [Consultas y alcance por rol](#consultas-y-alcance-por-rol). |

Sin token, o con uno alterado o vencido: 401. Rol o método que esta matriz no autoriza en la ruta, o
ruta fuera de ella: 403; método inexistente en una ruta que el rol sí puede usar: 405 (ver [Errores](#errores)).

### Consultas y alcance por rol

- `GET /usuarios` devuelve solo la cuenta propia. En `usuarios`, `conductores` (Conductor) y
  `solicitantes` (Solicitante), pedir la cuenta de otra persona responde 403 exista o no, para
  no revelar qué correos están registrados.
- `GET /viajes`: Administrador, todos (CU-04); Solicitante, los propios (RF03); Conductor, los
  asignados a él (RF07). Parámetros combinables, para todos los roles dentro de su alcance:
  - `estado`: uno de los 5 de `ESTADO_SOLICITUD`; `?estado=PENDIENTE` es la lista de pendientes
    del Administrador (RF04).
  - `fecha` (`AAAA-MM-DD`): viajes con `fhAgenda` ese día.
  - Sin parámetros, el Conductor recibe su agenda de hoy y los demás roles, todo su alcance.
  - La agenda del Conductor (sin `estado`) trae sus viajes `ASIGNADO`, `EN_CURSO` y `FINALIZADO`
    del día; uno `CANCELADO` sale de ella ([V5](#viaje), [V6](#viaje)).
- `GET /solicitantes` (Administrador): todos o, con `?estado=PENDIENTE`, las cuentas por aprobar
  (CU-17).
- `GET /habilitaciones-movil-conductor`: todas, o filtradas por `?patente=` (conductores de un
  vehículo) y/o `?correo=` (vehículos de un conductor).
- Un parámetro mal formado o con un valor fuera de su lista responde 400; uno desconocido se
  ignora.
- `ViajeDTOResponse` incluye `pasajeros` (`dniHash`, `nombre`): cada rol ve los pasajeros de los
  viajes que ya puede consultar (RF03, RF08, HU07), sin leer `pasajeros` ni `pasajeros-viaje`.
- `GET /pagos` del Solicitante: solo los de sus viajes (RF20).
- `POST /pagos` (CU-11/RF18): 409 si el viaje está cancelado o ya tiene pago; 400 si el banco
  (`codSbif`), el medio de pago o el tipo de recibo no existen en su catálogo.
- `POST /pasajeros` recibe el documento en `dni`, con su tipo en `tipoDocumento`, y el backend calcula el `dni_hash` (ver
  [Configuración](#configuración)); si el documento ya existe, devuelve su `dniHash` sin duplicarlo ni exponer el `nombre` guardado (RNF08).
- **Sin paginación:** cada lista responde completa según sus filtros, porque la caché Room
  reemplaza la consulta guardada con la respuesta completa ([Base de datos local](#base-de-datos-local)).
- **Orden fijo:**
  - `viajes`, por `fhAgenda` ascendente (lo próximo primero) en la agenda del Conductor y con
    `estado` `PENDIENTE`, `ASIGNADO` o `EN_CURSO`; descendente (lo reciente primero) sin `estado`
    o con `FINALIZADO` o `CANCELADO`. El empate se desata por `folio`.
  - `usuarios`, `conductores`, `solicitantes` y `pasajeros`, por `nombre`; `empresas`, por
    `razonSocial`; `moviles`, por `patente`; `habilitaciones-movil-conductor`, por `patente` y
    `nombreConductor`; `pagos`, por `folio` descendente.
- **Asociaciones repetidas:** "uno o más" conductores por vehículo (RF17/CU-10) se registra con un
  `POST /habilitaciones-movil-conductor` por par. Un par nuevo responde `201`; uno existente,
  `200` con el mismo cuerpo, sin duplicar. Igual con `POST /pasajeros-viaje`.
- **`GET /api/v1/catalogos`:** para cualquier rol autenticado; responde `CatalogosDTOResponse`
  ([Contratos JSON](#contratos-json)) con bancos, medios de pago, tipos de recibo, regiones y
  comunas. La app lo pide tras el login y lo mantiene en memoria durante la sesión, para los
  selects de "Registrar pago" y el selector de comuna de
  [Ubicaciones desde Google Places](#ubicaciones-desde-google-places).

Consultas por pantalla ([Navegación y pantallas](#navegación-y-pantallas)); al abrir una
notificación, `GET /viajes/{folio}`; al cerrar sesión desde un Perfil, `POST /auth/logout`:

| Pantalla | Llamadas |
|---|---|
| Inicio de aplicación | Ninguna: lee la sesión guardada. |
| Login | `POST /auth/login`; luego `GET /catalogos` o, con `cambioPasswordRequerido`, "Cambiar contraseña". |
| Registro de solicitante | `POST /solicitantes`. |
| Recuperar contraseña | `POST /auth/recuperar-password`. |
| Mis solicitudes | `GET /viajes`. |
| Nueva solicitud | Places ([Ubicaciones desde Google Places](#ubicaciones-desde-google-places)), luego el [Flujo de una solicitud](#flujo-de-una-solicitud-cu-01--rf02). |
| Detalle de solicitud (Solicitante) | `GET /viajes/{folio}` y, si trae `hashPago`, `GET /pagos/{hash}`; cancelar, `POST /viajes/{folio}/cancelar`. |
| Perfil (Solicitante) | `GET /solicitantes/{correo}`. |
| Cambiar contraseña | `PUT /usuarios/{correo}` con los datos actuales, `passwordActual` y la nueva `password` (Solicitante y Administrador). |
| Solicitudes (Administrador) | `GET /viajes?estado=PENDIENTE` al abrir; cada filtro, `GET /viajes?estado=…`; "Todas", `GET /viajes`. |
| Detalle de solicitud (Administrador) | `GET /viajes/{folio}` y, si trae `hashPago`, `GET /pagos/{hash}`; cancelar, `POST /viajes/{folio}/cancelar` con `motivo` (y `montoReembolso` si está vencido). |
| Cuentas por aprobar | `GET /solicitantes?estado=PENDIENTE`; `POST /solicitantes/{correo}/aprobar` o `/rechazar`. |
| Asignar conductor y vehículo | `GET /conductores` (se elige uno `DISPONIBLE`, con `viajesAsignados` y `folioEnCurso` como contexto); `GET /habilitaciones-movil-conductor?correo=` del elegido (vehículos con `capacidad` y `estadoMovil`); confirmar, `POST /viajes/{folio}/asignar`. |
| Conductores | `GET /conductores`. |
| Registrar conductor | `POST /conductores`. |
| Detalle de conductor | `GET /conductores/{correo}` y `GET /habilitaciones-movil-conductor?correo=`; editar o reemplazar la contraseña, `PUT /conductores/{correo}`; desactivar o reactivar, `POST /conductores/{correo}/activo`. |
| Vehículos | `GET /moviles`. |
| Registrar vehículo | `POST /moviles`. |
| Detalle de vehículo | `GET /moviles/{patente}` y `GET /habilitaciones-movil-conductor?patente=`; desactivar o reactivar, `POST /moviles/{patente}/activo`. |
| Gestionar habilitaciones | Desde el vehículo, `GET /conductores` y `GET /habilitaciones-movil-conductor?patente=`; desde el conductor, `GET /moviles` y `?correo=`. Un `POST /habilitaciones-movil-conductor` por cada par nuevo. |
| Registrar pago | `GET /catalogos` (ya en memoria); `POST /pagos`. |
| Detalle de pago | `GET /pagos/{hash}`; cambiar estado, `POST /pagos/{hash}/estado`. |
| Perfil (Administrador) | `GET /administradores/{correo}`. |
| Registrar administrador | `POST /administradores`. |
| Mis viajes | `GET /viajes` (hoy) o `GET /viajes?fecha=AAAA-MM-DD`. |
| Detalle de viaje | `GET /viajes/{folio}`; iniciar y finalizar, `POST /viajes/{folio}/estado` (o la cola sin conexión, RF11); rechazar, `POST /viajes/{folio}/rechazar` (solo con conexión). |
| Perfil (Conductor) | `GET /conductores/{correo}`. |

### Flujo de una solicitud (CU-01 / RF02)

Tres llamadas del Solicitante autenticado, porque viaje, pasajero y asociación son recursos distintos:

| Paso | Llamada | Cuerpo | Resultado |
|---|---|---|---|
| 1 | `POST /api/v1/pasajeros` | `tipoDocumento`, `dni`, `nombre` | Pasajero con su `dniHash` (si el documento ya existe, solo su `dniHash`). Uno por pasajero. |
| 2 | `POST /api/v1/viajes` | `fhAgenda` (de 2 horas a 90 días desde ahora), `origen` y `destino` (`direccion`, `comuna`, `region`, `latitud`, `longitud`), `numeroVuelo` opcional | Viaje `PENDIENTE` a nombre del Solicitante del JWT; devuelve su `folio`. |
| 3 | `POST /api/v1/pasajeros-viaje` | `folio`, `dniHash` | Asocia el pasajero; repetir una asociación existente no la duplica. |

Los pasos 1 y 2 son independientes; el 3 necesita el `dniHash` del 1 y el `folio` del 2. Solo el
Solicitante dueño agrega pasajeros, y solo con el viaje `PENDIENTE` o `ASIGNADO` (409 si no). Con
vehículo asignado no se admiten más pasajeros que su capacidad (409). Cada asociación suma 1 a
`VIAJE.cantidad_pasajeros` por trigger.

### Ubicaciones desde Google Places

`origen` y `destino` salen siempre de una sugerencia de Google Places, nunca de texto libre (HU02).

**En Android (Places SDK for Android):**

- Autocompletado restringido a Chile (`CL`) y en español, con un token de sesión por búsqueda
  para que las sugerencias y el detalle se cobren como una sola sesión (RNF07).
- Al elegir una sugerencia se piden solo el nombre, la dirección formateada, los componentes de
  dirección y las coordenadas del lugar.

| Campo de `origen`/`destino` | Origen en Places |
|---|---|
| `direccion` | Nombre del lugar si es un establecimiento (p. ej. un aeropuerto u hotel); si no, calle y número de la dirección formateada. Se corta a 200 caracteres. |
| `comuna` | Componente `administrative_area_level_3`; si falta, `locality`. |
| `region` | Componente `administrative_area_level_1`. |
| `latitud`, `longitud` | Coordenadas del lugar, redondeadas a 6 decimales. |

**En el backend (cotejo con el catálogo):**

1. Normaliza `comuna` y `region`: minúsculas, sin tildes ni diéresis, `ñ` como `n`, espacios
   simples y sin los prefijos "región de la", "región del", "región de" ni "región".
2. Busca la comuna por nombre normalizado. El catálogo no repite nombres de comuna, así que la
   región solo desempataría si se repitieran: una región escrita distinto (p. ej. "Región del
   Libertador Gral. Bernardo O'Higgins") no rechaza una comuna que calza.
3. Sin coincidencia responde 400 con `origen.comuna: no existe en el catálogo` (o
   `destino.comuna`).
4. Guarda y devuelve los nombres oficiales del catálogo, no los de Places.

**Respaldo en la app:** si Places no trae comuna o el backend responde ese 400, la app muestra un
selector de región y comuna con los datos de `GET /catalogos`, conserva la dirección y las
coordenadas de Places y reenvía. El selector solo corrige la comuna; no permite escribir la
dirección.

**Catálogo geográfico:** `V3__datos_iniciales.sql` carga las 16 regiones y las 346 comunas de
Chile con su nombre oficial, sin el prefijo "Región" (p. ej. "Metropolitana de Santiago",
"Pudahuel"). Cambia solo por migración.

### Acciones de negocio

Métodos adicionales de `ViajeController`, `PagoController`, `ConductorController`,
`MovilController` y `SolicitanteController`, fuera de `CrudController`; reciben el DTO de su
fila ([Contratos JSON](#contratos-json)) y devuelven la respuesta del recurso. Reglas y efectos en
[Máquinas de estado](#máquinas-de-estado).

| Método | Ruta | Rol | Cuerpo | Acción |
|---|---|---|---|---|
| `POST` | `/api/v1/viajes/{folio}/asignar` | Administrador | `correoConductor`, `patente` | [V2](#viaje): asignar o reasignar (CU-04/RF05/RF22); viaje `PENDIENTE` o `ASIGNADO`. |
| `POST` | `/api/v1/viajes/{folio}/estado` | Conductor asignado | `estado`: `EN_CURSO` o `FINALIZADO` | [V3](#viaje) `ASIGNADO` → `EN_CURSO` o [V4](#viaje) `EN_CURSO` → `FINALIZADO` (CU-05/RF09). |
| `POST` | `/api/v1/viajes/{folio}/cancelar` | Solicitante dueño | — | [V5](#viaje) `PENDIENTE`/`ASIGNADO` → `CANCELADO` (CU-08/RF15), con [P4](#pago) al 50 % si el pago está `PAGADO`. |
| `POST` | `/api/v1/viajes/{folio}/cancelar` | Administrador | `motivo`; `montoReembolso` si el viaje está vencido y el pago `PAGADO` | [V7](#viaje) (CU-15/RF23), con [P4](#pago) al 100 % o en el monto indicado. |
| `POST` | `/api/v1/viajes/{folio}/rechazar` | Conductor asignado | `motivo` | [V8](#viaje) `ASIGNADO` → `PENDIENTE` (CU-16/RF24). |
| `POST` | `/api/v1/pagos/{hash}/estado` | Administrador | `estado`: `PAGADO`, `RECHAZADO` o `ANULADO` | [P2](#pago) o [P3](#pago) (CU-12/RF19); `ANULADO` dispara [V6](#viaje). `REEMBOLSADO` (P4) no pasa por esta ruta. |
| `POST` | `/api/v1/conductores/{correo}/activo` | Administrador | `activo` | Desactivar o reactivar un Conductor (RF25); 409 con viajes `ASIGNADO` o `EN_CURSO`. |
| `POST` | `/api/v1/moviles/{patente}/activo` | Administrador | `activo` | Desactivar o reactivar un vehículo (RF25); misma regla. |
| `POST` | `/api/v1/solicitantes/{correo}/aprobar` | Administrador | — | `PENDIENTE` → `APROBADO` (CU-17/RF26); correo al Solicitante. 409 si ya fue revisado. |
| `POST` | `/api/v1/solicitantes/{correo}/rechazar` | Administrador | — | `PENDIENTE` → `RECHAZADO` (CU-17/RF26); correo al Solicitante. 409 si ya fue revisado. |

### Autenticación

| Método | Ruta | JWT | Acción |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | No | Autenticar con correo y contraseña y abrir una sesión (CU-14). |
| `POST` | `/api/v1/auth/refresh` | No | Renovar el acceso con el refresh token, que rota. |
| `POST` | `/api/v1/auth/logout` | Sí | Cerrar la sesión del JWT y borrar el token FCM. |
| `POST` | `/api/v1/auth/token-fcm` | Sí | Reemplazar el token FCM del usuario (RF13). |
| `POST` | `/api/v1/auth/recuperar-password` | No | Pedir una contraseña temporal por correo (CU-13/RF21). |

Las atiende `AuthController` con `LoginDTORequest`/`LoginDTOResponse`, `RefreshDTORequest`,
`TokenFcmDTORequest` y `RecuperarPasswordDTORequest` ([Contratos JSON](#contratos-json)).

- **Login:** recibe `correo`, `password` y, opcional, `tokenFcm`. La contraseña puede ser la
  normal (`password_hash`) o la temporal vigente (`password_temporal_hash`, RF21):
  - Con la normal: responde `accessToken` y `refreshToken`, y anula una temporal pendiente.
  - Con la temporal: responde solo `accessToken` (`refreshToken` en `null`) y
    `cambioPasswordRequerido: true`; la app abre "Cambiar contraseña" antes que cualquier otra
    pantalla.

  Todo fallo (correo desconocido, contraseña incorrecta, temporal vencida o usuario sin rol) da
  el mismo 401 "Credenciales inválidas".
- **JWT de acceso:** HS256 con `JWT_SECRET`; `sub` (correo), `rol`, `exp`
  (`JWT_EXPIRATION_SECONDS`, 3600 s por defecto) y `sid` (la sesión; ausente si se entró con la
  temporal). Va en cada petición como `Authorization: Bearer <token>`. Es stateless: no se
  revoca y vale hasta su `exp`.
- **Sesiones y refresh token:** cada login con la contraseña normal crea una fila en `SESION`.
  El refresh token es un valor aleatorio de 256 bits en Base64url (43 caracteres); se entrega una
  sola vez y solo se guarda su SHA-256.
  - `POST /auth/refresh` con un refresh vigente revoca su sesión y abre otra (rotación): nuevo
    `accessToken` y nuevo `refreshToken`, que vence a los `JWT_REFRESH_DIAS` (30) de emitido.
    Responde como el login.
  - Refresh desconocido, vencido o revocado: 401 "Sesión inválida". Presentar uno ya revocado se
    trata como robo y revoca todas las sesiones del usuario.
  - `POST /auth/logout` revoca la sesión `sid`, deja `USUARIO.token_fcm` en NULL y responde `204`.
  - Cambiar la contraseña propia revoca las demás sesiones del usuario (la `sid` actual sigue);
    el Administrador que reemplaza la de un Conductor revoca todas las del Conductor (CU-07).
- **Contraseñas:** BCrypt.
- **Recuperar contraseña:** solo Solicitante y Administrador (el Conductor recurre al
  Administrador, CU-07/RF14). Siempre `204`, exista o no el correo. Genera una temporal de 12
  caracteres, la guarda hasheada en `password_temporal_hash` con vencimiento a los
  `PASSWORD_TEMPORAL_MINUTOS` (30) y la envía por correo; nunca va a los logs. **No reemplaza la
  contraseña actual:** ambas sirven hasta que se use una, y un nuevo pedido reemplaza la temporal
  anterior. Cambiar la contraseña con `PUT /api/v1/usuarios/{correo}` (con `passwordActual`, que
  puede ser la temporal) deja la temporal en NULL.
- **Token FCM (RF13):** `USUARIO.token_fcm` guarda el del último dispositivo del usuario. Se fija
  en el login (`tokenFcm`) o con `POST /auth/token-fcm` si cambia durante la sesión, y se borra
  con `logout`. Es único: al guardarlo se quita de cualquier otro usuario, para que una cuenta
  anterior en el mismo teléfono no reciba sus push.
- **Administrador inicial:** la migración `V3` crea `admin@belmontespa.cl` con una contraseña
  temporal de desarrollo (en el comentario de la migración) en `password_temporal_hash`, que vence
  a los 30 días, y un `password_hash` inutilizable: al entrar debe cambiarla. Se reemplaza en
  cualquier entorno compartido.

**Límites de intentos.** Contadores en memoria de la instancia (se pierden al reiniciar), por
correo normalizado y por IP del cliente:

| Ruta | Límite | Al excederlo |
|---|---|---|
| `POST /auth/login` | 5 fallos en 15 minutos por correo, exista o no; un login exitoso reinicia el contador. | El correo queda bloqueado 15 minutos: 429 con `Retry-After`, aun con la contraseña correcta. |
| `POST /auth/recuperar-password` | 1 pedido cada 5 minutos y 5 por hora por correo. | `204` sin enviar correo, igual que con un correo inexistente. |
| `POST /auth/login`, `/auth/refresh`, `/auth/recuperar-password` y `POST /solicitantes` | 20 peticiones por minuto por IP, sumadas. | 429 con `Retry-After`. |

### Contratos JSON

Cuerpos de petición y respuesta de cada DTO. Convenciones comunes:

- Nombres en camelCase. Los valores de estado, rol y tipo son códigos en mayúsculas
  (`PENDIENTE`, `EN_USO`, `ADMINISTRADOR`, `RUT`); los de `medioPago` y `tipoRecibo` son el
  `nombre` de su catálogo.
- Fechas de negocio como fecha y hora local sin zona, ISO 8601 (`2026-09-24T14:30:00`),
  interpretadas en `America/Santiago`; el parámetro `fecha` es `AAAA-MM-DD`. El único instante
  con zona es `ApiError.timestamp` (UTC, sufijo `Z`).
- Coordenadas como número decimal; montos como entero en pesos chilenos (CLP).
- Un opcional ausente equivale a `null`, y un texto vacío o solo con espacios equivale a ausente.
  Los campos desconocidos se ignoran.
- Las respuestas incluyen los opcionales con `null` y nunca llevan `id` interno,
  `password_hash` ni el documento del pasajero en claro.
- Las reglas de cada campo están en [Validaciones](#validaciones).

**Peticiones** (`?` = opcional):

| DTO | Rutas | Campos |
|---|---|---|
| `LoginDTORequest` | `POST /auth/login` | `correo`, `password`, `tokenFcm?` |
| `RefreshDTORequest` | `POST /auth/refresh` | `refreshToken` |
| `TokenFcmDTORequest` | `POST /auth/token-fcm` | `tokenFcm` |
| `RecuperarPasswordDTORequest` | `POST /auth/recuperar-password` | `correo` |
| `SolicitanteDTORequest` | `POST /solicitantes`, `PUT /solicitantes/{correo}` | `nombre`, `correo`, `telefono?`, `password`, `passwordActual?`, `razonSocial`, `rutEmpresa` |
| `ConductorDTORequest` | `POST /conductores`, `PUT /conductores/{correo}` | `nombre`, `correo`, `telefono?`, `password` |
| `AdministradorDTORequest` | `POST /administradores` | `nombre`, `correo`, `telefono?`, `password` (inicial y temporal, CU-18) |
| `UsuarioDTORequest` | `POST /usuarios`, `PUT /usuarios/{correo}` | `nombre`, `correo`, `telefono?`, `password`, `passwordActual?` |
| `MovilDTORequest` | `POST /moviles` | `patente`, `capacidad`, `gpsSerial?` |
| `DetalleMovilConductorDTORequest` | `POST /habilitaciones-movil-conductor` | `patente`, `correoConductor` |
| `PasajeroDTORequest` | `POST /pasajeros` | `tipoDocumento` (`RUT` o `PASAPORTE`), `dni`, `nombre` |
| `DetalleViajePasajeroDTORequest` | `POST /pasajeros-viaje` | `folio`, `dniHash` |
| `ViajeDTORequest` | `POST /viajes` | `fhAgenda`, `origen`, `destino`, `numeroVuelo?`; `origen` y `destino` son objetos `{direccion, comuna, region, latitud, longitud}` |
| `PagoDTORequest` | `POST /pagos` | `folio`, `monto`, `codSbif`, `medioPago`, `tipoRecibo` |
| `AsignarViajeDTORequest` | `POST /viajes/{folio}/asignar` | `correoConductor`, `patente` |
| `CambiarEstadoViajeDTORequest` | `POST /viajes/{folio}/estado` | `estado` (`EN_CURSO` o `FINALIZADO`) |
| `CambiarEstadoPagoDTORequest` | `POST /pagos/{hash}/estado` | `estado` (`PAGADO`, `RECHAZADO` o `ANULADO`) |
| `CancelarViajeDTORequest` | `POST /viajes/{folio}/cancelar` (Administrador; el Solicitante no envía cuerpo) | `motivo`, `montoReembolso?` |
| `RechazarViajeDTORequest` | `POST /viajes/{folio}/rechazar` | `motivo` |
| `ActivoDTORequest` | `POST /conductores/{correo}/activo`, `POST /moviles/{patente}/activo` | `activo` (booleano) |

- **`PUT`** usa el DTO del `POST` y reemplaza todos los campos editables: `nombre`, `telefono`
  (ausente o `null` lo borra) y, si viene, `password`. `correo` debe coincidir con el de la ruta:
  no se cambia (400 si difiere). Sin `password` se conserva la actual; con ella se reemplaza y se
  anula la expiración de una temporal (RF21).
- **Contraseña propia:** en `PUT /usuarios` y `PUT /solicitantes` con `password`, el dueño de la
  cuenta envía `passwordActual` (obligatoria; una temporal vigente cuenta como actual). Si falta
  o no coincide: 400 `passwordActual: no coincide`, sin más detalle. En `PUT /conductores` el
  Administrador reemplaza la contraseña del Conductor sin `passwordActual` (CU-07). En `PUT /solicitantes`, `razonSocial` y `rutEmpresa` son opcionales y, si
  vienen, deben ser los de su empresa actual (400 si difieren): la empresa no cambia por API.
- **`PagoDTORequest`**: la empresa que paga (`PAGO.empresa_id_empresa`) es la del Solicitante
  del viaje; no viene en el cuerpo. El banco se identifica por `codSbif`.
- **`PasajeroDTORequest`**: `dni` es el único campo que lleva el documento en claro; el backend
  lo normaliza, calcula el `dni_hash` y lo descarta. `tipoDocumento` solo decide la validación
  y no se guarda.

**Respuestas** (`?` = puede ser `null`):

| DTO | Campos |
|---|---|
| `LoginDTOResponse` (login y `POST /auth/refresh`) | `accessToken`, `tokenType` (`Bearer`), `expiresIn` (segundos), `refreshToken?` (`null` si se entró con la temporal), `refreshExpiresIn?` (segundos), `cambioPasswordRequerido` (booleano), `correo`, `rol` (`SOLICITANTE`, `ADMINISTRADOR` o `CONDUCTOR`) |
| `UsuarioDTOResponse`, `AdministradorDTOResponse` | `nombre`, `correo`, `telefono?`, `fechaRegistro` |
| `ConductorDTOResponse` | Los de `UsuarioDTOResponse`, `activo`, `estadoConductor` (`DISPONIBLE` u `OCUPADO`), `viajesAsignados` (cantidad de sus viajes `ASIGNADO`) y `folioEnCurso?` (su viaje `EN_CURSO`, si tiene) |
| `SolicitanteDTOResponse` | Los de `UsuarioDTOResponse`, `razonSocial`, `rutEmpresa` y `estadoSolicitante` (`PENDIENTE`, `APROBADO` o `RECHAZADO`) |
| `EmpresaDTOResponse` | `razonSocial`, `rut` |
| `MovilDTOResponse` | `patente`, `capacidad`, `gpsSerial?`, `activo`, `estadoMovil` (`DISPONIBLE` o `EN_USO`) |
| `DetalleMovilConductorDTOResponse` | `patente`, `capacidad`, `estadoMovil`, `correoConductor`, `nombreConductor`, `estadoConductor` |
| `PasajeroDTOResponse` | `dniHash`, `nombre?` (`null` solo en `POST /pasajeros` cuando el documento ya existía, RNF08) |
| `DetalleViajePasajeroDTOResponse` | `folio`, `dniHash`, `nombrePasajero` |
| `ViajeDTOResponse` | `folio`, `estado`, `correoSolicitante`, `nombreSolicitante`, `telefonoSolicitante?`, `razonSocial` (empresa del Solicitante; RNF08), `origen` y `destino` (mismo objeto que en la petición), `fhCreacion`, `fhAgenda`, `fhAsignacion?`, `fhInicio?`, `fhFin?`, `numeroVuelo?`, `cantidadPasajeros`, `correoConductor?`, `nombreConductor?`, `patenteMovil?`, `hashPago?`, `estadoPago?`, `motivo?` (de V7 o V8), `pasajeros` (lista de `PasajeroDTOResponse`, con `nombre`) |
| `PagoDTOResponse` | `hash`, `folio`, `monto`, `estadoPago` (`PENDIENTE`, `PAGADO`, `RECHAZADO`, `ANULADO` o `REEMBOLSADO`), `banco`, `codSbif`, `rutEmpresa`, `medioPago`, `tipoRecibo`, `fechaPago?`, `montoReembolsado?` |
| `CatalogosDTOResponse` | `bancos` (lista de `{nombre, codSbif}`), `mediosPago` y `tiposRecibo` (listas de nombres), `regiones` (lista de `{nombre, comunas}`, con `comunas` como lista de nombres); todo por `nombre` |
| `ApiError` | `timestamp`, `status`, `error`, `message`, `path` ([Errores](#errores)) |

Ejemplo, paso 2 del [Flujo de una solicitud](#flujo-de-una-solicitud-cu-01--rf02).
`POST /api/v1/viajes`:

```json
{"fhAgenda": "2026-09-24T14:30:00",
 "origen": {"direccion": "Aeropuerto Arturo Merino Benítez", "comuna": "Pudahuel",
            "region": "Metropolitana de Santiago", "latitud": -33.392975, "longitud": -70.785803},
 "destino": {"direccion": "Av. Apoquindo 5400", "comuna": "Las Condes",
             "region": "Metropolitana de Santiago", "latitud": -33.411520, "longitud": -70.575384},
 "numeroVuelo": "LA1502"}
```

Respuesta `201` (`origen` y `destino` iguales a los enviados, omitidos aquí):

```json
{"folio": "TR-1042", "estado": "PENDIENTE", "correoSolicitante": "crios@andesconsult.cl",
 "nombreSolicitante": "Constanza Ríos", "telefonoSolicitante": "+56955550101",
 "razonSocial": "Andes Consultores SpA",
 "fhCreacion": "2026-09-20T10:05:12", "fhAgenda": "2026-09-24T14:30:00",
 "fhAsignacion": null, "fhInicio": null, "fhFin": null, "numeroVuelo": "LA1502",
 "cantidadPasajeros": 0, "correoConductor": null, "nombreConductor": null,
 "patenteMovil": null, "hashPago": null, "estadoPago": null, "pasajeros": []}
```

### Validaciones

Reglas de cada campo, comunes a todos los DTO que lo usan. Se aplican tras normalizar; el
backend guarda y devuelve el valor normalizado. Un fallo responde 400 con `message` en forma
`campo: motivo` (anidados como `origen.latitud`), con todos los campos fallidos juntos.

| Campo | Regla | Normalización |
|---|---|---|
| `correo`, `correoConductor` | Obligatorio; formato `local@dominio.tld`; hasta 150 caracteres. | Sin espacios en los extremos y en minúsculas; el login compara así, sin distinguir mayúsculas. |
| `nombre` (usuario y pasajero) | De 2 a 120 caracteres. | Sin espacios en los extremos. |
| `razonSocial` | De 2 a 50 caracteres. | Sin espacios en los extremos. |
| `telefono` | Opcional; 9 dígitos chilenos, con o sin el prefijo `+56`. | Sin espacios ni guiones y con prefijo: `+56955550101`. |
| `passwordActual` | Obligatoria solo al cambiar la contraseña propia (ver `PUT` en [Contratos JSON](#contratos-json)); se compara con el hash guardado. | Ninguna: no se recortan espacios. |
| `password` | De 8 a 72 caracteres (hasta 72 bytes en UTF-8, límite de BCrypt), con al menos una letra y un dígito. Rige en el autorregistro, el alta de Conductor y todo `PUT` que la cambie. La temporal de RF21 (12 caracteres) se genera cumpliéndola. | Ninguna: no se recortan espacios. |
| `rutEmpresa`, `dni` con `tipoDocumento` `RUT` | Cuerpo de 7 u 8 dígitos y dígito verificador válido (módulo 11: dígitos de derecha a izquierda por 2, 3, 4, 5, 6, 7, 2…; `11 − suma mod 11`, donde 11 → `0` y 10 → `K`). Se aceptan puntos y guion. | `EMPRESA.rut`: sin puntos, con guion y `K` mayúscula (`76543210-K`). `dni`: la de `PASAJERO.dni_hash`. |
| `dni` con `tipoDocumento` `PASAPORTE` | De 5 a 20 letras o dígitos. | La de `PASAJERO.dni_hash`. |
| `tipoDocumento` | `RUT` o `PASAPORTE`. | — |
| `patente` | Formato antiguo `AA1234` o actual `BBBB12`: `^([A-Z]{2}[0-9]{4}\|[A-Z]{4}[0-9]{2})$`. | En mayúsculas, sin espacios ni guion. |
| `capacidad` | Entero de 1 a 20. | — |
| `gpsSerial` | Opcional; entero mayor que 0. | — |
| `fhAgenda` | Entre ahora + 2 horas y ahora + 90 días, en hora de Chile al recibir la petición. | — |
| `direccion` | De 1 a 200 caracteres. | Sin espacios en los extremos. |
| `comuna`, `region` | Se cotejan con el catálogo según [Ubicaciones desde Google Places](#ubicaciones-desde-google-places); sin coincidencia, 400. | La del cotejo; se guardan y devuelven los nombres oficiales. |
| `latitud`, `longitud` | Entre −90 y 90, y entre −180 y 180; hasta 6 decimales. `origen` y `destino` no pueden tener las mismas coordenadas. | — |
| `numeroVuelo` | Opcional; código de aerolínea de 2 caracteres, 1 a 4 dígitos y una letra opcional: `^[A-Z0-9]{2}[0-9]{1,4}[A-Z]?$`. | En mayúsculas y sin espacios: `la 1502` → `LA1502`. |
| `monto` | Entero mayor que 0 (CLP). | — |
| `codSbif`, `medioPago`, `tipoRecibo` | Deben existir en su catálogo. | `medioPago` y `tipoRecibo` en mayúsculas. |
| `estado` | Uno de los valores de su DTO. | — |
| `motivo` | De 5 a 200 caracteres. | Sin espacios en los extremos. |
| `montoReembolso` | Entero de 0 al `monto` del pago. Obligatorio solo cuando el Administrador cancela un viaje vencido con pago `PAGADO`; en otro caso se ignora. | — |
| `activo` | Booleano obligatorio. | — |
| `refreshToken` | 43 caracteres Base64url. | — |
| `tokenFcm` | Opcional en el login, obligatorio en `/auth/token-fcm`; hasta 255 caracteres. | — |
| `folio`, `hash`, `dniHash` | `folio`: `^TR-[1-9][0-9]{3,16}$`; `hash`: `^[0-9a-f]{32}$`; `dniHash`: 43 caracteres Base64url. En el cuerpo, un valor mal formado es 400; en la ruta, 404, igual que uno inexistente. | — |

Orden de evaluación:

1. Cuerpo ilegible: 400 "Cuerpo inválido".
2. Reglas de esta tabla: 400.
3. Recurso de la ruta o del cuerpo inexistente (`folio`, `hash`, `patente`, `correo`, `dniHash`): 404.
4. Reglas de negocio y [Máquinas de estado](#máquinas-de-estado): 409. Un `estado` válido
   pero no permitido desde el estado actual es 409, no 400.

Android aplica las mismas reglas antes de enviar (HU02), sin reemplazar las del backend.

### Errores

Los errores de la aplicación responden `ApiError`:

```json
{"timestamp": "2026-10-16T18:30:00Z", "status": 409, "error": "Conflict",
 "message": "El correo ya está registrado", "path": "/api/v1/conductores"}
```

| Código | Cuándo | Cuerpo |
|---|---|---|
| `400` | Validación del cuerpo ([Validaciones](#validaciones); `message` lista `campo: motivo`), JSON ilegible ("Cuerpo inválido"), parámetro mal formado (p. ej. `fecha`) o dato inexistente en su catálogo o geografía. | `ApiError` |
| `401` | Sin token, o token alterado o vencido (cabecera `WWW-Authenticate: Bearer`); también el login fallido y un refresh inválido. | Vacío; el login fallido lleva `ApiError` |
| `403` | La matriz no autoriza ese método en esa ruta para el rol, o la ruta no figura en ella (cabecera `WWW-Authenticate`); también un recurso o cuenta ajena, o el login de un Solicitante no aprobado. | Vacío en los dos primeros casos; `ApiError` en el recurso ajeno y en el login |
| `404` | El recurso no existe. | `ApiError` |
| `405` | La matriz autoriza al rol en esa ruta pero el método no existe (p. ej. `PUT /moviles/{patente}` del Administrador, que tiene `moviles` completo). | `ApiError` |
| `409` | Regla de negocio o restricción: correo o patente repetidos, transición inválida, capacidad superada, o rechazo de un trigger ("La operación viola una restricción de datos"). | `ApiError` |
| `429` | Límite de intentos excedido ([Autenticación](#autenticación)); cabecera `Retry-After` en segundos. | `ApiError` |
| `500` | Error inesperado; mensaje genérico ("Error interno"), detalle solo en el log. | `ApiError` |

## Estructura del backend

### Paquetes y capas

Monolito por capas: cada capa existe una sola vez y reúne todas las entidades. Gradle (Groovy DSL,
wrapper `gradlew`/`gradlew.bat`); paquete base `cl.belmontespa.transporte`.

```text
backend/
├── .gitattributes, .gitignore, settings.gradle, build.gradle
├── gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.properties
└── src/
    ├── main/java/cl/belmontespa/transporte/
    │   ├── TransporteApplication.java
    │   ├── client/       EmailClient, FirebaseClient, FcmSender, FirebaseFcmSender, EnvioDiferido
    │   ├── config/       JwtProvider, SecurityConfig, FirebaseConfig
    │   ├── controller/   CrudController, AuthController, CatalogoController, {E}Controller
    │   ├── dto/request/  {E}DTORequest, LoginDTORequest, RefreshDTORequest, TokenFcmDTORequest, RecuperarPasswordDTORequest,
    │   │                 AsignarViajeDTORequest, CambiarEstadoViajeDTORequest, CambiarEstadoPagoDTORequest
    │   ├── dto/response/ {E}DTOResponse, LoginDTOResponse, CatalogosDTOResponse, ApiError
    │   ├── exception/    ApiExceptionHandler
    │   ├── model/        {E}, {C}, Ubicacion, HistorialViaje, Sesion, DetalleMovilConductorId, DetalleViajePasajeroId
    │   ├── repository/   {E}Repository, {C}Repository, UbicacionRepository, HistorialViajeRepository, SesionRepository
    │   └── service/      CrudService, AuthService, {E}Service, UbicacionService
    ├── main/resources/   application.properties,
    │                     db/migration/ V1__esquema.sql, V2__triggers.sql, V3__datos_iniciales.sql
    ├── test/java/cl/belmontespa/transporte/
    │   ├── SeguridadIT, CuentasIT, FlotaIT, PasajeroIT, ViajeIT, PagoIT, NotificacionesIT,
    │   │   TransporteApplicationTests
    │   ├── client/       EmailClientTest, FirebaseClientTest
    │   ├── config/       JwtProviderTest, FirebaseConfigTest
    │   ├── service/      PasajeroServiceTest
    │   └── support/      IntegrationTest, Api, ReiniciarEsquemaConfig, ReiniciarEsquemaConfigTest,
    │                     NotificacionesTestConfig, FcmGrabador
    └── test/resources/   application-test.properties
```

- **`{E}`**: las 11 entidades con recurso en la API (`Administrador`, `Conductor`,
  `DetalleMovilConductor`, `DetalleViajePasajero`, `Empresa`, `Movil`, `Pago`, `Pasajero`,
  `Solicitante`, `Usuario`, `Viaje`): `Model`, `Repository`, `Service`, `Controller`,
  `DTORequest` y `DTOResponse` cada una. Las M:N usan clases ID compuestas.
- **`{C}`**: los 10 catálogos (`Banco`, `Comuna`, `EstadoConductor`, `EstadoMovil`, `EstadoPago`,
  `EstadoSolicitante`, `EstadoSolicitud`, `MedioPago`, `Region`, `TipoRecibo`): solo `Model` y `Repository`, se
  modifican solo en la base; se validan con `existsById` (p. ej. `PagoService` con `banco_id`).
  `CatalogoController` lee bancos, medios de pago, tipos de recibo, regiones y comunas para
  `GET /catalogos`, sin escritura.
- **`Ubicacion`**: sin `Controller` ni DTO (clave compuesta, no sirve como ruta); `UbicacionService`
  solo desde `ViajeService`, y hace el cotejo de comuna de
  [Ubicaciones desde Google Places](#ubicaciones-desde-google-places). **`HistorialViaje`**: solo `Model` y `Repository`, lo llenan los
  triggers; exponerlo permitiría falsear o borrar el historial.
- **DTO**: `DTORequest` solo con campos escribibles por el cliente (nunca `id` ni
  `password_hash`); `DTOResponse` con la clave de negocio en vez del `id` y nunca
  `password_hash`. Ningún `Controller` recibe ni devuelve un `Model`.
- **Servicios y controladores**: `CrudService`/`CrudController` concentran el CRUD genérico y los
  puntos de extensión del alcance "solo lo propio"; `AuthService`/`AuthController`, login,
  sesiones, token FCM y recuperación (los límites de intentos, en un filtro previo); `ViajeService`/`PagoService` y sus controladores, asignación, estados,
  cancelación y pagos. `ApiExceptionHandler` centraliza las respuestas de error.
- **`config`**: `SecurityConfig` (cadena de seguridad), `JwtProvider` (emite y valida el JWT,
  CU-14), `FirebaseConfig` (envío FCM solo con credenciales).
- **`client`**: `EmailClient` (SMTP, RF21) y `FirebaseClient` (FCM, RF12/RF13, sobre
  `FcmSender`); `EnvioDiferido` los ejecuta después del commit y fuera de la transacción.

### Persistencia MariaDB

Conexión por `DB_URL`, `DB_USER` y `DB_PASSWORD` (valores de desarrollo como respaldo). Hibernate
solo valida (`ddl-auto=validate`); Flyway administra el esquema al iniciar:

| Migración | Contenido |
|---|---|
| `V1__esquema.sql` | Tablas, claves y restricciones del [Modelo de datos](#modelo-de-datos). |
| `V2__triggers.sql` | Rol exclusivo por usuario, folio de `VIAJE` (secuencia y formato `TR-`), transiciones de viaje y de pago (incluida la ventana de inicio y al menos un pasajero al asignar), conductor y vehículo activos y habilitados, capacidad, `motivo`, `cantidad_pasajeros` e `HISTORIAL_VIAJE`. |
| `V3__datos_iniciales.sql` | Catálogos, bancos, las 16 regiones y 346 comunas ([Ubicaciones desde Google Places](#ubicaciones-desde-google-places)) y el Administrador inicial. |

Cada sesión de la base usa la hora de Chile (`DB_TIME_ZONE`, por defecto `America/Santiago`)
para que `NOW()`/`CURRENT_TIMESTAMP` coincidan con los `LocalDateTime` de la aplicación; el
servidor necesita las zonas horarias cargadas (la imagen oficial de MariaDB las trae).

### Configuración

Todo por variables de entorno; `application.properties` trae valores de desarrollo como respaldo.

| Variable | Por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mariadb://localhost:3307/belmonte` | Conexión a MariaDB. |
| `DB_USER`, `DB_PASSWORD` | `belmonte`, `belmonte_dev` | Credenciales de la base. |
| `DB_TIME_ZONE` | `America/Santiago` | Zona horaria de cada sesión. |
| `JWT_SECRET` | valor de desarrollo | Clave Base64 de al menos 256 bits para firmar el JWT. |
| `JWT_EXPIRATION_SECONDS` | `3600` | Vigencia del JWT de acceso. |
| `JWT_REFRESH_DIAS` | `30` | Vigencia del refresh token (sesión). |
| `DNI_SECRET` | valor de desarrollo | Clave del HMAC-SHA256 de `PASAJERO.dni_hash`. |
| `PASSWORD_TEMPORAL_MINUTOS` | `30` | Vigencia de la contraseña temporal (RF21). |
| `ALTA_TEMPORAL_HORAS` | `72` | Vigencia de la contraseña inicial de un Administrador nuevo (CU-18). |
| `SPRING_MAIL_HOST` | sin valor | Servidor SMTP; sin él el correo queda desactivado. |
| `SPRING_MAIL_PORT` | `25` (valor de Spring) | Puerto SMTP. |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | sin valor | Credenciales SMTP. |
| `MAIL_FROM` | `no-reply@belmontespa.cl` | Remitente. |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | `true`, `true` | En `false` solo para un relay interno sin credenciales. |
| `FIREBASE_CREDENTIALS_FILE` | sin valor | JSON de la cuenta de servicio de Firebase; sin él el push queda desactivado. |
| `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD` | `jdbc:mariadb://localhost:3307/belmonte_test`, `belmonte`, `belmonte_dev` | Base de las pruebas de integración. |

Fuera de desarrollo se definen `JWT_SECRET`, `DNI_SECRET` y las credenciales de la base.
`DNI_SECRET` se fija una vez: cambiarlo invalida los `dni_hash` guardados. El JSON de Firebase
nunca va al repositorio.

### Notificaciones externas

Correo y push siguen las mismas reglas:

- **Sin configurar** (falta `SPRING_MAIL_HOST` o `FIREBASE_CREDENTIALS_FILE`): la aplicación
  arranca y solo registra que no envió nada. Un archivo de Firebase inexistente o que no sea una
  cuenta de servicio impide arrancar.
- **Después del commit y en otro hilo:** una transacción revertida no genera aviso, y un envío
  fallido solo queda en el log, sin afectar la respuesta.
- **Sin datos personales:** los push llevan folio y fecha agendada, no nombres ni direcciones
  (RNF08). La contraseña temporal nunca se escribe en el log.

**Push (RF12/RF13).** Solo a quien registró un token FCM al iniciar sesión; "Administradores" son
todos los que tengan uno. Los de un viaje incluyen `data.folio` para que la app lo abra; el de
cuentas por aprobar abre "Cuentas por aprobar".

| Evento | Destinatario | Título | Mensaje |
|---|---|---|---|
| Asignar o reasignar | Conductor | Nuevo viaje asignado | Viaje `{folio}` para `{fecha}` |
| Asignar o reasignar | Solicitante | Viaje asignado | Tu viaje `{folio}` ya tiene conductor y vehículo |
| Reasignar a otro conductor | Conductor reemplazado | Viaje reasignado | El viaje `{folio}` fue asignado a otro conductor |
| Pasar a En Curso o Finalizado | Solicitante | Viaje `{folio}` | Estado: `EN_CURSO` o `FINALIZADO` |
| Cancelar un viaje con conductor (por el Solicitante, el Administrador o la anulación de su pago) | Conductor | Viaje cancelado | El viaje `{folio}` fue cancelado |
| Nueva solicitud (V1) | Administradores | Nueva solicitud | Viaje `{folio}` para `{fecha}` |
| Rechazo del Conductor (V8) | Administradores | Viaje rechazado | El viaje `{folio}` volvió a pendiente |
| Rechazo del Conductor (V8) | Solicitante | Viaje sin conductor | Tu viaje `{folio}` volvió a pendiente; se asignará otro conductor |
| Cancelar por el Administrador o por anulación del pago (V7, V6) | Solicitante | Viaje cancelado | Tu viaje `{folio}` fue cancelado |
| Cambio del pago (P2, P3, P4) | Solicitante | Pago del viaje `{folio}` | Estado: `PAGADO`, `RECHAZADO`, `ANULADO` o `REEMBOLSADO` |
| Cuenta pendiente de aprobación (RF01) | Administradores | Cuenta por aprobar | Hay una cuenta nueva por revisar |

**Correo.** Texto plano:

- Recuperar contraseña (RF21): asunto "Belmonte SpA: tu contraseña temporal", con la contraseña
  y su vigencia.
- Revisión de una cuenta (CU-17/RF26): asunto "Belmonte SpA: tu cuenta fue aprobada" o "…fue
  rechazada". La cuenta pendiente no puede iniciar sesión, así que no recibe push.

**Correo en la demostración.** Un proveedor transaccional con plan gratuito (p. ej. Brevo),
por SMTP con las variables `SPRING_MAIL_*` (RNF07). Sin dominio propio, `MAIL_FROM` es un
remitente verificado en el proveedor (una dirección institucional del equipo);
`no-reply@belmontespa.cl` es solo el valor por defecto de desarrollo. Se vigila el cupo diario
del plan.

**Probar el correo con Mailpit.** Con el servicio `mailpit` arriba, desde `backend/` (usuario y
clave cualesquiera, pero definidos, porque la API autentica por defecto); luego "Recuperar
contraseña" y leer el correo en <http://localhost:8025>:

```bash
SPRING_MAIL_HOST=localhost SPRING_MAIL_PORT=1025 \
SPRING_MAIL_USERNAME=dev SPRING_MAIL_PASSWORD=dev ./gradlew bootRun
```

### Pruebas

```bash
cd backend
./gradlew test          # en Windows: gradlew.bat test
```

Las pruebas de integración levantan la aplicación completa detrás de MockMvc contra la MariaDB
real del compose, porque los triggers de `V2` llevan parte de las reglas de negocio. Requieren
el contenedor `mariadb` y el esquema `belmonte_test`, creado una vez:

```bash
mariadb -h127.0.0.1 -P3307 -uroot -proot_dev -e "CREATE DATABASE IF NOT EXISTS belmonte_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL ON belmonte_test.* TO 'belmonte'@'%';"
```

- El esquema se reinicia en cada ejecución (Flyway `clean` + `migrate`) con una guarda que solo
  lo permite si su nombre termina en `_test`: las pruebas no pueden borrar `belmonte`.
- Sin avisos reales: correo a un SMTP en memoria (GreenMail) y push a un grabador que reemplaza a Firebase.
- Cada prueba crea sus datos por la API real; no dependen del orden.

| Pruebas | Cubren |
|---|---|
| `SeguridadIT` | JWT (ausente, alterado, vencido, otra clave), roles, ruta no listada, login y validación; sesiones (refresh, rotación, reuso, logout) y límites de intentos. |
| `CuentasIT` | Autorregistro, alta y edición de cuentas, recuperación de contraseña y 403 en cuentas ajenas. |
| `FlotaIT` | Vehículos y habilitación de conductores. |
| `PasajeroIT` | Hash del documento, pasajeros de un viaje, capacidad y contador (trigger). |
| `ViajeIT` | Creación, ciclo Pendiente → Finalizado, asignación y reasignación, cancelación, historial (trigger) y consultas por rol. |
| `PagoIT` | Registro, estados, reembolso al cancelar el viaje y cancelación del viaje al anular el pago. |
| `NotificacionesIT` | Correo de recuperación (con la clave se inicia sesión) y push de cada evento. |
| Unitarias | `JwtProviderTest`, `PasajeroServiceTest`, `EmailClientTest`, `FirebaseClientTest`, `FirebaseConfigTest` y la guarda del esquema. |

## Frontend (Android)

### Arquitectura y responsabilidades

Android Studio, **Kotlin y Jetpack Compose**, Gradle con Kotlin DSL. **MVVM con Repository y
fuentes locales y remotas**, en un único módulo `app` organizado por responsabilidades:

```text
Pantalla Compose ⇄ ViewModel ⇄ Repository ⇄ { Room / SQLite (local) , Retrofit / API REST (remoto) }
```

- **View / Screen** dibuja y envía acciones al **ViewModel**, que valida entradas de presentación,
  llama al **Repository** y expone el estado. El Repository coordina el **Local DataSource** (Room
  y DAO) y el **Remote DataSource** (Retrofit), actualiza la caché y gestiona las operaciones
  pendientes. **Modelos y mapeadores** separan contratos JSON, entidades locales y datos de la app.
  **Hilt** inyecta dependencias.
- Las pantallas no acceden a Retrofit, Room ni los DAO; los ViewModel dependen de los repositorios.
- El backend es la autoridad sobre permisos, asignaciones, disponibilidad y transiciones; las
  validaciones Android son de experiencia de usuario y no reemplazan las del servidor.
- Sin capa obligatoria de casos de uso al inicio; puede agregarse para operaciones complejas o
  compartidas.

### Tecnologías

| Ámbito | Tecnologías |
|---|---|
| Desarrollo y build | Android Studio (desarrollo, depuración, emuladores, compilación); Kotlin; Gradle Kotlin DSL y catálogo de versiones; KSP (anotaciones de Room y herramientas que lo requieran). |
| UI y estado | Jetpack Compose; Material 3 (tema y componentes); Navigation Compose (flujos por rol); AndroidX ViewModel; Kotlin Coroutines y Flow / StateFlow. |
| Datos | Room / SQLite (caché de viajes y solicitudes, cola de cambios pendientes); Retrofit con convertidor JSON de Kotlin Serialization (DTO); OkHttp (transporte HTTP e incorporación del JWT); WorkManager (sincronización persistente de pendientes); DataStore (preferencias y metadatos pequeños); Android Keystore (claves que cifran la sesión persistida). |
| Integración | Hilt; Firebase Cloud Messaging (recepción y token del dispositivo); Google Places SDK for Android (origen y destino con coordenadas); Intents de Android (Google Maps o Waze). |
| Pruebas | JUnit, AndroidX Test, Espresso y Compose UI Test. |

Versiones fijadas en `gradle/libs.versions.toml` y en los archivos Gradle del proyecto Android,
compatibles con Kotlin, KSP, Android Gradle Plugin y el `minSdk` de esta especificación.

### Organización del código

Android y backend comparten el paquete base `cl.belmontespa.transporte`, pero cada uno tiene su
propio árbol, build y dependencias; no comparten clases ni se acoplan.

```text
cl/belmontespa/transporte/
├── BelmonteApplication.kt, MainActivity.kt
├── di/, model/
├── data/
│   ├── remote/     api/, dto/, interceptor/
│   ├── local/      database/, dao/, entity/
│   └── mapper/, repository/, session/
├── ui/             navigation/, components/, theme/, auth/, solicitante/, administrador/, conductor/
├── notification/
└── sync/
```

Cada pantalla y su ViewModel van en su funcionalidad. `model` contiene los modelos de la app; las
entidades Room y los DTO remotos quedan separados en `data`.

### Navegación y pantallas

![Wireframes de baja fidelidad](mockups/wireframes-belmonte.svg)

Una actividad (`MainActivity`) con Navigation Compose: flujo de autenticación + un flujo por rol.
Cada pantalla tiene un ViewModel que expone su estado (datos, carga, resultados, errores) por
`StateFlow`, observado por Compose según el ciclo de vida. El rol lo entrega el backend tras el
login (sin selector de rol) y decide el flujo; la navegación por rol no sustituye la
autorización del backend.

| Acceso | Contenido | Destino |
|---|---|---|
| Inicio de aplicación | Comprueba la sesión guardada. | Login o inicio del rol. |
| Login | Correo, contraseña, "Iniciar sesión", "Crear cuenta solicitante", "Recuperar contraseña". | Inicio del rol (o "Cambiar contraseña" si entró con la temporal), registro o recuperación. |
| Registro de solicitante | Nombre, correo, teléfono, contraseña, razón social y RUT de empresa. | Login con el correo precargado; si la empresa ya existía, aviso de que la cuenta espera aprobación (CU-17). |
| Recuperar contraseña | Correo y explicación de la temporal (Solicitante y Administrador). | Confirmación genérica y login. |

El Conductor no tiene autorregistro ni recuperación por autoservicio.

| Rol | Barra inferior | Pantallas | Reglas |
|---|---|---|---|
| Solicitante | Mis solicitudes · Perfil | Mis solicitudes (inicio): folio, pasajero(s), fecha, hora, origen, destino, estado y "Nueva solicitud". Nueva solicitud: fecha y hora, origen y destino con Places (con selector de comuna si no calza; ver [Ubicaciones desde Google Places](#ubicaciones-desde-google-places)), uno o más pasajeros con nombre y documento (agregar, editar, quitar antes de enviar), vuelo opcional. Detalle de solicitud: viaje, pasajeros, estado, conductor, vehículo y estado de pago si existe. Perfil: datos y empresa, cambio de contraseña, cerrar sesión. Cambiar contraseña: actual, nueva y confirmación (también Administrador). | Enviar abre el detalle; volver lleva a "Mis solicitudes". "Cancelar solicitud" solo en `PENDIENTE`/`ASIGNADO`, con confirmación; luego se queda en el detalle con el nuevo estado. |
| Administrador | Solicitudes · Conductores · Vehículos · Perfil | Solicitudes (inicio): lista con filtro inicial "Pendientes" y filtros por estado. Cuentas por aprobar (enlace en Solicitudes con su cantidad): aprobar o rechazar (CU-17). Detalle de solicitud: viaje, solicitante y empresa, pasajeros, asignación, motivo y pago; asignar/reasignar, registrar/consultar pago y "Cancelar viaje" (motivo y, si está vencido, monto a reembolsar; CU-15). Asignar conductor y vehículo: conductor + vehículo habilitado, resumen y confirmación. Conductores → Registrar conductor (nombre, correo, teléfono, contraseña inicial) y Detalle de conductor (datos, estado, activo, vehículos habilitados, edición, reemplazo de contraseña, desactivar o reactivar). Vehículos → Registrar vehículo (patente, capacidad, serial GPS opcional) y Detalle de vehículo (datos, estado, activo, conductores habilitados, desactivar o reactivar). Gestionar habilitaciones: solo agregar, sin eliminar mientras no haya un contrato que lo autorice. Registrar pago: monto, banco, medio de pago, tipo de recibo. Detalle de pago: datos, estado y transiciones manuales [P2](#pago) (desde `PENDIENTE`) y [P3](#pago) (`PAGADO` → `ANULADO`). Perfil / Cambiar contraseña / Registrar administrador (desde Perfil; CU-18). | Pagos solo desde el viaje (sin sección propia en el MVP). Sin asignación automática ni dashboard. Lista de solicitudes → detalle → asignación o pago; en las pendientes, "Asignar" abre directo la asignación (HU04); listas de conductores y vehículos → su detalle y registro; habilitaciones desde el detalle del conductor o del vehículo. |
| Conductor | Mis viajes · Perfil | Mis viajes (inicio): agenda del día por hora (pasajero, hora y estado; HU07), con selector de fecha. Detalle de viaje: folio, horario, solicitante y su teléfono, pasajeros, vuelo, origen, destino, vehículo y estado. Perfil: datos y cerrar sesión (la contraseña la gestiona el Administrador). | En el detalle: Navegar (elegir Google Maps o Waze); Iniciar viaje en `ASIGNADO` y dentro de la ventana de ± 1 hora, y Finalizar viaje en `EN_CURSO`, con confirmación; "Rechazar" en `ASIGNADO` antes de iniciar, con motivo y solo con conexión (CU-16); sin conexión, "Pendiente de sincronizar" hasta que el servidor confirme. El selector de fecha no agrega pantalla de historial. |

Reglas comunes:

- Pantallas principales con barra inferior; formularios y detalles con flecha atrás. Salir de un
  formulario con cambios pide confirmar el descarte.
- Iniciar sesión elimina el flujo de acceso del historial; cerrar sesión llama a
  `POST /auth/logout` y elimina el flujo autenticado y la sesión local.
- Sesión que no se puede renovar: reautenticar antes de seguir con operaciones protegidas.
- Pulsar una notificación abre el detalle tras comprobar sesión, rol y acceso.
- Cada pantalla contempla carga, contenido, lista vacía, error y sin conexión, según corresponda.
- Durante un envío la acción se bloquea (sin pulsaciones duplicadas).
- Un cambio de estado se muestra confirmado solo tras la respuesta del backend.
- Un viaje `PENDIENTE` o `ASIGNADO` muestra el chip "Atrasado" o "Vencido" según
  la regla de retraso de [V3](#viaje), calculado en la app con `fh_agenda`.

### Textos y tema

**Tema Material 3**, sin color dinámico (la marca no cambia con el fondo de pantalla); claro u
oscuro según el sistema. Roles tomados de la paleta de los [mockups](mockups/wireframes-belmonte.html):

| Rol | Claro | Oscuro |
|---|---|---|
| `primary` | `#2F6F5E` | `#4FAE95` |
| `onPrimary` | `#FFFFFF` | `#08201B` |
| `primaryContainer` | `#D9EBE6` | `#1F3630` |
| `background` | `#F3F2EE` | `#15171A` |
| `surface` | `#FFFFFF` | `#1D2023` |
| `surfaceVariant` | `#ECEAE3` | `#262A2D` |
| `onSurface` | `#211F1C` | `#EEF0EE` |
| `onSurfaceVariant` | `#6B6A66` | `#9EA3A1` |
| `outline` | `#4A5560` | `#7C8790` |
| `outlineVariant` | `#D8D6D0` | `#2C2F33` |
| `tertiary` (información) | `#3A6EA5` | `#7FB0E0` |
| `tertiaryContainer` | `#DDE8F2` | `#1F2C37` |
| `error` | `#A3402F` | `#E08A78` |
| `onError` | `#FFFFFF` | `#2A0F0A` |
| `errorContainer` | `#F5E1DC` | `#3A221D` |
| Aviso (color extendido) | `#8A6514` | `#D9B45A` |
| Contenedor de aviso (color extendido) | `#F6ECD2` | `#352C17` |

Chips de estado (texto / borde / fondo):

| Estilo | Estados | Colores |
|---|---|---|
| Neutro | `PENDIENTE` (viaje y pago) | `onSurfaceVariant` / `outline` / sin fondo |
| Información | `ASIGNADO` | `tertiary` / `tertiary` / `tertiaryContainer` |
| Lleno | `EN_CURSO`, `OCUPADO`, `EN_USO` | `onPrimary` / `primary` / `primary` |
| Tenue | `FINALIZADO`, inactivo | `onSurfaceVariant` / `outlineVariant` / `surfaceVariant` |
| Correcto | `PAGADO`, `DISPONIBLE` | `primary` / `primary` / `primaryContainer` |
| Peligro | `CANCELADO`, `RECHAZADO` | `error` / `error` / `errorContainer` |
| Peligro, solo borde | `ANULADO`, vencido | `error` / `error` / sin fondo |
| Aviso | `REEMBOLSADO`, atrasado, "Pendiente de sincronizar", cuenta por aprobar | aviso / aviso / contenedor de aviso |

Tipografía Roboto; folio, patente, horas y etiquetas de campo en IBM Plex Mono (licencia OFL,
incluida en el APK).

**Mensajes de error.** Textos en `strings.xml` (español); nunca se muestran trazas ni códigos
técnicos.

| Causa | Texto |
|---|---|
| Sin conexión | "Sin conexión. Revisa tu red e inténtalo de nuevo." En el Conductor, el banner: "Sin conexión. Los cambios se enviarán al reconectar." |
| Sin conexión y sin datos guardados | "No hay datos guardados. Conéctate para cargarlos." |
| Tiempo agotado | "El servidor no respondió a tiempo. Inténtalo de nuevo." |
| 400 | Bajo cada campo, su motivo del `message`; si no es de un campo: "Revisa los datos ingresados." |
| 401 en el login | "Correo o contraseña incorrectos." |
| Sesión que no se puede renovar | "Tu sesión terminó. Inicia sesión de nuevo." |
| 403 sin permiso o recurso ajeno | "No tienes permiso para ver esto." |
| 403 cuenta pendiente | "Tu cuenta espera la aprobación de un Administrador." |
| 403 cuenta rechazada | "Tu cuenta no fue aprobada. Contacta a Belmonte SpA." |
| 404 | "No encontramos este registro." |
| 409 | El `message` del backend (p. ej. "El correo ya está registrado"); luego se recargan los datos. |
| 429 | "Demasiados intentos. Espera {n} minutos." (`n` desde `Retry-After`, redondeado hacia arriba) |
| 500 | "Ocurrió un error inesperado. Inténtalo más tarde." |

**Listas vacías:**

| Pantalla | Texto |
|---|---|
| Mis solicitudes | "Aún no tienes solicitudes. Crea la primera con «Nueva solicitud»." |
| Solicitudes (Administrador) | Con "Pendientes": "No hay solicitudes pendientes."; con otro filtro: "No hay solicitudes en este estado." |
| Cuentas por aprobar | "No hay cuentas por aprobar." |
| Asignar conductor y vehículo | "No hay conductores disponibles. Revisa las habilitaciones o espera a que terminen sus viajes." (CU-04) |
| Conductores | "Aún no hay conductores. Registra el primero." |
| Vehículos | "Aún no hay vehículos. Registra el primero." |
| Detalle de conductor / de vehículo | "Sin vehículos habilitados." / "Sin conductores habilitados." |
| Mis viajes | "No tienes viajes para este día." |

### API y sesión

- Endpoints `/api/v1/` vía Retrofit, con DTO de petición y respuesta alineados con los contratos
  del backend.
- El login envía `correo`, `password` y, si está disponible, `tokenFcm`; el JWT va en las
  peticiones protegidas como `Authorization: Bearer <token>`.
- Sin contraseñas en Room, DataStore ni logs. La sesión persistida se cifra con una clave de
  Android Keystore; DataStore no se considera cifrado por sí mismo.
- `accessToken` y `refreshToken` se guardan cifrados como la sesión. Ante un 401 en una petición
  protegida, OkHttp renueva una sola vez con `POST /auth/refresh` (sin renovaciones simultáneas)
  y repite la petición; si la renovación falla, la sesión termina y se pide login. Con
  `cambioPasswordRequerido` no hay refresh: tras cambiar la contraseña, la app inicia sesión con
  la nueva.
- Los errores HTTP se traducen a mensajes comprensibles, con tratamiento distinto para
  autenticación, permisos, validación, conflictos de negocio y conectividad.

### Persistencia y conectividad

- Crear solicitudes y obtener información nueva requiere conexión; la app no es offline-first.
- Room permite consultar viajes y solicitudes ya sincronizados y guarda los cambios de estado
  permitidos hechos durante un corte, marcados como pendientes de sincronización.
- WorkManager los envía cuando hay conexión y sesión válida, en orden por viaje. Un cambio local
  pendiente nunca se muestra como confirmado por el servidor.
- Los fallos temporales de conexión se reintentan; un rechazo por permisos o reglas de negocio
  obliga a actualizar los datos y mostrar el conflicto, sin reintentos indefinidos.
- Caché y cola se asocian a la cuenta, para que otro usuario no vea ni envíe sus datos. Al cerrar
  sesión con cambios pendientes se avisa y se permite sincronizarlos o descartarlos antes de
  borrar los datos locales.

### Base de datos local

`belmonte_local.db`, **Room sobre SQLite**, versión **1**. Guarda viajes y solicitudes consultados,
sus pasajeros y los cambios de estado pendientes del Conductor; MariaDB sigue siendo la fuente
autorizada de reglas y estados confirmados. No replica todas las tablas del backend: el MVP no
gestiona flota, cuentas ni pagos sin conexión.

Tipos **Kotlin** (`String`, `Int`, `Double`, `Long`; `?` = admite `null`). Las fechas de negocio
son `String` ISO 8601 interpretadas en `America/Santiago`, según el contrato del backend; los
instantes técnicos son `Long` en milisegundos desde Unix.

**`viajes_cache`**

| Campo | Tipo Kotlin | Nulo | Regla |
|---|---|---|---|
| `cuenta` | `String` | no | Correo de la cuenta autenticada, según el backend. |
| `folio` | `String` | no | Identificador público del viaje. |
| `estado_confirmado` | `String` | no | Último estado recibido del servidor. |
| `correo_solicitante` | `String` | no | Solicitante del viaje. |
| `nombre_solicitante` | `String` | no | Solicitante del viaje. |
| `telefono_solicitante` | `String?` | sí | Solicitante del viaje; NULL si no lo registró. |
| `razon_social` | `String` | no | Empresa del Solicitante. |
| `fh_agenda` | `String` | no | Fecha y hora programada. |
| `fh_creacion` | `String` | no | Fecha y hora de creación. |
| `fh_asignacion` | `String?` | sí | Fechas informadas por el servidor. |
| `fh_inicio` | `String?` | sí | Fechas informadas por el servidor. |
| `fh_fin` | `String?` | sí | Fechas informadas por el servidor. |
| `numero_vuelo` | `String?` | sí | Número de vuelo. |
| `cantidad_pasajeros` | `Int` | no | Cantidad confirmada por el servidor. |
| `origen_direccion` | `String` | no | Dirección de origen. |
| `origen_comuna` | `String` | no | Dirección de origen. |
| `origen_region` | `String` | no | Dirección de origen. |
| `origen_latitud` | `Double` | no | Coordenadas de origen. |
| `origen_longitud` | `Double` | no | Coordenadas de origen. |
| `destino_direccion` | `String` | no | Dirección de destino. |
| `destino_comuna` | `String` | no | Dirección de destino. |
| `destino_region` | `String` | no | Dirección de destino. |
| `destino_latitud` | `Double` | no | Coordenadas de destino. |
| `destino_longitud` | `Double` | no | Coordenadas de destino. |
| `correo_conductor` | `String?` | sí | Conductor asignado. |
| `nombre_conductor` | `String?` | sí | Conductor asignado. |
| `patente_movil` | `String?` | sí | Vehículo asignado. |
| `hash_pago` | `String?` | sí | Resumen del pago asociado. |
| `estado_pago` | `String?` | sí | Resumen del pago asociado. |
| `motivo` | `String?` | sí | Motivo de la cancelación por el Administrador (V7) o del rechazo (V8). |
| `descargado_en` | `Long` | no | Instante de actualización de la caché. |

PK compuesta **`cuenta + folio`**. Las ubicaciones se guardan en la fila como instantánea para consulta; no hay catálogo local de direcciones.

**`pasajeros_viaje_cache`**

| Campo | Tipo Kotlin | Nulo | Regla |
|---|---|---|---|
| `cuenta` | `String` | no | Cuenta propietaria de la caché. |
| `folio` | `String` | no | Viaje al que pertenece. |
| `dni_hash` | `String` | no | Identificador entregado por el backend. |
| `nombre` | `String` | no | Nombre del pasajero. |

PK compuesta **`cuenta + folio + dni_hash`**; `cuenta + folio` referencia a `viajes_cache` y eliminar un viaje de la caché elimina sus pasajeros. Se llena con `pasajeros` de `ViajeDTOResponse`. El RUT o pasaporte original no se persiste.

**`operaciones_pendientes`**

| Campo | Tipo Kotlin | Nulo | Regla |
|---|---|---|---|
| `operacion_id` | `String` | no | UUID generado localmente; clave primaria. |
| `cuenta` | `String` | no | Cuenta que realizó la acción. |
| `folio` | `String` | no | Viaje afectado. |
| `secuencia` | `Int` | no | Orden de las operaciones dentro del viaje. |
| `estado_origen` | `String` | no | Estado desde el cual se solicitó el cambio. |
| `estado_destino` | `String` | no | `EN_CURSO` o `FINALIZADO`. |
| `estado_envio` | `String` | no | `PENDIENTE`, `ENVIANDO` o `CONFLICTO`. |
| `creado_en` | `Long` | no | Instante de creación local. |
| `intentos` | `Int` | no | Número de intentos de envío. |
| `ultimo_intento_en` | `Long?` | sí | Último intento. |
| `codigo_error` | `String?` | sí | Código técnico sin datos personales. |

PK `operacion_id`; UQ **`cuenta + folio + secuencia`**; `cuenta + folio` referencia al viaje de la misma cuenta en `viajes_cache`.

`consultas_cache` y `consulta_viaje_cache` distinguen una lista vacía de una nunca descargada.

**`consultas_cache`**

| Campo | Tipo Kotlin | Nulo | Regla |
|---|---|---|---|
| `cuenta` | `String` | no | Cuenta que realizó la consulta. |
| `clave_consulta` | `String` | no | Identifica la consulta y sus filtros. |
| `sincronizada_en` | `Long` | no | Instante de la última respuesta completa guardada. |

PK compuesta **`cuenta + clave_consulta`**.

**`consulta_viaje_cache`**

| Campo | Tipo Kotlin | Nulo | Regla |
|---|---|---|---|
| `cuenta` | `String` | no | Cuenta propietaria de los datos. |
| `clave_consulta` | `String` | no | Consulta a la que pertenece el resultado. |
| `folio` | `String` | no | Viaje devuelto por la consulta. |

PK compuesta **`cuenta + clave_consulta + folio`**; `cuenta + clave_consulta` referencia a `consultas_cache` y `cuenta + folio` a `viajes_cache`. Ejemplos de consulta: agenda del Conductor de una fecha, solicitudes propias o pendientes del Administrador.

Reglas de la cola (`operaciones_pendientes`):

- Se puede encolar Iniciar y luego Finalizar un mismo viaje, en ese orden.
- Dos pulsaciones de la misma acción no crean dos operaciones.
- El estado local pendiente no sobrescribe `estado_confirmado`.
- Un conflicto detiene las operaciones posteriores de ese viaje.
- La ventana de inicio (V3) la evalúa el servidor al recibir la operación: un Iniciar encolado
  que llega después de `fh_agenda` + 1 hora responde 409 y queda en `CONFLICTO`, y la app indica
  avisar al Administrador.
- Una sesión que no se puede renovar pausa el envío hasta autenticarse con la misma cuenta.
- Al confirmar una operación, la actualización del viaje y el borrado de la operación van en una
  transacción.
- Si se perdió la respuesta del servidor, se consulta el viaje antes de reintentar; el contrato
  para reconocer una operación ya aplicada debe definirse antes de implementar ese caso (ver
  [Decisiones abiertas](#decisiones-abiertas)).
- El UUID local no supone que el backend admita claves de idempotencia.

Reglas de persistencia local:

- Cada registro pertenece a la cuenta que lo descargó.
- Una respuesta completa reemplaza las asociaciones de su consulta en una transacción. **Una
  consulta fallida no vacía la caché.** Que un viaje deje de aparecer en una lista no borra el
  viaje ni sus operaciones pendientes.
- Viajes y pasajeros se actualizan juntos en una transacción.
- Los DAO exponen consultas observables con `Flow` y escriben con funciones suspendidas; solo la
  capa de datos accede a los DAO.
- Sin contraseñas, JWT ni claves externas en Room.
- La base va en almacenamiento privado y se excluye de las copias de seguridad.
- Cerrar sesión borra los datos de la cuenta después de resolver o descartar sus pendientes.
- Sin migración destructiva automática: los cambios de esquema preservan datos y cola.

### Notificaciones y navegación externa

- FCM notifica asignaciones, reasignaciones, cancelaciones y cambios de estado; la notificación identifica el
  viaje por su folio y provoca una consulta al backend para obtener datos actualizados.
- La app gestiona los canales y el permiso de notificaciones cuando corresponde, obtiene el token
  FCM y lo envía en el login; si cambia durante la sesión (`onNewToken`), lo envía con
  `POST /auth/token-fcm`, y sin sesión lo guarda para el próximo login. Al cerrar sesión,
  `POST /auth/logout` lo borra en el servidor.
- Google Places selecciona direcciones y obtiene coordenadas; la clave del SDK tiene
  restricciones de aplicación y de API.
- La ruta se abre por Intent en Google Maps o Waze; sin app compatible, aviso. El MVP no incluye
  motor de rutas ni mapa embebido.

### Compilación, firma y distribución

- JDK 21 para Gradle; `compileSdk` y `targetSdk` 36, `minSdk` 26 (Android 8.0, RNF05); las
  dependencias deben soportar ese mínimo.
- **URL del backend** en `BuildConfig.API_BASE_URL`, por tipo de build: debug,
  `http://10.0.2.2:8080/` (emulador estándar hacia el anfitrión); release, la propiedad Gradle
  `belmonte.apiBaseUrl` con la IP del anfitrión de la demostración, obligatoria para compilar
  release.
- **HTTP:** `network_security_config` lo permite solo hacia `10.0.2.2` y hacia el host de la
  demostración ([Entornos](#entornos)); cualquier otro destino exige HTTPS.
- **Claves de servicios:**
  - `google-services.json` va en `app/`, fuera de Git; cada integrante lo descarga de la consola
    de Firebase.
  - La clave de Places va en `local.properties` (`PLACES_API_KEY`), fuera de Git, y se inyecta
    por `BuildConfig`. Está restringida a la Places API y al paquete `cl.belmontespa.transporte`
    con los SHA-1 de los certificados de debug y de release.
  - Las credenciales de servicios del backend (Firebase Admin, SMTP, `JWT_SECRET`) nunca van en
    el APK.
- **Firma de release:** keystore `belmonte-release.jks` fuera del repositorio; su ruta, alias y
  contraseñas van en `~/.gradle/gradle.properties` o en variables de entorno. El equipo respalda
  el keystore: sin él no se puede actualizar la app ya instalada. R8 activo en release.
- **Versionado:** `versionCode` sube en cada APK entregado; `versionName` sigue
  `MAYOR.MENOR.PARCHE`; el archivo se llama `belmonte-<versionName>.apk`.
- **Distribución (RNF10):** el APK firmado y su SHA-256 se publican en GitHub Releases del
  repositorio; se instala permitiendo fuentes externas en el dispositivo.

### Pruebas Android

- ViewModel y repositorios con dependencias falsas.
- Contratos HTTP, autenticación y tratamiento de errores.
- DAO, persistencia y migraciones Room.
- Navegación, formularios y estados de las pantallas Compose.
- Cola de sincronización, reintentos y conflictos.
- Flujos completos por rol contra el backend.

### Decisiones abiertas

- Cómo tratar un reintento cuyo cambio el servidor ya aplicó, sin duplicaciones ni conflictos
  incorrectos.
