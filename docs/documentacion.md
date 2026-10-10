# Actores Involucrados — Sistema de Gestión de Traslados

*Belmonte SpA · MVP móvil · DSY1105 · 09 de septiembre de 2026*

## 1. Objetivo

Identificar y caracterizar técnicamente los actores humanos y sistemas
externos que interactúan con la solución móvil de gestión de traslados,
delimitando responsabilidades, puntos de interacción y dependencias del
MVP.

## 2. Contexto del sistema

El proyecto busca centralizar y automatizar el flujo operativo de
traslados corporativos. El MVP contempla captura de solicitudes, gestión
y asignación de viajes, estados del traslado e integración con
aplicaciones de navegación externa.

## 3. Actores identificados

| Actor | Tipo | Responsabilidad / interacción | Información principal |
|---|---|---|---|
| Cliente corporativo / Solicitante | Humano — externo | Inicia una solicitud mediante el formulario digital. | Fecha, hora, origen, destino, vuelo (si aplica) y datos del pasajero. |
| Pasajero | Humano — usuario del servicio | Persona que realiza el traslado; puede coincidir o no con el solicitante. | Nombre del pasajero y datos asociados al servicio. |
| Recepcionista / Administrador | Humano — operador | Gestiona y distribuye solicitudes entre conductores disponibles. | Solicitudes, asignaciones y estado operativo. |
| Conductor | Humano — usuario operativo | Consulta viajes asignados, ejecuta el traslado y utiliza navegación externa. | Viajes, origen, destino, estado y coordenadas. |
| Google Maps / Waze | Sistema externo | Recibe origen/destino georreferenciados mediante navegación invocada desde el dispositivo. | Coordenadas de origen y destino para el trazado de ruta. |

## 4. Relaciones e interacciones

**Solicitante → Plataforma:** Registra la solicitud de traslado con los
datos logísticos requeridos.

**Pasajero → Servicio:** Representa a la persona que realizará el
traslado; puede ser distinta del solicitante.

**Recepcionista / Administrador → Plataforma:** Gestiona y distribuye
solicitudes entre conductores.

**Conductor → Plataforma:** Consulta los viajes asignados y la
información necesaria para ejecutar el servicio.

**Conductor → Google Maps / Waze:** Invoca la navegación externa
mediante Intents del sistema operativo, utilizando origen y destino
georreferenciados.

## 5. Datos relevantes del dominio

La estructura sintética del servicio considera: identificador del viaje,
nombre del pasajero, fecha, hora, dirección de origen georreferenciada,
dirección de destino georreferenciada y estado del traslado.

**Estados:** Pendiente → Asignado → En Curso → Finalizado.

## 6. Dependencias y consideraciones técnicas

- Conectividad a Internet para consumir la API REST y mantener
  actualizaciones operativas.

- Uso de Intents del sistema operativo Android para abrir aplicaciones
  de navegación.

- Google Maps y/o Waze son sistemas externos y generan el trazado de
  navegación.

- El backend Spring Boot procesa centralmente la asignación y los
  estados del viaje.

- Para el MVP académico deben utilizarse datos ficticios, sintéticos,
  agregados o públicos debidamente anonimizados.

## 7. Diagrama técnico de actores

La representación siguiente delimita el sistema y sus principales puntos
de interacción.

![Diagrama de actores](media/image1.png)

---

# Casos de Uso — Belmonte SpA

*MVP de gestión de traslados corporativos*

## 1. Propósito

Este documento formaliza los casos de uso identificados para el MVP de
gestión de traslados corporativos de Belmonte SpA. Se identifican los
actores involucrados y se describe la interacción funcional entre cada
actor, el sistema y los sistemas externos.

## 2. Actores involucrados

Los actores se clasifican según su interacción con el sistema. Se
distingue explícitamente al Cliente corporativo / Solicitante del
Pasajero, ya que quien agenda el traslado puede no ser la persona que
efectivamente realiza el viaje.

