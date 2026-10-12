# Aplicacion de Belmonte Spa

**Ultima modificacion:** 26-09-2026

## Integrantes:
* Daniel Muñoz
* Michel Sanhueza

## Indice

- [Integrantes](#integrantes)
- [Introduccion](#introduccion)
  - [Descripcion de la problematica](#descripcion-de-la-problematica)
  - [Actores involucrados](#actores-involucrados)
  - [Casos de uso](#casos-de-uso)
- [Requerimientos funcionales](#requerimientos-funcionales)
- [Requisitos no funcionales](#requisitos-no-funcionales)
- [Historias de usuario y Backlog](#historias-de-usuario-y-backlog)
- [Modelamiento del problema](#modelamiento-del-problema)
  - [Decisiones de modelo](#decisiones-de-modelo)
  - [Modelo relacional](#modelo-relacional)
- [Arquitectura](#arquitectura)
  - [Base de datos MariaDB](#base-de-datos-mariadb)
  - [Backend](#backend)
  - [Frontend](#frontend)
  - [Frontend (Android)](#frontend-android)
- [Estructura del backend](#estructura-del-backend)
  - [Propósito](#propósito)
  - [Árbol de carpetas](#árbol-de-carpetas)
  - [Responsabilidad de cada capa](#responsabilidad-de-cada-capa)
  - [CRUD REST](#crud-rest)
  - [Acciones de negocio](#acciones-de-negocio)
  - [Autenticación](#autenticación)
  - [Persistencia MariaDB](#persistencia-mariadb)
  - [Estado y alcance actual](#estado-y-alcance-actual)

## Introduccion

Belmonte SpA hace transporte privado corporativo (traslados ejecutivos y aeropuerto).
Este MVP móvil centraliza el flujo de traslados: captura de solicitudes, asignación a
conductores y enlace con navegación externa (Google Maps/Waze).

### Descripcion de la problematica

Hoy las solicitudes se coordinan a mano por teléfono, correo y WhatsApp, lo que
produce datos críticos erróneos (horarios, direcciones), respuestas lentas y retrasos
en que el conductor reciba su ruta.

### Actores involucrados

- **ACT-01 — Cliente corporativo / Solicitante:** agenda el traslado (datos del viaje
  y del pasajero).
- **ACT-02 — Pasajero:** quien viaja; puede no ser el solicitante.
- **ACT-03 — Recepcionista / Administrador:** asigna solicitudes a conductores;
  gestiona flota (vehículos y habilitaciones) y pagos.
- **ACT-04 — Conductor:** ejecuta el traslado; ve sus viajes del día y abre la ruta en
  un navegador externo.
- **ACT-05 — Sistema de navegación externo (Google Maps / Waze):** recibe las
  coordenadas por Intent y traza la ruta.

> **Nota:** el Anexo de Requerimiento (§2.4) agrupa Solicitante y Pasajero. Aquí se
> separan porque el Pasajero no pertenece necesariamente a la empresa que paga
> (`PASAJERO` no tiene FK a `EMPRESA`) y el Solicitante puede no viajar.

### Casos de uso

#### CU-01 — Registrar solicitud de traslado

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-01 |
| **Objetivo** | Registrar una solicitud de traslado por formulario. |
| **Precondiciones** | Acceso al formulario. |
| **Flujo principal** | 1. Ingresa pasajero, fecha/hora, origen, destino y n.º de vuelo (si aplica).<br>2. El sistema la registra en Pendiente. |
| **Postcondiciones** | Solicitud almacenada y disponible para gestión. |
| **Alternativos / excepciones** | Datos incompletos o inválidos; las reglas de validación no están definidas en el alcance actual. |

#### CU-02 — Visualizar viajes asignados

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-04 |
| **Objetivo** | Consultar los viajes asignados del día. |
| **Precondiciones** | Viajes asignados y conectividad. |
| **Flujo principal** | 1. Accede a la sección de viajes.<br>2. El sistema lista los suyos. |
| **Postcondiciones** | Información de viajes actualizada. |
| **Alternativos / excepciones** | Sin conectividad o sin viajes; comportamiento no definido. |

#### CU-03 — Navegar a la ruta del viaje

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-04 |
| **Objetivo** | Abrir una app de navegación externa con el origen y destino del viaje. |
| **Precondiciones** | Viaje asignado con coordenadas. |
| **Flujo principal** | 1. Selecciona un viaje y presiona "Navegar".<br>2. La app genera un Intent con origen y destino.<br>3. El SO lo entrega a una app compatible, que inicia la navegación. |
| **Postcondiciones** | La app externa recibe los puntos del viaje. |
| **Alternativos / excepciones** | No hay app compatible o el Intent no puede procesarse. |

#### CU-04 — Gestionar y distribuir solicitudes

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03 |
| **Objetivo** | Revisar y asignar solicitudes a conductores disponibles. |
| **Precondiciones** | Solicitudes registradas y conductores disponibles. |
| **Flujo principal** | 1. Consulta las solicitudes y selecciona una.<br>2. Selecciona un conductor disponible (el sistema sugiere sus vehículos habilitados).<br>3. Confirma.<br>4. El sistema pasa el traslado a Asignado y marca conductor y vehículo como Ocupado/En uso. |
| **Postcondiciones** | Solicitud asociada a un conductor y a un vehículo habilitado para él. |
| **Alternativos / excepciones** | Sin conductores disponibles. No hay regla de reasignación automática. |

#### CU-05 — Actualizar estado del traslado

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03; ACT-04 |
| **Objetivo** | Gestionar el ciclo de vida del traslado por estados. |
| **Precondiciones** | Solicitud registrada. |
| **Flujo principal** | Pendiente → (Administrador asigna) Asignado → (Conductor inicia) En Curso → (Conductor finaliza) Finalizado. Al finalizar, conductor y vehículo vuelven a Disponible. |
| **Postcondiciones** | El estado refleja la etapa actual. |
| **Alternativos / excepciones** | No hay reglas para retroceder o reabrir un traslado. La cancelación es CU-08. |

#### CU-06 — Trazar ruta óptima hacia el destino

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-05 |
| **Objetivo** | Calcular y mostrar la ruta con las coordenadas recibidas. |
| **Precondiciones** | Coordenadas válidas de origen y destino. |
| **Flujo principal** | 1. Recibe origen y destino.<br>2. Calcula la ruta con el tráfico disponible.<br>3. Muestra el trazado al conductor. |
| **Postcondiciones** | Ruta visible en la app externa. |
| **Alternativos / excepciones** | Algoritmo, proveedor de tráfico y criterios pertenecen al sistema externo. |

#### CU-07 — Registrar conductor

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03 |
| **Objetivo** | Crear una cuenta de Conductor con credenciales (no hay autorregistro). |
| **Precondiciones** | Administrador autenticado. |
| **Flujo principal** | 1. Ingresa nombre, correo, teléfono y contraseña inicial.<br>2. El sistema valida correo único y campos obligatorios.<br>3. Guarda la contraseña como hash.<br>4. El conductor queda disponible de inmediato para autenticarse y ser asignado (CU-04). |
| **Postcondiciones** | Cuenta de Conductor activa. |
| **Alternativos / excepciones** | Correo existente o campo faltante: rechazo. El Conductor no tiene vista de "cambiar contraseña"; el Administrador la reemplaza en su ficha. |

#### CU-08 — Cancelar solicitud de traslado

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-01 |
| **Objetivo** | Cancelar una solicitud propia, liberando al conductor si lo había. |
| **Precondiciones** | Solicitud propia en Pendiente o Asignado. |
| **Flujo principal** | 1. Abre el detalle y presiona "Cancelar".<br>2. El sistema valida que el estado sea Pendiente o Asignado.<br>3. Pasa a Cancelado y registra fecha/hora.<br>4. Si había conductor y vehículo: vuelven a Disponible, se notifica al conductor por push (FCM) y el viaje sale de su lista "Viajes del día".<br>5. Si había pago asociado, se anula. |
| **Postcondiciones** | Solicitud Cancelada; conductor y vehículo liberados; pago Anulado. |
| **Alternativos / excepciones** | En Curso o Finalizado: botón no disponible y cancelación rechazada. |

#### CU-09 — Registrar vehículo

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03 |
| **Objetivo** | Registrar un vehículo de la flota. |
| **Precondiciones** | Administrador autenticado. |
| **Flujo principal** | 1. Ingresa patente y capacidad; serial GPS opcional (el smartphone del conductor también reporta ubicación).<br>2. El sistema valida patente única.<br>3. El vehículo queda Disponible. |
| **Postcondiciones** | Vehículo activo, habilitable a conductores y asignable. |
| **Alternativos / excepciones** | Patente existente o campo faltante: rechazo. |

#### CU-10 — Habilitar conductor para vehículo

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03 |
| **Objetivo** | Habilitar a un conductor para operar un vehículo. |
| **Precondiciones** | Conductor (CU-07) y vehículo (CU-09) registrados. |
| **Flujo principal** | 1. Selecciona un conductor y uno o más vehículos.<br>2. El sistema registra la relación. |
| **Postcondiciones** | Conductor habilitado; relación M:N. |
| **Alternativos / excepciones** | Relación ya existente: se ignora sin duplicar. |

#### CU-11 — Registrar pago de un viaje

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03 |
| **Objetivo** | Registrar el pago asociado a un viaje. |
| **Precondiciones** | Viaje en estado distinto de Cancelado. |
| **Flujo principal** | 1. Abre el viaje e ingresa monto, banco, medio de pago y tipo de recibo.<br>2. El sistema crea el pago en Pendiente y lo asocia al viaje. |
| **Postcondiciones** | Viaje con pago Pendiente. |
| **Alternativos / excepciones** | Viaje Cancelado: no se registra pago. |

#### CU-12 — Actualizar estado de un pago

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03 |
| **Objetivo** | Reflejar si un pago fue cobrado, rechazado o anulado. |
| **Precondiciones** | Pago en Pendiente. |
| **Flujo principal** | 1. Selecciona un pago.<br>2. Lo marca Pagado o Rechazado. Anulado es automático al cancelarse el viaje (CU-08), no manual. |
| **Postcondiciones** | Pago en su nuevo estado (Pagado, Rechazado o Anulado). |
| **Alternativos / excepciones** | Estado terminal (Rechazado o Anulado): no se puede cambiar. |

#### CU-13 — Recuperar contraseña

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-01; ACT-03 |
| **Objetivo** | Recuperar el acceso sin depender de otra persona. |
| **Precondiciones** | Cuenta de Solicitante o Administrador con correo registrado. |
| **Flujo principal** | 1. Ingresa su correo en "olvidé mi contraseña".<br>2. El sistema valida que sea de un Solicitante o Administrador.<br>3. Genera una contraseña temporal aleatoria, la guarda hasheada en `password_hash` y fija `password_temporal_expira`.<br>4. La envía en texto plano solo al correo registrado, vía `EmailClient` (único momento en que existe en texto plano; nunca se persiste así — RNF01).<br>5. El usuario inicia sesión con ella antes de que expire (CU-14). |
| **Postcondiciones** | El usuario puede autenticarse con la contraseña temporal hasta que expire. |
| **Alternativos / excepciones** | Correo inexistente o de un Conductor: mismo mensaje genérico, sin revelar el motivo. Si expira sin usarse, hay que pedir otra. |

> **Nota:** el Conductor queda fuera a propósito: su contraseña solo la reemplaza el
> Administrador (CU-07/RF14), para que un desvinculado no recupere acceso por su cuenta.

#### CU-14 — Iniciar sesión

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-01; ACT-03; ACT-04 |
| **Objetivo** | Autenticar al usuario y emitir el JWT que autoriza sus acciones por rol. |
| **Precondiciones** | Cuenta existente: autorregistrada (Solicitante, RF01), creada por el Administrador (Conductor, RF14) o por migración Flyway (Administrador). |
| **Flujo principal** | 1. Ingresa correo y contraseña; la app adjunta el token FCM del dispositivo si lo tiene (RF13).<br>2. El sistema valida contra `USUARIO.password_hash`; rechaza si `password_temporal_expira` no es nulo y ya pasó (RF21).<br>3. Determina el rol según en cuál de `SOLICITANTE`/`ADMINISTRADOR`/`CONDUCTOR` existe el `id_usuario` (único, por `USUARIO_ROL_ACTIVO`).<br>4. Guarda o reemplaza `USUARIO.token_fcm` si vino un token.<br>5. Emite un JWT con `sub` = correo, `rol` y `exp` (RNF01).<br>6. Lo retorna; el cliente lo adjunta en cada request. |
| **Postcondiciones** | Usuario autenticado; el `SecurityFilterChain` valida el JWT en cada request y aplica el mapeo rol→endpoint. |
| **Alternativos / excepciones** | Correo inexistente, contraseña incorrecta o contraseña temporal expirada: mismo error genérico, sin indicar qué falló. |

## Requerimientos funcionales

"Sistema" indica un requisito sin actor que lo inicie.

| ID | Actor | Requisito |
|---|---|---|
| **RF01** | Solicitante | Registrarse (con razón social y RUT de su empresa, que crean o reutilizan una fila en `EMPRESA`) y autenticarse (CU-14). |
| **RF02** | Solicitante | Registrar una solicitud: pasajero, fecha, hora, origen y destino (autocompletado de Google Places, con coordenadas) y n.º de vuelo si aplica. |
| **RF03** | Solicitante | Ver el historial y estado de sus solicitudes. |
| **RF04** | Administrador | Ver las solicitudes pendientes de asignación. |
| **RF05** | Administrador | Asignar un conductor disponible y un vehículo habilitado para él; la solicitud pasa a Asignado y ambos a Ocupado/En uso. |
| **RF06** | Conductor | Autenticarse con credenciales asignadas por el Administrador, sin autorregistro (CU-14). |
| **RF07** | Conductor | Ver en tiempo real los viajes asignados del día. |
| **RF08** | Conductor | Ver el detalle de un viaje asignado (pasajero, origen, destino). |
| **RF09** | Conductor | Pasar un traslado a En Curso y a Finalizado; al finalizar, conductor y vehículo vuelven a Disponible. |
| **RF10** | Sistema | Integrar por Intents de Android una app de navegación externa (Google Maps/Waze) que trace la ruta desde las coordenadas. |
| **RF11** | Sistema | Persistir localmente (Room/SQLite) solicitudes y viajes ya consultados para verlos sin conexión; los cambios hechos offline (p. ej. un cambio de estado) quedan en una cola local y se sincronizan por API REST al reconectar. |
| **RF12** | Sistema | Notificar por push (FCM) a los actores afectados ante cambios relevantes (asignación, En Curso, Finalizado), sin recarga manual. |
| **RF13** | Sistema | Registrar y mantener actualizado el token FCM de cada usuario autenticado. |
| **RF14** | Administrador | Registrar una cuenta de Conductor (nombre, correo, teléfono, contraseña inicial), utilizable de inmediato. |
| **RF15** | Solicitante | Cancelar una solicitud propia en Pendiente o Asignado; si tenía conductor y vehículo se liberan (Disponible), se anula el pago asociado y se notifica al conductor por push. |
| **RF16** | Administrador | Registrar un vehículo con patente y capacidad; serial GPS opcional (la ubicación también viene del smartphone del conductor). |
| **RF17** | Administrador | Habilitar uno o más conductores para un vehículo. |
| **RF18** | Administrador | Registrar el pago de un viaje en cualquier estado previo a Cancelado (monto, banco, medio de pago, tipo de recibo); nace en Pendiente. |
| **RF19** | Administrador | Marcar un pago como Pagado o Rechazado; si el viaje se cancela (RF15), se anula automáticamente. |
| **RF20** | Solicitante | Ver el estado de pago de sus viajes en el detalle de la solicitud. |
| **RF21** | Solicitante, Administrador | Recuperar la contraseña con su correo (CU-13): contraseña temporal enviada al correo, con expiración. El Conductor no tiene este autoservicio (solo RF14), para que un desvinculado no recupere acceso. |

## Requisitos no funcionales

- **RNF01 (Seguridad):** contraseñas solo como hash; autenticación app↔backend con
  JWT con expiración (CU-14); acceso a cada endpoint validado por rol en un
  `SecurityFilterChain` centralizado. Única excepción de tránsito al "nunca en texto
  plano": la contraseña temporal de RF21, que se envía una vez por correo y no se
  guarda en claro.
- **RNF02 (Conectividad):** requiere Internet continuo (Anexo §3.3); no es
  offline-first, y crear o recibir una solicitud nueva exige conexión. Tolera cortes
  breves para datos ya sincronizados (Room/SQLite) y para cambios de estado de un
  viaje ya asignado, que se encolan y reintentan al reconectar (RF11).
- **RNF03 (Tiempo de respuesta):** consultas (listar solicitudes, viajes del día) en
  menos de 2 s con 4G/Wi-Fi. Un push (RF12/RF13) se refleja en segundos, no minutos.
- **RNF04 (Usabilidad en terreno):** pantallas del Conductor con botones grandes y
  legibles y mínimos pasos para marcar En Curso/Finalizado o abrir la navegación.
- **RNF05 (Compatibilidad):** Android 8.0 (API 26) o superior. El backend expone solo
  contratos JSON REST, sin acoplarse a un cliente.
- **RNF06 (Mantenibilidad):** capas estrictas (`Controller-Service-Repository` en
  Spring Boot, `View-ViewModel-Repository` en Android), de modo que una regla de
  negocio se cambie en un solo lugar.
- **RNF07 (Servicios externos):** tres dependencias con cuota gratuita limitada —
  Google Places API (autocompletado), Firebase Cloud Messaging (push) y un servicio
  de correo, SMTP o proveedor transaccional (contraseña temporal, RF21/CU-13). El uso
  debe monitorearse para no exceder el nivel gratuito.
- **RNF08 (Privacidad):** nombre, teléfono y dirección de pasajeros y solicitantes no
  van a logs, y solo los ven el Solicitante, el Administrador y el Conductor asignado
  de cada solicitud.
- **RNF09 (Datos académicos):** solo datos ficticios, sintéticos, agregados o públicos
  anonimizados (Anexo §4; Ley N.º 21.719 y Ley N.º 19.628). Los antecedentes se
  comparten por canales institucionales con acceso restringido.
- **RNF10 (Entregable):** colaboración y documentación en Git/GitHub; el ejecutable
  final es un APK firmado en modo release (Anexo, "Alcance tecnológico comprometido").

## Historias de usuario y Backlog

| ID | Historia | Criterios de aceptación | Prioridad | SP |
|---|---|---|---|---|
| HU01 | Como Solicitante, quiero registrarme y autenticarme, para acceder de forma segura a mis solicitudes. (RF01) | El registro se rechaza si el correo existe o falta un campo obligatorio (empresa, contacto, correo, teléfono, contraseña).<br>Credenciales válidas llevan a "Mis solicitudes"; inválidas muestran un error sin indicar qué dato falló.<br>"Empresa" (razón social + RUT) crea o reutiliza una fila en `EMPRESA`; no se duplica. | Alta | 3 |
| HU02 | Como Solicitante, quiero registrar una solicitud eligiendo origen y destino con autocompletado de Google Places, para coordinar el viaje con datos exactos. (RF02) | Origen y destino se eligen de la lista de sugerencias, nunca texto libre; al elegir se capturan latitud y longitud.<br>No se puede enviar si origen o destino no vienen de la lista.<br>N.º de vuelo opcional; pasajero, fecha, hora, origen y destino obligatorios. | Alta | 5 |
| HU03 | Como Solicitante, quiero ver el historial y estado de mis solicitudes, para saber si mi viaje ya fue asignado. (RF03) | La lista muestra pasajero, fecha/hora, ruta (origen→destino) y estado de cada solicitud propia.<br>El estado corresponde al último cambio registrado. | Media | 3 |
| HU04 | Como Administrador, quiero ver las solicitudes pendientes de asignación, para distribuirlas rápido. (RF04) | Solo se listan solicitudes en Pendiente.<br>Cada fila lleva a "Asignar conductor" de esa solicitud. | Alta | 3 |
| HU05 | Como Administrador, quiero asignar o reasignar un conductor disponible a una solicitud, para confirmar el viaje y mantenerlo cubierto. (RF05) | Solo se listan conductores disponibles (sin viaje en curso).<br>Al confirmar, la solicitud pasa a Asignado y aparece en "Viajes del día" del conductor.<br>También se asigna un vehículo habilitado para él; conductor y vehículo pasan a Ocupado/En uso. | Alta | 5 |
| HU06 | Como Conductor, quiero autenticarme con las credenciales que me entrega el Administrador, para acceder a mis viajes sin registrarme. (RF06) | El login del Conductor no ofrece "crear cuenta".<br>Credenciales inválidas muestran un error sin revelar si el correo existe. | Alta | 2 |
| HU07 | Como Conductor, quiero ver en tiempo real mis viajes del día, para organizar mi jornada. (RF07) | Solo viajes del conductor autenticado, de la fecha actual, ordenados por hora.<br>Cada viaje muestra pasajero, hora y estado. | Alta | 3 |
| HU08 | Como Conductor, quiero ver el detalle de un viaje asignado, para confirmar los datos antes de iniciar. (RF08) | Muestra pasajero, n.º de vuelo (si aplica), origen y destino completos.<br>Incluye el botón "Navegar" y los controles de cambio de estado. | Alta | 3 |
| HU09 | Como Conductor, quiero pasar un traslado a En Curso y a Finalizado, para reflejar el avance real. (RF09) | "Iniciar viaje" solo habilitado en Asignado; "Finalizar viaje" solo en En Curso.<br>Cada cambio registra su fecha/hora.<br>Al finalizar, conductor y vehículo vuelven a Disponible. | Alta | 3 |
| HU10 | Como Conductor, quiero abrir la ruta en Google Maps o Waze desde la app, para no copiar direcciones a mano. (RF10) | "Navegar" abre Google Maps o Waze con el destino precargado.<br>Sin app de navegación instalada, se muestra un aviso en vez de fallar en silencio. | Alta | 3 |
| HU11 | Como Conductor, quiero seguir consultando mis viajes sin conexión y que los cambios se sincronicen al recuperarla, para no perder información en terreno. (RF11) | Solicitudes y viajes ya cargados se consultan sin conexión.<br>Un cambio de estado offline se guarda en una cola local, marcado "pendiente de sincronizar".<br>Al reconectar, la cola se envía sola al backend. | Media | 8 |
| HU12 | Como Solicitante, quiero recibir un push cuando cambie el estado de mi traslado, para enterarme sin recargar la app. (RF12) | Push al pasar a Asignado, En Curso o Finalizado.<br>Al abrir la notificación o "Mis solicitudes", el estado ya está actualizado. | Media | 5 |
| HU13 | Como Conductor, quiero recibir un push cuando se me asigna un viaje, para enterarme de inmediato. (RF12) | Push cuando el Administrador me asigna una solicitud.<br>Al abrirla, el viaje aparece en "Viajes del día" con sus datos completos. | Alta | 5 |
| HU14 | Como usuario autenticado (cualquier rol), quiero que la app registre mi token de notificaciones al iniciar sesión, para recibir avisos dirigidos. (RF13) | Al iniciar sesión, la app registra o actualiza mi token FCM en el backend.<br>Si el token cambia (reinstalación, otro dispositivo), reemplaza al anterior sin duplicar registros. | Alta | 3 |
| HU15 | Como Administrador, quiero registrar una cuenta de Conductor con sus credenciales, para que se autentique sin autorregistrarse. (RF14) | Se rechaza si el correo existe o falta un campo obligatorio.<br>El Administrador define la contraseña en el formulario (no se genera sola); se guarda como hash.<br>El conductor queda disponible de inmediato en "Asignar conductor" (HU05).<br>No hay vista de "cambiar contraseña" para el Conductor: el Administrador la reemplaza en su ficha. | Alta | 3 |
| HU16 | Como Solicitante, quiero cancelar una solicitud que ya no necesito, para liberar al conductor y evitar un viaje innecesario. (RF15) | "Cancelar" solo disponible en Pendiente o Asignado; no en En Curso ni Finalizado.<br>Al cancelar pasa a Cancelado y se registra la fecha/hora.<br>Si tenía conductor y vehículo, vuelven a Disponible, se notifica al conductor por push (RF12) y el viaje sale de "Viajes del día".<br>Si tenía pago asociado, queda Anulado. | Alta | 3 |
| HU17 | Como Administrador, quiero registrar un vehículo con patente y capacidad, para poder asignarlo. (RF16) | Patente obligatoria y única; capacidad mayor a 0.<br>GPS serial opcional (se puede usar el GPS del smartphone del conductor).<br>Queda en estado inicial Disponible. | Media | 3 |
| HU18 | Como Administrador, quiero habilitar conductores para un vehículo, para controlar quién lo opera. (RF17) | Solo conductores ya registrados (HU15).<br>Relación M:N entre vehículos y conductores.<br>Habilitar una relación existente no la duplica. | Media | 2 |
| HU19 | Como Administrador, quiero registrar el pago de un viaje en cualquier momento antes de que se cancele, para no depender de que termine el servicio. (RF18) | Se puede registrar para un viaje en cualquier estado distinto de Cancelado.<br>El hash del pago es único.<br>Banco, medio de pago y tipo de recibo se eligen de catálogos.<br>Nace en Pendiente. | Media | 5 |
| HU20 | Como Administrador, quiero actualizar el estado de un pago, o que se anule solo si se cancela el viaje, para mantener la facturación al día. (RF19) | Transiciones válidas: Pendiente→Pagado, Pendiente→Rechazado, (Pendiente\|Pagado)→Anulado.<br>Anulado es terminal.<br>La anulación por cancelación del viaje (HU16) es automática. | Media | 3 |
| HU21 | Como Solicitante, quiero ver el estado de pago de mis viajes, para saber si ya fueron cobrados. (RF20) | Se muestra en "Detalle de solicitud" (HU03) solo si el viaje tiene pago asociado. | Baja | 2 |
| HU22 | Como Solicitante o Administrador, quiero recuperar mi contraseña si la olvido, para volver a acceder sin depender de otra persona. (RF21) | Solo Solicitante y Administrador; para cualquier otro correo se responde el mismo mensaje genérico.<br>La contraseña temporal expira tras un plazo; si no se usa, hay que pedir otra. | Media | 3 |

## Modelamiento del problema

Motor: **MariaDB**. El esquema se crea con las migraciones Flyway de
`backend/src/main/resources/db/migration`. Las PK simples usan `AUTO_INCREMENT`.

| Entidad | Atributo | Tipo de dato | Clave | Descripción |
|---|---|---|---|---|
| REGION | id_region | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| REGION | nombre | VARCHAR(60) | UNIQUE, NOT NULL | |
| COMUNA | id_comuna | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| COMUNA | nombre | VARCHAR(80) | NOT NULL (UNIQUE junto a id_region) | |
| COMUNA | id_region | BIGINT UNSIGNED | FK → REGION, NOT NULL | |
| UBICACION | id_ubicacion | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| UBICACION | direccion | VARCHAR(200) | NOT NULL (UNIQUE junto a id_comuna, latitud, longitud) | Dirección georreferenciada reutilizable como origen o destino (RF10). |
| UBICACION | id_comuna | BIGINT UNSIGNED | FK → COMUNA, NOT NULL | |
| UBICACION | latitud | DECIMAL(9,6) | NOT NULL (UNIQUE junto a direccion, id_comuna, longitud) | |
| UBICACION | longitud | DECIMAL(9,6) | NOT NULL (UNIQUE junto a direccion, id_comuna, latitud) | |
| USUARIO | id_usuario | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| USUARIO | nombre | VARCHAR(120) | NOT NULL | |
| USUARIO | correo | VARCHAR(150) | UNIQUE, NOT NULL | Login. |
| USUARIO | telefono | VARCHAR(20) | — | |
| USUARIO | password_hash | VARCHAR(200) | NOT NULL | Nunca texto plano. |
| USUARIO | password_temporal_expira | DATETIME | — | NULL: contraseña normal. Con valor: `password_hash` es una contraseña temporal válida hasta ese instante (RF21). Siempre NULL para CONDUCTOR. |
| USUARIO | fecha_registro | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| USUARIO | token_fcm | VARCHAR(255) | — | Token push del dispositivo actual; NULL hasta el primer login desde la app. Se reemplaza en cada login, no se duplica (RF13). |
| USUARIO_ROL_ACTIVO | id_usuario | BIGINT UNSIGNED | PK, FK → USUARIO | Exclusividad de rol: triggers `BEFORE INSERT` en `SOLICITANTE`/`ADMINISTRADOR`/`CONDUCTOR` insertan aquí primero; la PK rechaza un segundo rol. Sin columnas propias. |
| EMPRESA | id_empresa | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| EMPRESA | razon_social | VARCHAR(50) | NOT NULL | |
| EMPRESA | rut | VARCHAR(15) | UNIQUE, NOT NULL | Identificador legal único. |
| SOLICITANTE | id_usuario | BIGINT UNSIGNED | PK, FK → USUARIO | Extensión 1:1 (RF01). |
| SOLICITANTE | empresa_id_empresa | BIGINT UNSIGNED | FK → EMPRESA, NOT NULL | |
| ADMINISTRADOR | id_usuario | BIGINT UNSIGNED | PK, FK → USUARIO | Extensión 1:1, sin atributos propios. Se crea por migración Flyway de carga inicial; no hay autorregistro ni endpoint que lo cree. |
| ESTADO_CONDUCTOR | id_estado | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| ESTADO_CONDUCTOR | estado | VARCHAR(20) | UNIQUE | P. ej. disponible/ocupado; sin NOT NULL en el DDL. |
| CONDUCTOR | id_usuario | BIGINT UNSIGNED | PK, FK → USUARIO | Extensión 1:1. |
| CONDUCTOR | estado_conductor_id_estado | BIGINT UNSIGNED | FK → ESTADO_CONDUCTOR, NOT NULL | Hace que `VIAJE.conductor_id_usuario` solo referencie usuarios con este rol. |
| ESTADO_MOVIL | id_estado_mov | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| ESTADO_MOVIL | nombre | VARCHAR(20) | UNIQUE, NOT NULL | P. ej. disponible/en mantención. |
| MOVIL | id_movil | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| MOVIL | patente | VARCHAR(6) | UNIQUE, NOT NULL | |
| MOVIL | gps_serial | INT | — | Opcional (RF16). |
| MOVIL | estado_movil_id_estado_mov | BIGINT UNSIGNED | FK → ESTADO_MOVIL, NOT NULL | |
| MOVIL | capacidad | INT | NOT NULL | Pasajeros. |
| DETALLE_MOVIL_CONDUCTOR | id_movil | BIGINT UNSIGNED | PK, FK → MOVIL | Habilitación vehículo↔conductor. |
| DETALLE_MOVIL_CONDUCTOR | id_usuario | BIGINT UNSIGNED | PK, FK → CONDUCTOR | |
| PASAJERO | id_pasajero | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| PASAJERO | dni_hash | VARCHAR(50) | UNIQUE, NOT NULL | Hash del documento de identidad: RUT chileno o pasaporte extranjero. |
| PASAJERO | nombre | VARCHAR(120) | NOT NULL | Puede no coincidir con el solicitante (CU-01). |
| ESTADO_SOLICITUD | id_estado | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| ESTADO_SOLICITUD | nombre_estado | VARCHAR(20) | UNIQUE, NOT NULL | PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO (RF05/RF09) y CANCELADO (RF15/CU-08). |
| BANCO | id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| BANCO | nombre | VARCHAR(50) | NOT NULL | |
| BANCO | cod_sbif | INT | UNIQUE, NOT NULL | Código SBIF; identificador externo único. |
| ESTADO_PAGO | id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| ESTADO_PAGO | estado | VARCHAR(20) | UNIQUE, NOT NULL | Pendiente, Pagado, Rechazado o Anulado (RF18/RF19). |
| MEDIO_PAGO | id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| MEDIO_PAGO | nombre | VARCHAR(20) | UNIQUE, NOT NULL | P. ej. transferencia, tarjeta. |
| TIPO_RECIBO | id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| TIPO_RECIBO | nombre | VARCHAR(20) | UNIQUE, NOT NULL | |
| PAGO | id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| PAGO | hash | VARCHAR(50) | UNIQUE, NOT NULL | Generado por el backend al crear el pago; no viene de una pasarela externa. |
| PAGO | monto | INT | NOT NULL | |
| PAGO | estado_pago_id | BIGINT UNSIGNED | FK → ESTADO_PAGO, NOT NULL | |
| PAGO | banco_id | BIGINT UNSIGNED | FK → BANCO, NOT NULL | |
| PAGO | empresa_id_empresa | BIGINT UNSIGNED | FK → EMPRESA, NOT NULL | Empresa que paga. |
| PAGO | medio_pago_id | BIGINT UNSIGNED | FK → MEDIO_PAGO, NOT NULL | |
| PAGO | tipo_recibo_id | BIGINT UNSIGNED | FK → TIPO_RECIBO, NOT NULL | |
| PAGO | fecha_pago | DATETIME | NOT NULL | |
| VIAJE | id_viaje | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| VIAJE | folio | VARCHAR(20) | UNIQUE, NOT NULL | Identificador público y clave de ruta en la API; lo genera el backend al crear el viaje. Inmutable. |
| VIAJE | id_solicitante | BIGINT UNSIGNED | FK → SOLICITANTE, NOT NULL | |
| VIAJE | id_origen | BIGINT UNSIGNED | FK → UBICACION, NOT NULL | |
| VIAJE | id_destino | BIGINT UNSIGNED | FK → UBICACION, NOT NULL | |
| VIAJE | id_estado | BIGINT UNSIGNED | FK → ESTADO_SOLICITUD, NOT NULL | |
| VIAJE | movil_id_movil | BIGINT UNSIGNED | FK → MOVIL | NULL hasta la asignación. |
| VIAJE | pago_id | BIGINT UNSIGNED | FK → PAGO | NULL si no hay pago. |
| VIAJE | cantidad_pasajeros | INT | NOT NULL, DEFAULT 0 | Mantenido por trigger desde `DETALLE_VIAJE_PASAJERO` (nunca lo escribe el cliente); no puede superar la capacidad del móvil asignado. |
| VIAJE | fh_creacion | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Registro de la solicitud. Inmutable. |
| VIAJE | fh_agenda | DATETIME | NOT NULL | Fecha y hora agendada del traslado (RF02), ingresada por el Solicitante. |
| VIAJE | fh_asignacion | DATETIME | — | |
| VIAJE | fh_inicio | DATETIME | — | Paso a En Curso. |
| VIAJE | fh_fin | DATETIME | — | Paso a Finalizado. |
| VIAJE | conductor_id_usuario | BIGINT UNSIGNED | FK → CONDUCTOR | NULL hasta la asignación (RF05). |
| VIAJE | numero_vuelo | VARCHAR(20) | — | Opcional. |
| DETALLE_VIAJE_PASAJERO | id_viaje | BIGINT UNSIGNED | PK, FK → VIAJE | |
| DETALLE_VIAJE_PASAJERO | id_pasajero | BIGINT UNSIGNED | PK, FK → PASAJERO | |
| HISTORIAL_VIAJE | id_historial | BIGINT UNSIGNED | PK, AUTO_INCREMENT | |
| HISTORIAL_VIAJE | id_viaje | BIGINT UNSIGNED | FK → VIAJE, NOT NULL | |
| HISTORIAL_VIAJE | campo_modificado | VARCHAR(30) | NOT NULL | Campo de `VIAJE` que cambió (conductor, móvil, estado, cantidad de pasajeros, etc.). |
| HISTORIAL_VIAJE | valor | VARCHAR(200) | NOT NULL | Valor nuevo, como texto. |
| HISTORIAL_VIAJE | fecha_cambio | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

El actor ACT-05 no tiene tabla: no hay datos propios que persistir.

### Decisiones de modelo

- **`CANCELADO`** se suma a los 4 estados del Anexo (§3.2) para dejar auditoría: la
  solicitud no se borra ni se reutiliza otro estado (RF15/CU-08).
- **`HISTORIAL_VIAJE`** guarda una fila por cada campo que cambia en cada `UPDATE` de
  `VIAJE` (conductor, móvil, estado, cantidad de pasajeros; p. ej. una reasignación,
  HU05). La escriben los triggers de `VIAJE`, no la aplicación. `folio` y
  `fh_creacion` son inmutables y nunca generan fila.
- **Roles como tabla por subtipo** (extensiones 1:1 de `USUARIO`) en vez de un
  `TIPO_USUARIO` con `CHECK`. Como eso no impide que un `id_usuario` tenga dos roles,
  `USUARIO_ROL_ACTIVO` impone la exclusividad.
- **Catálogos en vez de `CHECK`** (`ESTADO_SOLICITUD`, `ESTADO_CONDUCTOR`,
  `ESTADO_MOVIL`, `ESTADO_PAGO`, `MEDIO_PAGO`, `TIPO_RECIBO`): agregar o renombrar un
  valor no altera el DDL de las tablas que lo usan.
- **`VIAJE.cantidad_pasajeros`** es una denormalización deliberada: la mantiene un
  trigger y evita un `COUNT(*)` en cada consulta de "viajes del día" (RNF03).
- **3FN:** `UBICACION` separada de `VIAJE` (las coordenadas dependen de la dirección,
  no del viaje); `PASAJERO` + `DETALLE_VIAJE_PASAJERO` separados (varios pasajeros por
  viaje serían un grupo repetitivo); `id_comuna` es atributo propio de `UBICACION`,
  por lo que `COMUNA`/`REGION` es una jerarquía geográfica normal.

### Modelo relacional

```mermaid
erDiagram
    REGION {
        bigint id_region PK
        varchar nombre UK
    }
    COMUNA {
        bigint id_comuna PK
        bigint id_region FK
    }
    UBICACION {
        bigint id_ubicacion PK
        bigint id_comuna FK
    }
    USUARIO {
        bigint id_usuario PK
        varchar correo UK
    }
    USUARIO_ROL_ACTIVO {
        bigint id_usuario "PK, FK"
    }
    EMPRESA {
        bigint id_empresa PK
        varchar rut UK
    }
    SOLICITANTE {
        bigint id_usuario "PK, FK"
        bigint empresa_id_empresa FK
    }
    ADMINISTRADOR {
        bigint id_usuario "PK, FK"
    }
    ESTADO_CONDUCTOR {
        bigint id_estado PK
    }
    CONDUCTOR {
        bigint id_usuario "PK, FK"
        bigint estado_conductor_id_estado FK
    }
    ESTADO_MOVIL {
        bigint id_estado_mov PK
    }
    MOVIL {
        bigint id_movil PK
        varchar patente UK
        bigint estado_movil_id_estado_mov FK
    }
    DETALLE_MOVIL_CONDUCTOR {
        bigint id_movil "PK, FK"
        bigint id_usuario "PK, FK"
    }
    PASAJERO {
        bigint id_pasajero PK
        varchar dni_hash UK
    }
    ESTADO_SOLICITUD {
        bigint id_estado PK
    }
    BANCO {
        bigint id PK
        int cod_sbif UK
    }
    ESTADO_PAGO {
        bigint id PK
    }
    MEDIO_PAGO {
        bigint id PK
    }
    TIPO_RECIBO {
        bigint id PK
    }
    PAGO {
        bigint id PK
        varchar hash UK
        bigint estado_pago_id FK
        bigint banco_id FK
        bigint empresa_id_empresa FK
        bigint medio_pago_id FK
        bigint tipo_recibo_id FK
    }
    VIAJE {
        bigint id_viaje PK
        varchar folio UK
        bigint id_solicitante FK
        bigint id_origen FK
        bigint id_destino FK
        bigint id_estado FK
        bigint movil_id_movil FK
        bigint pago_id FK
        bigint conductor_id_usuario FK
    }
    DETALLE_VIAJE_PASAJERO {
        bigint id_viaje "PK, FK"
        bigint id_pasajero "PK, FK"
    }
    HISTORIAL_VIAJE {
        bigint id_historial PK
        bigint id_viaje FK
    }

    REGION ||--o{ COMUNA : tiene
    COMUNA ||--o{ UBICACION : tiene
    USUARIO ||--o| SOLICITANTE : extiende
    USUARIO ||--o| ADMINISTRADOR : extiende
    USUARIO ||--o| CONDUCTOR : extiende
    USUARIO ||--o| USUARIO_ROL_ACTIVO : "tiene rol activo"
    EMPRESA ||--o{ SOLICITANTE : emplea
    EMPRESA ||--o{ PAGO : paga
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
    PAGO |o--o{ VIAJE : asociado
    VIAJE ||--o{ DETALLE_VIAJE_PASAJERO : incluye
    PASAJERO ||--o{ DETALLE_VIAJE_PASAJERO : viaja
    VIAJE ||--o{ HISTORIAL_VIAJE : registra
    ESTADO_PAGO ||--o{ PAGO : clasifica
    BANCO ||--o{ PAGO : interviene
    MEDIO_PAGO ||--o{ PAGO : utiliza
    TIPO_RECIBO ||--o{ PAGO : emite
```

El diagrama solo lista columnas PK/FK/UK; el resto está en la tabla anterior.

## Arquitectura

### Base de datos **MariaDB**

Base de datos oficial. Motor `InnoDB` (ACID), soporte directo en Spring
(`MariaDBDialect`, `mariadb-java-client`), licencia GPL v2 y bajo consumo en
desarrollo local junto a Android Studio y la API.

#### Iniciar la base de datos local

Requiere Docker Compose. Desde la raíz del repositorio, en PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d mariadb
```

La API usa `DB_URL`, `DB_USER` y `DB_PASSWORD` (valores locales en `.env.example`) y
Flyway migra al iniciar el backend. `.env` está fuera de Git: cambia las credenciales
en cualquier entorno compartido. `docker compose down` detiene conservando datos;
`docker compose down -v` también los borra.

### Backend

**Spring Boot**:

* **API REST JSON** para el cliente Android (Retrofit o Volley).
* **Seguridad stateless con JWT** (Spring Security). Un único `SecurityFilterChain`
  mapea cada ruta a sus roles y rechaza por defecto (`denyAll`) todo endpoint no
  listado. Única excepción: `/api/v1/auth/**`, que no puede exigir un JWT que aún no
  existe.
* **Capas** `Controller` ➔ `Service` ➔ `Repository`.
* **Spring Data JPA** para la persistencia.
* **Push con Firebase Cloud Messaging:** ante asignación, cambio de estado o
  cancelación, la capa `Service` notifica al actor afectado con el SDK de Firebase
  Admin. El token de dispositivo se actualiza en cada login.

#### Mapeo de roles por endpoint

| Recurso | Roles con acceso | Alcance |
|---|---|---|
| `auth` | Público | Login (CU-14) y recuperación de contraseña (CU-13/RF21); único recurso accesible sin JWT. |
| `usuarios` | Solicitante, Administrador, Conductor | Cada uno solo su propia cuenta (RNF08). |
| `administradores` | Administrador | Gestión de cuentas de Administrador. |
| `conductores` | Administrador (crea/edita, CU-07); Conductor | El Conductor solo su propia cuenta. |
| `solicitantes` | Solicitante (su cuenta); Administrador (lectura) | — |
| `empresas` | Solicitante (crea/reutiliza al registrarse, RF01); Administrador (lectura) | — |
| `moviles` | Administrador | Gestión de flota (CU-09, CU-10). |
| `pagos` | Administrador (gestión, CU-11/CU-12); Solicitante (lectura) | El Solicitante solo los pagos de sus viajes (RF20). |
| `pasajeros` | Solicitante (los registra, CU-01); Administrador (lectura) | — |
| `viajes` | Solicitante (los propios, RF02/RF03); Administrador (todos, CU-04); Conductor (los asignados, RF07/RF08) | — |

Los catálogos y `ubicaciones` no son recursos de la API (ver "CRUD REST").

### Frontend

![Wireframes de baja fidelidad](mockups/wireframes-belmonte.svg)

Vistas del MVP por actor:

**Transversal**
- **Login:** para los tres actores con app.
- **Perfil / Cierre de sesión.**

**Cliente corporativo / Solicitante**
- **Registro:** cuenta propia (el Conductor no se autorregistra).
- **Registrar solicitud de traslado:** CU-01.
- **Mis solicitudes (historial):** traslados con su estado.
- **Detalle de solicitud:** datos, estado y conductor una vez asignado.

**Recepcionista / Administrador**
- **Panel de solicitudes pendientes:** entrada a CU-04.
- **Asignar conductor:** CU-04.
- **Panel general / dashboard** *(segunda iteración)*: conteo de viajes por estado.
- **Sistema de distribución de solicitudes** *(mejora futura, fuera del MVP)*:
  asignación automática de conductor (hoy manual, RF05/CU-04). Sin RF ni HU que lo
  respalde.

**Conductor**
- **Viajes del día:** asignados para hoy, por hora. CU-02.
- **Detalle de viaje asignado:** datos, control En Curso/Finalizado y botón de
  navegación. CU-03 y CU-05.
- **Historial de viajes realizados** *(segunda iteración)*.

### Frontend (Android)

**MVVM**, alineado con las capas del backend:

* **View / ViewModel:** las pantallas en Jetpack Compose solo dibujan estado y emiten
  eventos; la lógica de presentación vive en el `ViewModel`.
* **Repository como única fuente de verdad:** el `ViewModel` no habla con Retrofit ni
  Room; el `Repository` decide entre el `DataSource` local (Room/SQLite) y el remoto
  (Retrofit) y aplica la sincronización offline de RF11.
* **Hilt** para inyectar `ViewModel → Repository → DataSources` y poder sustituir el
  `DataSource` remoto por uno falso en pruebas.
* **Un ViewModel por pantalla**, agrupado por actor (p. ej.
  `RegistrarSolicitudViewModel`, `ViajesDelDiaViewModel`, `AsignarConductorViewModel`).

# Estructura del backend

## Propósito

Monolito Spring Boot por capas; cada capa existe una sola vez y reúne las clases de
todas las entidades. API REST JSON sobre MariaDB con Spring Data JPA y Flyway. Build
con **Gradle** (Groovy DSL, wrapper `gradlew`/`gradlew.bat`). Paquete base:
`cl.belmontespa.transporte`.

## Árbol de carpetas

```text
backend/
├── .gitattributes, .gitignore
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew, gradlew.bat
├── settings.gradle
├── build.gradle
└── src/
    ├── main/
    │   ├── java/cl/belmontespa/transporte/
    │   │   ├── TransporteApplication.java
    │   │   ├── client/       EmailClient, FirebaseClient
    │   │   ├── config/       JwtProvider, SecurityConfig
    │   │   ├── controller/   CrudController, AuthController, {E}Controller
    │   │   ├── dto/
    │   │   │   ├── request/  {E}DTORequest, LoginDTORequest,
    │   │   │   │             RecuperarPasswordDTORequest, AsignarViajeDTORequest,
    │   │   │   │             CambiarEstadoViajeDTORequest, CambiarEstadoPagoDTORequest
    │   │   │   └── response/ {E}DTOResponse, LoginDTOResponse, ApiError
    │   │   ├── exception/    ApiExceptionHandler
    │   │   ├── model/        {E}, {C}, Ubicacion, HistorialViaje,
    │   │   │                 DetalleMovilConductorId, DetalleViajePasajeroId
    │   │   ├── repository/   {E}Repository, {C}Repository, UbicacionRepository,
    │   │   │                 HistorialViajeRepository
    │   │   └── service/      CrudService, AuthService, {E}Service, UbicacionService
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/
    │           ├── V1__crear_esquema_inicial.sql
    │           └── V2__completar_modelo_belmonte.sql
    └── test/java/cl/belmontespa/transporte/
        └── TransporteApplicationTests.java
```

- **`{E}`** — las 11 entidades con recurso en la API: `Administrador`, `Conductor`,
  `DetalleMovilConductor`, `DetalleViajePasajero`, `Empresa`, `Movil`, `Pago`,
  `Pasajero`, `Solicitante`, `Usuario`, `Viaje`. Cada una tiene `Model`, `Repository`,
  `Service`, `Controller`, `DTORequest` y `DTOResponse`.
- **`{C}`** — los 9 catálogos: `Banco`, `Comuna`, `EstadoConductor`, `EstadoMovil`,
  `EstadoPago`, `EstadoSolicitud`, `MedioPago`, `Region`, `TipoRecibo`. Solo `Model` y
  `Repository`: no se exponen ni se modifican por API, solo desde la base de datos.
  Quien necesita validar un valor (p. ej. `PagoService` con `banco_id`) usa
  `existsById` del `Repository` del catálogo.
- **`Ubicacion`** — sin `Controller` ni DTO: su única clave de negocio es compuesta
  (dirección + comuna + coordenadas) y no sirve como clave de ruta. `UbicacionService`
  se usa solo desde `ViajeService`.
- **`HistorialViaje`** — solo `Model` y `Repository`: lo llenan los triggers de `VIAJE`
  (ver `datamodeler/schema.sql`). Exponerlo permitiría falsear o borrar el historial.
- **DTO** — el `DTORequest` solo acepta campos que el cliente puede escribir (nunca
  `id` ni `password_hash`); el `DTOResponse` expone la clave de negocio en vez del `id`
  y nunca incluye `password_hash`. Ningún `Controller` recibe ni devuelve un `Model`.
  `CrudController`/`CrudService` operan sobre estos tipos de forma genérica.

## Responsabilidad de cada capa

| Carpeta | Responsabilidad |
|---|---|
| `model` | Entidades JPA; las relaciones M:N usan clases ID compuestas. |
| `repository` | Interfaces Spring Data JPA. |
| `service` | Lógica de aplicación. `CrudService` concentra el CRUD repetido; `AuthService` valida credenciales y coordina la recuperación de contraseña. |
| `controller` | Endpoints REST. `CrudController` comparte el CRUD entre controladores; `AuthController` expone login y recuperación. |
| `dto` | Contratos de entrada y salida de la API, y `ApiError` para errores. |
| `exception` | Manejo centralizado de excepciones y respuestas de error HTTP. |
| `config` | `SecurityConfig` define la cadena de seguridad; `JwtProvider` emite y valida el JWT (CU-14). |
| `client` | Contratos de servicios externos: `FirebaseClient` (push, RF12/RF13) y `EmailClient` (contraseña temporal, RF21). |
| `resources/db/migration` | Migraciones SQL versionadas de Flyway. |

## CRUD REST

Prefijo `/api/v1/`. El `id` interno no se expone: las rutas usan la columna `UNIQUE`
de negocio de cada entidad.

| Método | Ruta | Acción |
|---|---|---|
| `GET` | `/api/v1/{recurso}` | Listar |
| `GET` | `/api/v1/{recurso}/{valor_unique_key}` | Consultar uno |
| `POST` | `/api/v1/{recurso}` | Crear |
| `PUT` | `/api/v1/{recurso}/{valor_unique_key}` | Solo `usuarios`, `conductores` y `solicitantes` |

| Recurso | Clave de ruta |
|---|---|
| `usuarios`, `administradores`, `conductores`, `solicitantes` | `correo` (de `USUARIO`) |
| `empresas` | `rut` |
| `moviles` | `patente` |
| `pasajeros` | `dni_hash` |
| `pagos` | `hash` |
| `viajes` | `folio` |

- **Sin `DELETE`** en ningún recurso: ninguna fila se destruye por API, a lo más
  cambia de estado (`CANCELADO`, `HISTORIAL_VIAJE`).
- **`PUT`** solo en `usuarios`, `conductores` y `solicitantes` (p. ej. el Administrador
  reemplazando la contraseña de un Conductor, CU-07). El resto solo tiene `GET`/`POST`.
- **`viajes` y `pagos`** no se mutan por `PUT`: conductor, móvil, estado y cantidad de
  pasajeros de `VIAJE`, y el estado de `PAGO`, los valida y audita la base de datos
  (triggers de `datamodeler/schema.sql`) y se cambian solo por "Acciones de negocio".
- **No son recursos:** los 9 catálogos (`bancos`, `comunas`, `estados-conductor`,
  `estados-movil`, `estados-pago`, `estados-solicitud`, `medios-pago`, `regiones`,
  `tipos-recibo`) y `ubicaciones`, que se crea y consulta solo como parte de `viajes`.
- **Tablas puente:** clave de dos columnas, también de negocio; se crean como unidad
  y no tienen `PUT`.
  - `/api/v1/habilitaciones-movil-conductor/{patente}/{correo}`
  - `/api/v1/pasajeros-viaje/{folio}/{dni_hash}`

## Acciones de negocio

| Método | Ruta | Acción |
|---|---|---|
| `POST` | `/api/v1/viajes/{folio}/asignar` | Asignar conductor y vehículo; el viaje pasa a Asignado (CU-04/RF05). Administrador. |
| `POST` | `/api/v1/viajes/{folio}/estado` | Marcar En Curso o Finalizado (CU-05/RF09). Conductor, solo sus viajes. |
| `POST` | `/api/v1/viajes/{folio}/cancelar` | Cancelar el viaje propio (CU-08/RF15). Solicitante. |
| `POST` | `/api/v1/pagos/{hash}/estado` | Marcar Pagado o Rechazado (CU-12/RF19). Administrador. Anulado sigue siendo automático y no pasa por esta ruta. |

Son métodos adicionales de `ViajeController`/`PagoController`, fuera de
`CrudController`. Usan `AsignarViajeDTORequest`, `CambiarEstadoViajeDTORequest` y
`CambiarEstadoPagoDTORequest`, y devuelven `ViajeDTOResponse`/`PagoDTOResponse`.

## Autenticación

| Método | Ruta | Acción |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Autenticar con correo/contraseña y emitir el JWT (CU-14). |
| `POST` | `/api/v1/auth/recuperar-password` | Solicitar una contraseña temporal por correo (CU-13/RF21). |

Único punto de entrada sin JWT. Las atiende `AuthController` con `LoginDTORequest`/
`LoginDTOResponse` y `RecuperarPasswordDTORequest`.

## Persistencia MariaDB

`application.properties` toma la conexión de `DB_URL`, `DB_USER` y `DB_PASSWORD`, con
valores locales de desarrollo como respaldo. Hibernate valida el esquema; Flyway lo
administra:

- `V1__crear_esquema_inicial.sql`: esquema inicial.
- `V2__completar_modelo_belmonte.sql`: completa el modelo de este documento.

## Estado y alcance actual

Lo definido en este documento que todavía no existe en el código:

- **JWT:** la seguridad configurada es HTTP Basic. Sin `AuthController`/`AuthService`/
  `JwtProvider` no hay login ni emisión de JWT, y RNF01 no se cumple.
- **DTO:** los `{E}DTORequest`/`{E}DTOResponse` no existen; los CRUD reciben y
  devuelven `Model`, filtrando `password_hash` en las respuestas de
  `USUARIO`/`CONDUCTOR`/`ADMINISTRADOR`/`SOLICITANTE` — un riesgo de seguridad concreto.
- **Acciones de negocio:** no existen `/asignar`, `/estado` ni `/cancelar`; hoy solo
  hay CRUD genérico.
- **Push:** `FirebaseClient` es solo una interfaz; faltan su implementación y
  configuración, y la persistencia del token de dispositivo (RF13).
- **Correo:** `EmailClient` no existe; no hay envío de la contraseña temporal.
