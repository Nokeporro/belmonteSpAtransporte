# BelmonteSpAtransporte

**Ultima modificacion:** 22-09-2026

## Integrantes:
* Daniel Muñoz
* Michel Sanhueza

## Indice

## Introduccion

Belmonte SpA es una empresa del rubro de transporte privado corporativo, enfocada en traslados ejecutivos y trayectos hacia/desde aeropuertos. Actualmente toda su gestión operativa es manual: las solicitudes de viaje se coordinan de forma dispersa por llamadas telefónicas, correos electrónicos y WhatsApp, transmitiendo de manera informal datos como direcciones, números de vuelo, nombres de pasajeros, fechas y horarios.

Este proyecto busca desarrollar un Producto Mínimo Viable (MVP) de aplicación móvil que automatice y centralice el flujo completo de traslados en una plataforma unificada, integrando la captura de solicitudes, la asignación de viajes a conductores y el enlace directo con aplicaciones de navegación externa (Google Maps / Waze) para el trazado de rutas.

### Descripcion de la problematica

La dispersión de la información en múltiples canales informales genera:
- Ineficiencias logísticas y lentitud en los tiempos de respuesta.
- Riesgo de errores en datos críticos (horarios mal informados, direcciones incorrectas).
- Retrasos en que los conductores reciban sus rutas asignadas.
- Lentitud en la confirmación del servicio de cara al cliente.

El objetivo del proyecto es eliminar la pérdida de información por canales informales, lograr una confirmación de solicitudes ágil y reducir el error manual al despachar rutas a los choferes.

### Actores involucrados

- **Cliente corporativo / Solicitante:** Empresa o persona (ej. asistente, área de RRHH) que agenda el traslado ingresando el formulario con los datos del viaje (fecha, hora, origen, destino, número de vuelo si aplica) y los datos del pasajero.
- **Pasajero:** Persona que efectivamente realiza el traslado; puede coincidir o no con quien lo solicitó.
- **Recepcionista / Administrador:** Encargado de tomar y distribuir las solicitudes de traslado entre los conductores disponibles.
- **Conductor:** Ejecuta el transporte en terreno; visualiza en tiempo real los viajes que tiene asignados para el día y utiliza la app para trazar la ruta en navegadores externos.
- **Sistema de navegación externo (Google Maps / Waze):** Actor externo que recibe las coordenadas georreferenciadas de origen y destino, y genera el trazado de ruta óptima al ser invocado por el conductor mediante Intents del sistema operativo.

### Casos de uso

1. **Registrar solicitud de traslado** (Cliente corporativo/Solicitante): completar un formulario digital con los datos del pasajero, fecha, hora, punto de origen, punto de destino y número de vuelo (si aplica).
2. **Visualizar viajes asignados** (Conductor): ver en tiempo real la lista de viajes asignados para el día.
3. **Navegar a la ruta del viaje** (Conductor): presionar un botón que levante, mediante Intents del sistema operativo, una app externa de navegación (Google Maps / Waze) con el origen y destino georreferenciados.
4. **Gestionar y distribuir solicitudes** (Recepcionista/Administrador): recibir las solicitudes ingresadas y asignarlas a un conductor.
5. **Actualizar estado del traslado** (Recepcionista/Administrador asigna Pendiente → Asignado; Conductor marca En Curso y Finalizado desde terreno): reflejar el ciclo de vida del viaje según los estados Pendiente, Asignado, En Curso y Finalizado.
6. **Trazar ruta óptima hacia el destino** (Sistema de navegación externo): a partir de las coordenadas georreferenciadas de origen y destino recibidas al ser invocado por el conductor, calcular y mostrar la ruta óptima de tráfico.

## Requerimientos funcionales

- **RF01:** El sistema debe permitir al Cliente corporativo/Solicitante registrarse y autenticarse en la aplicación.
- **RF02:** El sistema debe permitir al Cliente corporativo/Solicitante registrar una solicitud de traslado indicando datos del pasajero, fecha, hora, dirección de origen, dirección de destino y número de vuelo (si aplica).
- **RF03:** El sistema debe permitir al Cliente corporativo/Solicitante visualizar el historial y estado de sus solicitudes de traslado.
- **RF04:** El sistema debe permitir al Recepcionista/Administrador visualizar las solicitudes pendientes de asignación.
- **RF05:** El sistema debe permitir al Recepcionista/Administrador asignar un conductor disponible a una solicitud de traslado, actualizando su estado a "Asignado".
- **RF06:** El sistema debe permitir al Conductor autenticarse mediante credenciales asignadas por el Administrador, sin permitir autorregistro.
- **RF07:** El sistema debe permitir al Conductor visualizar en tiempo real los viajes asignados para el día.
- **RF08:** El sistema debe permitir al Conductor visualizar el detalle de un viaje asignado (datos del pasajero, dirección de origen y de destino).
- **RF09:** El sistema debe permitir al Conductor actualizar el estado de un traslado a "En Curso" y a "Finalizado".
- **RF10:** El sistema debe integrar, mediante Intents del sistema operativo Android, una aplicación externa de navegación (Google Maps/Waze) para trazar la ruta hacia el destino a partir de las coordenadas georreferenciadas.
- **RF11:** El sistema debe persistir localmente (Room/SQLite) los datos de las solicitudes y sincronizarlos mediante una API REST con el backend.
- **RF12:** El sistema debe requerir conectividad a internet continua para reflejar en tiempo real las actualizaciones de estado de los viajes.

