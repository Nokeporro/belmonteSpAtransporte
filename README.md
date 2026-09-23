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
### Base de datos
### Backend
### Frontend

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
