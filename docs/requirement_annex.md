# Duoc UC | ESCUELA DE INFORMÁTICA Y TELECOMUNICACIONES | CITT
## ANEXO DE REQUERIMIENTO
**Desarrollo de Aplicaciones Móviles (DSY1105)**

| Campo | Detalle |
| :--- | :--- |
| **Asignatura** | DSY1105 Desarrollo de Aplicaciones Móviles |
| **Propósito** | Levantamiento de necesidad real para desarrollo de Producto Mínimo Viable (MVP) móvil. |

> **Instrucción de uso:** Este anexo debe ser completado por la organización
> con apoyo del Coordinador o Coordinadora CITT. Su finalidad es entregar un
> contexto real y suficiente, sin definir de antemano la solución técnica ni
> compartir información innecesaria, confidencial o datos personales reales.

---

## 1. Identificación de la organización y contraparte

| Antecedentes | Información solicitada |
| :--- | :--- |
| **Nombre de la organización:** | Belmonte SpA |
| **Rubro o actividad principal:** | Servicios de transporte de pasajeros / Traslados corporativos |
| **Nombre y cargo de la contraparte:** | Rosalba Cantillo - Administradora / Representante Legal |
| **Correo institucional y teléfono:** | Belmonte01Spa@gmail.com / +569-99405434 |
| **Coordinador/a CITT responsable:** | Freddy Campos Orellana |
| **Fecha de levantamiento:** | 09 de Septiembre de 2026 |
| **Sede:** | Padre Alonso Ovalle |

* **Uso de datos de contacto:** Los datos de la contraparte se utilizarán
  exclusivamente para coordinar los hitos del programa, con acceso limitado a
  quienes intervienen en la gestión académica y de vinculación.

---

## 2. Contexto y necesidad real

### 2.1 Presentación breve de la organización Empresa dedicada al rubro del
transporte privado corporativo, orientada a movilizar pasajeros que requieren
traslados estratégicos, ejecutivos y traslados hacia/desde aeropuertos.

### 2.2 Proceso o situación actual que se desea mejorar Toda la gestión
operativa actual es completamente manual. Las solicitudes de servicios se
coordinan de forma dispersa mediante llamadas telefónicas, correos electrónicos
y mensajería de WhatsApp, donde se transmiten de manera informal datos
logísticos como direcciones, números de vuelo, nombres de pasajeros, fechas y
horarios.

### 2.3 Problema o necesidad concreta La dispersión de la información genera
ineficiencias logísticas, lentitud en los tiempos de respuesta y riesgos de
errores en el procesamiento de datos críticos (como variaciones de horarios o
direcciones incorrectas). Afecta directamente la agilidad con la que los
conductores reciben sus rutas y ralentiza la confirmación de cara al cliente.

### 2.4 Actores involucrados
* **Solicitantes / Pasajeros:** Quienes piden el servicio corporativo.
* **Recepcionista / Administrador:** Encargado de tomar y distribuir las
  solicitudes.
* **Conductores:** Quienes ejecutan el transporte en terreno.

### 2.5 Resultado esperado desde la perspectiva del negocio Automatizar y
centralizar operativamente el flujo completo de traslados en una plataforma
unificada. Se busca lograr máxima agilidad en la captura de información y una
integración que enlace de forma directa las coordenadas geográficas con
aplicaciones de navegación externa.

### 2.6 Criterios de valor o éxito Efectividad y agilidad radical en la
confirmación de solicitudes de traslado, eliminación de la pérdida de
información por canales informales y disminución del error manual al despachar
rutas hacia los choferes.

---

## 3. Requerimientos específicos de la asignatura

### 3.1 Funciones o tareas que realizan actualmente los perfiles usuarios
* **Pasajero / Solicitante:** Podrá ingresar digitalmente un formulario de
  traslado detallando fecha, hora, punto de origen, destino y número de vuelo
  (si aplica).
* **Conductor:** Podrá visualizar sus viajes asignados para el día en tiempo
  real y presionar un botón para mapear la ruta directamente en navegadores
  externos.

### 3.2 Información necesaria para el proceso **Estructura de datos sintética
del servicio:**
* Identificador del viaje
* Nombre del pasajero
* Fecha
* Hora
* Dirección de Origen (Georreferenciada)
* Dirección de Destino (Georreferenciada)
* Estado del traslado (`Pendiente`, `Asignado`, `En Curso`, `Finalizado`)