| ID | Actor | Tipo | Descripción / responsabilidad |
|---|---|---|---|
| **ACT-01** | Cliente corporativo / Solicitante | Actor humano primario | Empresa o persona (por ejemplo, asistente o área de RRHH) que agenda el traslado. Ingresa el formulario con fecha, hora, origen, destino, número de vuelo si aplica y datos del pasajero. |
| **ACT-02** | Pasajero | Actor humano asociado | Persona que efectivamente realiza el traslado. Puede coincidir o no con quien realizó la solicitud. |
| **ACT-03** | Recepcionista / Administrador | Actor humano primario | Encargado de tomar y distribuir las solicitudes de traslado entre los conductores disponibles. |
| **ACT-04** | Conductor | Actor humano primario | Ejecuta el transporte en terreno, visualiza en tiempo real los viajes asignados para el día y utiliza la aplicación para trazar la ruta mediante navegadores externos. |
| **ACT-05** | Sistema de navegación externo (Google Maps / Waze) | Sistema externo | Recibe las coordenadas georreferenciadas de origen y destino y genera el trazado de ruta al ser invocado por el conductor mediante Intents del sistema operativo. |

## 3. Casos de uso

Los siguientes casos de uso representan las funcionalidades actualmente
identificadas para el MVP.

### CU-01 — Registrar solicitud de traslado

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-01 — Cliente corporativo / Solicitante |
| **Objetivo** | Registrar una solicitud de traslado mediante un formulario digital. |
| **Precondiciones** | El solicitante dispone de acceso al formulario de solicitud. |
| **Flujo principal** | 1. El solicitante ingresa al formulario.<br>2. Registra los datos del pasajero.<br>3. Registra fecha y hora del traslado.<br>4. Registra punto de origen y punto de destino.<br>5. Ingresa número de vuelo cuando corresponda.<br>6. El sistema registra la solicitud en estado Pendiente. |
| **Postcondiciones** | Solicitud almacenada y disponible para gestión. |
| **Flujos alternativos / excepciones** | Datos incompletos o inválidos. Las reglas específicas de validación no están definidas en el alcance actual. |

### CU-02 — Visualizar viajes asignados

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-04 — Conductor |
| **Objetivo** | Permitir al conductor consultar los viajes que tiene asignados para el día. |
| **Precondiciones** | El conductor tiene viajes asignados y conectividad con el sistema. |
| **Flujo principal** | 1. El conductor accede a la sección de viajes.<br>2. El sistema obtiene los viajes asignados.<br>3. El sistema presenta la lista de viajes correspondiente al conductor. |
| **Postcondiciones** | El conductor dispone de la información actualizada de sus viajes. |
| **Flujos alternativos / excepciones** | Sin conectividad o ausencia de viajes asignados. El comportamiento específico no está definido. |

### CU-03 — Navegar a la ruta del viaje

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-04 — Conductor |
| **Objetivo** | Abrir una aplicación externa de navegación utilizando el origen y destino georreferenciados del viaje. |
| **Precondiciones** | Existe un viaje asignado con coordenadas de origen y destino. |
| **Flujo principal** | 1. El conductor selecciona un viaje.<br>2. Presiona la opción de navegación.<br>3. La aplicación genera un Intent con origen y destino.<br>4. El sistema operativo entrega la solicitud a una aplicación compatible de navegación.<br>5. La aplicación externa inicia la navegación. |
| **Postcondiciones** | La aplicación de navegación externa recibe los puntos del viaje. |
| **Flujos alternativos / excepciones** | No existe una aplicación compatible o el Intent no puede ser procesado. |

### CU-04 — Gestionar y distribuir solicitudes

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03 — Recepcionista / Administrador |
| **Objetivo** | Recibir, revisar y asignar solicitudes de traslado a conductores disponibles. |
| **Precondiciones** | Existen solicitudes registradas y conductores disponibles. |
| **Flujo principal** | 1. El administrador consulta las solicitudes.<br>2. Selecciona una solicitud.<br>3. Selecciona un conductor disponible.<br>4. Confirma la asignación.<br>5. El sistema actualiza el estado del traslado a Asignado. |
| **Postcondiciones** | La solicitud queda asociada a un conductor. |
| **Flujos alternativos / excepciones** | No existen conductores disponibles. No se ha definido una regla de reasignación automática. |