## Historias de usuario
## Modelamiento del problema
### Modelo logico
### Modelo relacional

## Arquitectura
**Cliente-Servidor Desacoplada y Arquitectura en Capas**

El proyecto implementa una **Arquitectura Cliente-Servidor Desacoplada**, respaldada por un patrón de **Arquitectura en Capas (N-Tier)** en el servidor backend:

* **Cliente-Servidor Desacoplado (API REST):**
  * La aplicación móvil (Android) actúa como la capa de presentación independiente y se comunica con el servidor exclusivamente a través de servicios web **RESTful** mediante peticiones HTTP/JSON.
  * **Ventaja:** Desacopla la interfaz de usuario de la lógica de negocio. Permite actualizar, rediseñar o escalar la app móvil sin alterar el backend ni la base de datos.

* **Arquitectura en Capas en el Backend (Layered Architecture):**
  El código de Spring Boot se estructura bajo la separación estricta de responsabilidades en tres capas:
  1. **Capa de Controladores (`Controller` / API REST):** Gestiona la recepción de peticiones HTTP del dispositivo móvil, valida las entradas y devuelve respuestas estandarizadas en JSON.
  2. **Capa de Servicios (`Service` / Lógica de Negocio):** Contiene las reglas del negocio de transporte (procesamiento de estados de viaje, asignación de choferes y lógica de transporte).
  3. **Capa de Persistencia (`Repository` / Acceso a Datos):** Administra las transacciones y consultas a MariaDB mediante Spring Data JPA/Hibernate.
  * **Ventaja:** Facilita la legibilidad, el mantenimiento, las pruebas del sistema y presenta una estructura profesional para la evaluación del proyecto.

* **Modelo Stateless (Sin Estado) con JWT:**
  * La comunicación entre el cliente móvil y el backend es totalmente *stateless*. El servidor no almacena sesiones activas en memoria; cada petición valida la identidad del usuario mediante un token **JWT (JSON Web Token)**.
  * **Ventaja:** Maximiza la eficiencia del servidor y asegura un manejo de sesiones confiable para dispositivos móviles que operan sobre redes móviles variables.
    
### Lenguaje de Programación

#### Backend: Java
Se definió **Java** como el lenguaje para el servidor backend con **Spring Boot** por los siguientes motivos técnicos:

* **Estándar Empresarial y Madurez:** Es el lenguaje nativo por excelencia del ecosistema Spring Boot, contando con soporte absoluto, documentación exhaustiva y un ecosistema de librerías altamente consolidadas.
* **Tipado Estático y Robustez:** Al ser un lenguaje fuertemente tipado, permite detectar errores de incompatibilidad de datos durante la compilación en lugar de la ejecución, garantizando la estabilidad en el procesamiento transaccional de viajes.
* **Modelado del Dominio (POO):** Facilita la abstracción limpia de las entidades operativas de transporte mediante Programación Orientada a Objetos, encajando de manera directa en la arquitectura por capas (`Controller` ➔ `Service` ➔ `Repository`).
* **Portabilidad y Rendimiento:** La JVM asegura una alta eficiencia al procesar peticiones HTTP/REST concurrentes y permite desplegar el servidor en cualquier entorno o contenedor de forma transparente.

####  Frontend Móvil: Kotlin
Se adoptó **Kotlin** para la aplicación nativa en **Android Studio** debido a sus ventajas fundamentales para el desarrollo móvil moderno:

* **Estándar Oficial "Android-First":** Es el lenguaje prioritario y oficialmente recomendado por Google para el desarrollo nativo en Android, lo que garantiza el mejor soporte en librerías, APIs del sistema operativo y herramientas del IDE.
* **Seguridad de Nulos (Null Safety):** Incorpora un sistema de tipos que previene en tiempo de compilación las caídas de la app por punteros nulos (*NullPointerExceptions*), la causa principal de fallos en dispositivos móviles.
* **Manejo de Asincronía con Corrutinas (Coroutines):** Permite realizar llamadas a la API REST (vía Retrofit) y consultas locales (vía Room) en segundo plano de forma no bloqueante, manteniendo la interfaz de usuario rápida y fluida durante el trayecto.
* **Sintaxis Concisa y Menor Código Repetitivo:** Características como las *Data Classes*, funciones de extensión y mapeo sintáctico reducen significativamente la cantidad de código *boilerplate* frente a Java nativo en Android, acelerando el desarrollo del MVP.
* **Interoperabilidad 100% con la JVM:** Al compilar sobre la JVM/Android Runtime, comparte exactamente el mismo modelo de datos JSON que retorna el backend en Java, garantizando una integración limpia entre ambas capas.
  
### Base de datos
**MariaDB**

* **Integridad Transaccional (ACID):** Utiliza el motor de almacenamiento `InnoDB`, garantizando la consistencia y atomicidad de los datos en procesos críticos de logística (como la asignación de vehículos, registro de mantenimientos y estados de viaje).
* **Compatibilidad Total con el Ecosistema Spring:** Cuenta con dialectos oficiales en Hibernate (`MariaDBDialect`) y un driver JDBC oficial (`mariadb-java-client`) ligero y eficiente, permitiendo la generación y migración automática de tablas desde Java.
* **Filosofía Open Source:** Es un sistema de gestión de bases de datos 100% libre y comunitario (Licencia GPL v2), asegurando estabilidad sin depender de licencias comerciales.
* **Eficiencia de Recursos en Desarrollo:** Presenta un consumo optimizado de memoria y CPU en entornos de desarrollo local, permitiendo ejecutar simultáneamente el entorno de Android Studio, la base de datos y la API de Spring Boot sin sobrecargar el equipo.
### Backend
**Spring Boot** como el framework principal para la capa del backend debido a las siguientes ventajas técnicas:

* **Arquitectura REST Nativa para Clientes Móviles:** Permite estructurar y exponer endpoints HTTP en formato JSON estandarizado. Esto facilita la integración transparente con la aplicación móvil Android utilizando librerías de red como Retrofit o Volley.
* **Seguridad Stateless mediante JWT:** A través de **Spring Security**, la aplicación implementa autenticación basada en tokens **JWT (JSON Web Tokens)**. Al ser una API sin estado (*stateless*), es el enfoque óptimo para dispositivos móviles, permitiendo un control de acceso seguro y basado en roles (ej. Administrador, Chofer).
* **Diseño Modular y Separación de Capas:** Impone un patrón de diseño claro (`Controller` ➔ `Service` ➔ `Repository`), lo que garantiza un código limpio, mantenible, fácil de auditar y preparado para futuras ampliaciones de lógica de negocio.
* **Productividad con Spring Data JPA:** Automatiza la persistencia de datos mediante el mapeo objeto-relacional (ORM), eliminando la necesidad de escribir código SQL manual redundante para las operaciones estándar (CRUD).
### Frontend

![Wireframes de baja fidelidad](mockups/wireframes-belmonte.svg)

Vistas propuestas para el MVP, agrupadas por actor:

**Transversal**
- **Login:** autenticación para los tres actores con app móvil (Cliente corporativo/Solicitante, Recepcionista/Administrador, Conductor).
- **Perfil / Cierre de sesión:** datos del usuario logueado.

**Cliente corporativo / Solicitante**
- **Registro:** creación de cuenta propia. El Conductor no se autorregistra: sus credenciales son asignadas por el Administrador, por lo que solo usa la vista de Login.
- **Registrar solicitud de traslado:** formulario con datos del pasajero, fecha, hora, origen, destino y n.º de vuelo (si aplica). Cubre el caso de uso 1.
- **Mis solicitudes (historial):** lista de traslados agendados con su estado actual (Pendiente/Asignado/En Curso/Finalizado).
- **Detalle de solicitud:** vista de una solicitud puntual con los datos ingresados, estado, y datos del conductor una vez asignado.

**Recepcionista / Administrador**
- **Panel de solicitudes pendientes:** listado de solicitudes sin asignar. Entrada al caso de uso 4.
- **Asignar conductor:** vista para elegir un conductor disponible para una solicitud puntual. Cubre el caso de uso 4.
- **Panel general / dashboard** *(segunda iteración)*: resumen operativo con conteo de viajes por estado.

**Conductor**
- **Viajes del día:** lista de viajes asignados para la fecha actual, ordenados por hora. Cubre el caso de uso 2.
- **Detalle de viaje asignado:** datos del pasajero, origen, destino, control para marcar "En Curso"/"Finalizado" y botón que dispara el Intent hacia Google Maps/Waze. Cubre los casos de uso 5 y 6.
- **Historial de viajes realizados** *(segunda iteración)*: viajes ya finalizados por el conductor.