### 3.3 Condiciones de uso de la aplicación móvil Uso intensivo en terreno por
parte de conductores a través de smartphones Android. Requiere de manera
mandatoria conectividad a internet continua (API REST) para actualizaciones en
tiempo real y el uso de *Intents* del sistema operativo para levantar de forma
nativa apps de navegación satelital.

### 3.4 Conexión con otros sistemas o herramientas Necesidad imperativa de
enlazar la data de la aplicación móvil de manera externa con **Google Maps**
y/o **Waze** para la navegación y trazado de rutas óptimas de tráfico. El
backend (**Spring Boot**) procesará la asignación central de los estados del
viaje.

### Alcance tecnológico comprometido (MVP)

| Componente / El MVP podrá considerar | Tecnologías del programa de la asignatura |
| :--- | :--- |
| **Interfaz móvil funcional, formularios, navegación y gestión de estado** | Kotlin, Android Studio, Jetpack Compose, Material Design 3, MVVM |
| **Persistencia local y recursos nativos** | Room/SQLite y recursos nativos del dispositivo móvil |
| **Integración front-end/back-end y pruebas** | API REST, microservicios Spring Boot, Retrofit, pruebas unitarias |
| **Colaboración, documentación y ejecutable** | Git/GitHub, Trello y APK firmado en modo release |

> **Alcance comprometido:** El entregable es una propuesta de solución
> tecnológica materializada en un MVP. No corresponde al desarrollo definitivo
> ni a la publicación productiva de una aplicación.

---

## 4. Protección de datos personales y seguridad de la información

> **Regla general obligatoria:** El desafío se desarrollará con datos
> ficticios, sintéticos, agregados, o públicos debidamente anonimizados. No se
> deben entregar datos reales, listados de personas, credenciales, llaves,
> tokens, respaldos, registros productivos ni accesos a sistemas internos.
> Queda estrictamente prohibido ingresar bases de datos de clientes reales o
> registros de mensajería vigentes de la empresa.

### 4.1 Clasificación previa de la información
* La información compartida es pública o fue autorizada institucionalmente para
  este uso académico.
* La información compartida para el desarrollo del caso no contiene nombres,
  RUT, correos personales, teléfonos, domicilios u otros identificadores
  reales.
* Los ejemplos, registros y bases de prueba serán estrictamente ficticios o
  sintéticos generados para el entorno de pruebas académico.
* Los permisos del dispositivo (ubicación, navegación) se probarán sin capturar
  datos reales de personas en entornos de producción.

### 4.2 Consideraciones importantes
* **Finalidad y minimización:** Entregar únicamente la información
  estrictamente necesaria para resolver académicamente el desafío.
* **Acceso restringido:** Los antecedentes serán compartidos solo mediante
  canales institucionales con fines académicos.
* **Seguridad desde el diseño:** Se aplicarán controles simulados y registros
  sin datos reales.
* **Publicación:** Se usarán datos ficticios en presentaciones, videos,
  capturas y demostraciones finales.

---

## 5. Participación del socio formador

| Hito | Participación esperada | Duración |
| :--- | :--- | :--- |
| **Inicio** | Presentar el contexto operativo del transporte y resolver dudas iniciales de los alumnos respecto a los flujos de traslados. | 45 min, online |
| **Seguimiento** | Revisar los primeros wireframes o prototipos móviles en Kotlin y entregar retroalimentación general sobre el diseño de pantallas. | 45 min, online |
| **Cierre** | Observar la demostración final del MVP móvil interactuando con las llamadas a mapas y responder la encuesta académica. | Según coord. |

* **Rol académico:** La organización aporta contexto, resuelve dudas y
  retroalimenta, pero no califica a las y los estudiantes ni define las
  exigencias curriculares.

---

## 6. Declaración, validación y referencias

* La necesidad de automatización de traslados descrita es real, vigente y
  perfectamente abordable de manera semestral por los alumnos.
* La organización Belmonte SpA comprende que diferentes grupos estudiantiles
  pueden proponer alternativas o flujos de interfaces diferentes.
* Se acepta formalmente que el entregable final corresponde a un Producto
  Mínimo Viable (MVP) de carácter estrictamente educativo y no un software
  final listo para producción comercial.

### Referencias normativas:
* **Ley N.º 21.719:** Regula la protección y tratamiento de datos personales y
  crea la Agencia de Protección de Datos Personales (Vigencia 1 de diciembre de
  2026).
* **Ley N.º 19.628:** Sobre la protección de la vida privada, según texto
  vigente y sus modificaciones.
* Políticas, procedimientos y estándares institucionales de Duoc UC aplicables
  a privacidad y seguridad.