### CU-05 — Actualizar estado del traslado

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-03 — Recepcionista / Administrador; ACT-04 — Conductor |
| **Objetivo** | Gestionar el ciclo de vida operativo del traslado mediante estados. |
| **Precondiciones** | Existe una solicitud registrada. |
| **Flujo principal** | 1. El traslado inicia en estado Pendiente.<br>2. Recepcionista / Administrador asigna un conductor → Asignado.<br>3. Conductor inicia el traslado → En Curso.<br>4. Conductor finaliza el traslado → Finalizado. |
| **Postcondiciones** | El estado representa la etapa actual del traslado. |
| **Flujos alternativos / excepciones** | No se encuentran definidas reglas para retroceder estados, cancelar o reabrir un traslado. |

### CU-06 — Trazar ruta óptima hacia el destino

| Campo | Detalle |
|---|---|
| **Actor(es)** | ACT-05 — Sistema de navegación externo |
| **Objetivo** | Calcular y mostrar una ruta utilizando las coordenadas de origen y destino entregadas por la aplicación. |
| **Precondiciones** | El sistema externo recibe coordenadas válidas de origen y destino. |
| **Flujo principal** | 1. El sistema externo recibe origen y destino.<br>2. Procesa las coordenadas.<br>3. Calcula una ruta considerando la información de tráfico disponible.<br>4. Muestra el trazado al conductor. |
| **Postcondiciones** | La ruta de navegación queda visualizada en la aplicación externa. |
| **Flujos alternativos / excepciones** | El algoritmo de cálculo, proveedor de tráfico y criterios internos pertenecen al sistema externo. |

## 4. Relación actor ↔ caso de uso

| Actor | CU-01 | CU-02 | CU-03 | CU-04 | CU-05 | CU-06 |
|---|---|---|---|---|---|---|
| ACT-01 — Cliente corporativo / Solicitante | ✓ | | | | | |
| ACT-02 — Pasajero | | | | | | |
| ACT-03 — Recepcionista / Administrador | | | | ✓ | ✓ | |
| ACT-04 — Conductor | | ✓ | ✓ | | ✓ | |
| ACT-05 — Navegación externa | | | | | | ✓ |

Nota: ACT-02 (Pasajero) se mantiene como actor identificado del dominio,
pero actualmente no tiene un caso de uso directo en el MVP definido. Su
información forma parte de la solicitud registrada por el Cliente
corporativo / Solicitante.

## 5. Ciclo de vida del traslado

**PENDIENTE → ASIGNADO → EN CURSO → FINALIZADO**

Pendiente: solicitud registrada y aún no distribuida. Asignado:
solicitud asociada a un conductor. En Curso: conductor ejecutando el
traslado. Finalizado: traslado completado.

## 6. Integraciones y dependencias técnicas

- Aplicación móvil Android: formularios, visualización de viajes,
  acciones del conductor y gestión de estado.

- API REST: comunicación entre la aplicación móvil y el backend.

- Backend Spring Boot: procesamiento y gestión central de los estados y
  asignaciones.

- Intents del sistema operativo: mecanismo de invocación de aplicaciones
  externas de navegación.

- Google Maps / Waze: navegación y trazado de rutas.

- Datos georreferenciados: coordenadas de origen y destino utilizadas
  para la navegación.

- Conectividad a Internet: requerida para actualizaciones y comunicación
  con la API.

## 7. Trazabilidad funcional

La identificación ACT-01 a ACT-05 y CU-01 a CU-06 permite utilizar estos
elementos como referencias estables para futuras historias de usuario,
requisitos funcionales, diagramas UML y casos de prueba.

---

# Requisitos Funcionales y No Funcionales

*Belmonte SpA — MVP de gestión de traslados corporativos*

## 1. Propósito

Este documento especifica los requisitos funcionales (RF) y no
funcionales (RNF) identificados para el MVP de gestión de traslados
corporativos de Belmonte SpA. Los requisitos se expresan de forma
verificable y se relacionan con los actores, casos de uso y componentes
técnicos definidos para el proyecto.

## 2. Actores relacionados

| ID | Actor | Descripción |
|---|---|---|
| ACT-01 | Cliente corporativo / Solicitante | Registra y consulta solicitudes de traslado. |
| ACT-02 | Pasajero | Persona que efectivamente realiza el traslado; puede ser distinta del solicitante. |
| ACT-03 | Recepcionista / Administrador | Gestiona solicitudes y asigna conductores. |
| ACT-04 | Conductor | Consulta y ejecuta viajes asignados y actualiza su estado. |
| ACT-05 | Sistema de navegación externo | Google Maps/Waze; recibe coordenadas mediante la integración del sistema operativo. |

## 3. Requisitos funcionales

Los siguientes requisitos corresponden a la especificación proporcionada
para el proyecto. Se conserva la numeración RF01–RF12 para facilitar la
trazabilidad.

| ID | Nombre | Actor(es) | Especificación | Criterio de cumplimiento |
|---|---|---|---|---|
| RF01 | Registro y autenticación del solicitante | ACT-01 | El sistema debe permitir al Cliente corporativo/Solicitante registrarse y autenticarse en la aplicación. | Usuario registrado; credenciales válidas para autenticación. |
| RF02 | Registro de solicitud de traslado | ACT-01 | El sistema debe permitir al Cliente corporativo/Solicitante registrar una solicitud indicando datos del pasajero, fecha, hora, dirección de origen, dirección de destino y número de vuelo cuando aplique. | Solicitante autenticado. |
| RF03 | Consulta de historial y estado | ACT-01 | El sistema debe permitir al Cliente corporativo/Solicitante visualizar el historial y estado de sus solicitudes de traslado. | Solicitante autenticado y con solicitudes registradas. |
| RF04 | Consulta de solicitudes pendientes | ACT-03 | El sistema debe permitir al Recepcionista/Administrador visualizar las solicitudes pendientes de asignación. | Administrador autenticado; existen solicitudes pendientes. |
| RF05 | Asignación de conductor | ACT-03 | El sistema debe permitir al Recepcionista/Administrador asignar un conductor disponible a una solicitud de traslado y actualizar su estado a "Asignado". | Solicitud pendiente y conductor disponible. |
| RF06 | Autenticación del conductor | ACT-04 | El sistema debe permitir al Conductor autenticarse mediante credenciales asignadas por el Administrador, sin permitir autorregistro. | Credenciales previamente asignadas por el Administrador. |
| RF07 | Visualización de viajes del día | ACT-04 | El sistema debe permitir al Conductor visualizar en tiempo real los viajes asignados para el día. | Conductor autenticado y conectado al servicio. |
| RF08 | Detalle del viaje | ACT-04 | El sistema debe permitir al Conductor visualizar el detalle de un viaje asignado, incluyendo datos del pasajero, dirección de origen y dirección de destino. | Viaje asignado al conductor. |
| RF09 | Actualización de estado | ACT-04 | El sistema debe permitir al Conductor actualizar el estado de un traslado a "En Curso" y "Finalizado". | Traslado asignado al conductor. |
| RF10 | Integración con navegación externa | ACT-04 / ACT-05 | El sistema debe integrar, mediante Intents de Android, una aplicación externa de navegación (Google Maps/Waze) para trazar la ruta hacia el destino a partir de coordenadas georreferenciadas. | Origen y destino georreferenciados; aplicación de navegación compatible. |
| RF11 | Persistencia local y sincronización | ACT-01 / ACT-03 / ACT-04 | El sistema debe persistir localmente, mediante Room/SQLite, los datos de las solicitudes y sincronizarlos mediante una API REST con el backend. | Aplicación instalada y acceso a almacenamiento local; API disponible para sincronización. |
| RF12 | Conectividad para actualización en tiempo real | ACT-03 / ACT-04 | El sistema debe requerir conectividad a Internet continua para reflejar en tiempo real las actualizaciones de estado de los viajes. | Conectividad a Internet disponible. |

## 4. Requisitos no funcionales

Los RNF describen restricciones y atributos de calidad o implementación
derivados del alcance entregado y del Anexo de Requerimiento. Cuando un
valor cuantitativo no está definido por la fuente, se evita inventarlo.

| ID | Nombre | Categoría | Especificación | Verificación |
|---|---|---|---|---|
| RNF01 | Plataforma móvil | Compatibilidad | La solución móvil debe operar sobre smartphones Android, dado que el uso del conductor se define sobre esta plataforma. | Validar instalación y ejecución en un dispositivo Android compatible. |
| RNF02 | Conectividad | Disponibilidad / comunicación | La solución requiere conectividad a Internet para la comunicación mediante API REST y para reflejar actualizaciones de viajes en tiempo real. | Probar las operaciones que requieren comunicación con backend bajo conectividad disponible. |
| RNF03 | Arquitectura de integración | Interoperabilidad | La comunicación entre aplicación móvil y backend debe utilizar una API REST. | Verificar que las operaciones de integración utilicen endpoints REST definidos para el MVP. |
| RNF04 | Navegación externa | Interoperabilidad | La aplicación debe utilizar Intents del sistema operativo Android para invocar aplicaciones externas de navegación. | Verificar que un viaje con coordenadas válidas pueda abrir una aplicación compatible. |
| RNF05 | Persistencia local | Persistencia | La aplicación debe contemplar persistencia local mediante Room/SQLite para los datos de solicitudes. | Verificar almacenamiento y recuperación local de los datos definidos para el MVP. |
| RNF06 | Backend | Tecnología | El backend comprometido para el MVP corresponde a Spring Boot. | Verificar que el servicio backend utilizado corresponda a la tecnología definida. |
| RNF07 | Actualización de información | Actualización | La solución debe reflejar los cambios de estado de los viajes en tiempo real, sujeto a la conectividad requerida. | Realizar una prueba de cambio de estado y verificar su reflejo en los clientes correspondientes. |
| RNF08 | Protección de datos | Seguridad / privacidad | El desarrollo debe utilizar datos ficticios, sintéticos, agregados o públicos anonimizados; no se deben incorporar datos reales de clientes, credenciales, tokens ni registros productivos. | Revisar repositorio, datos de prueba y configuración para comprobar que no contienen información real. |
| RNF09 | Minimización de información | Privacidad | La solución debe manejar únicamente la información estrictamente necesaria para resolver el desafío académico. | Revisar los datos almacenados, transmitidos y utilizados por el MVP. |
| RNF10 | Acceso restringido | Seguridad | Los antecedentes y recursos del proyecto deben mantenerse mediante canales institucionales y con acceso restringido según corresponda. | Revisar permisos de acceso y repositorio de documentación/datos. |
| RNF11 | Seguridad desde el diseño | Seguridad | El proyecto debe aplicar controles simulados y registros sin datos reales en el contexto académico. | Revisar demostraciones, registros y configuraciones de prueba. |
| RNF12 | Documentación y control de versiones | Mantenibilidad | El proyecto debe utilizar Git/GitHub para colaboración y documentación, según el alcance tecnológico comprometido. | Verificar repositorio, historial de cambios y documentación técnica. |

## 5. Matriz de trazabilidad

La siguiente matriz relaciona los requisitos funcionales con los casos
de uso definidos anteriormente.

| Requisito | Caso(s) de uso relacionado(s) | Actor principal | Observación |
|---|---|---|---|
| RF01 | CU-01 | ACT-01 | Introduce autenticación como precondición de las funciones del solicitante. |
| RF02 | CU-01 | ACT-01 | Corresponde directamente al registro de la solicitud. |
| RF03 | — | ACT-01 | Nueva funcionalidad respecto de los casos de uso actuales; requiere un caso de uso de consulta. |
| RF04 | CU-04 | ACT-03 | Corresponde a la gestión de solicitudes pendientes. |
| RF05 | CU-04 / CU-05 | ACT-03 | La asignación produce el estado Asignado. |
| RF06 | — | ACT-04 | Nueva funcionalidad de autenticación del conductor; requiere un caso de uso específico. |
| RF07 | CU-02 | ACT-04 | Corresponde a la visualización de viajes asignados. |
| RF08 | CU-02 / CU-03 | ACT-04 | Amplía CU-02 con el detalle del viaje. |
| RF09 | CU-05 | ACT-04 | Define explícitamente las transiciones En Curso y Finalizado. |
| RF10 | CU-03 / CU-06 | ACT-04 / ACT-05 | Formaliza la integración mediante Intents. |
| RF11 | — | Sistema | Introduce persistencia local y sincronización, aspecto técnico que no estaba expresado como caso de uso. |
| RF12 | CU-02 / CU-05 | ACT-03 / ACT-04 | Formaliza la dependencia de conectividad para actualización en tiempo real. |
