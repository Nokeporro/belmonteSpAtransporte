# Estructura del backend

## Propósito

El backend de Belmonte está organizado como un monolito Spring Boot con separación clásica por capas. Cada capa existe una sola vez y reúne las clases de todas las entidades del sistema. La aplicación expone una API REST JSON y persiste datos en MariaDB mediante Spring Data JPA y migraciones Flyway.

Paquete base Java: `com.belmontespa.transporte`.

## Árbol de carpetas

```text
backend/
├── .gitattributes
├── .gitignore
├── gradle/wrapper/                # Archivos del Gradle Wrapper
├── gradlew
├── gradlew.bat
├── build.gradle                   # Dependencias y configuración Gradle
├── settings.gradle
├── ESTRUCTURA_BACKEND.md          # Este documento
└── src/
    ├── main/
    │   ├── java/com/belmontespa/transporte/
    │   │   ├── TransporteApplication.java
    │   │   ├── client/
    │   │   │   └── FirebaseClient.java
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller/
    │   │   │   ├── CrudController.java
    │   │   │   ├── AdministradorController.java
    │   │   │   ├── BancoController.java
    │   │   │   ├── ComunaController.java
    │   │   │   ├── ConductorController.java
    │   │   │   ├── DetalleMovilConductorController.java
    │   │   │   ├── DetalleViajePasajeroController.java
    │   │   │   ├── EmpresaController.java
    │   │   │   ├── EstadoConductorController.java
    │   │   │   ├── EstadoMovilController.java
    │   │   │   ├── EstadoPagoController.java
    │   │   │   ├── EstadoSolicitudController.java
    │   │   │   ├── FechaHoraController.java
    │   │   │   ├── MedioPagoController.java
    │   │   │   ├── MovilController.java
    │   │   │   ├── PagoController.java
    │   │   │   ├── PasajeroController.java
    │   │   │   ├── RegionController.java
    │   │   │   ├── SolicitanteController.java
    │   │   │   ├── TipoReciboController.java
    │   │   │   ├── UbicacionController.java
    │   │   │   ├── UsuarioController.java
    │   │   │   └── ViajeController.java
    │   │   ├── dto/
    │   │   │   └── response/ApiError.java
    │   │   ├── exception/
    │   │   │   └── ApiExceptionHandler.java
    │   │   ├── model/
    │   │   │   ├── Administrador.java
    │   │   │   ├── Banco.java
    │   │   │   ├── Comuna.java
    │   │   │   ├── Conductor.java
    │   │   │   ├── DetalleMovilConductor.java
    │   │   │   ├── DetalleMovilConductorId.java
    │   │   │   ├── DetalleViajePasajero.java
    │   │   │   ├── DetalleViajePasajeroId.java
    │   │   │   ├── Empresa.java
    │   │   │   ├── EstadoConductor.java
    │   │   │   ├── EstadoMovil.java
    │   │   │   ├── EstadoPago.java
    │   │   │   ├── EstadoSolicitud.java
    │   │   │   ├── FechaHora.java
    │   │   │   ├── MedioPago.java
    │   │   │   ├── Movil.java
    │   │   │   ├── Pago.java
    │   │   │   ├── Pasajero.java
    │   │   │   ├── Region.java
    │   │   │   ├── Solicitante.java
    │   │   │   ├── TipoRecibo.java
    │   │   │   ├── Ubicacion.java
    │   │   │   ├── Usuario.java
    │   │   │   └── Viaje.java
    │   │   ├── repository/
    │   │   │   ├── AdministradorRepository.java
    │   │   │   ├── BancoRepository.java
    │   │   │   ├── ComunaRepository.java
    │   │   │   ├── ConductorRepository.java
    │   │   │   ├── DetalleMovilConductorRepository.java
    │   │   │   ├── DetalleViajePasajeroRepository.java
    │   │   │   ├── EmpresaRepository.java
    │   │   │   ├── EstadoConductorRepository.java
    │   │   │   ├── EstadoMovilRepository.java
    │   │   │   ├── EstadoPagoRepository.java
    │   │   │   ├── EstadoSolicitudRepository.java
    │   │   │   ├── FechaHoraRepository.java
    │   │   │   ├── MedioPagoRepository.java
    │   │   │   ├── MovilRepository.java
    │   │   │   ├── PagoRepository.java
    │   │   │   ├── PasajeroRepository.java
    │   │   │   ├── RegionRepository.java
    │   │   │   ├── SolicitanteRepository.java
    │   │   │   ├── TipoReciboRepository.java
    │   │   │   ├── UbicacionRepository.java
    │   │   │   ├── UsuarioRepository.java
    │   │   │   └── ViajeRepository.java
    │   │   └── service/
    │   │       ├── AdministradorService.java
    │   │       ├── BancoService.java
    │   │       ├── ComunaService.java
    │   │       ├── ConductorService.java
    │   │       ├── CrudService.java
    │   │       ├── DetalleMovilConductorService.java
    │   │       ├── DetalleViajePasajeroService.java
    │   │       ├── EmpresaService.java
    │   │       ├── EstadoConductorService.java
    │   │       ├── EstadoMovilService.java
    │   │       ├── EstadoPagoService.java
    │   │       ├── EstadoSolicitudService.java
    │   │       ├── FechaHoraService.java
    │   │       ├── MedioPagoService.java
    │   │       ├── MovilService.java
    │   │       ├── PagoService.java
    │   │       ├── PasajeroService.java
    │   │       ├── RegionService.java
    │   │       ├── SolicitanteService.java
    │   │       ├── TipoReciboService.java
    │   │       ├── UbicacionService.java
    │   │       ├── UsuarioService.java
    │   │       └── ViajeService.java
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/
    │           ├── V1__crear_esquema_inicial.sql
    │           └── V2__completar_modelo_belmonte.sql
    └── test/java/com/belmontespa/transporte/
        └── TransporteApplicationTests.java
```

> En el árbol anterior, `repository/` y `service/` contienen una clase por entidad, además de sus clases base. Las dos tablas de relación M:N también tienen sus repositorios y servicios propios.

## Responsabilidad de cada capa

| Carpeta | Responsabilidad |
|---|---|
| `model` | Entidades JPA que representan las tablas MariaDB. Las relaciones M:N usan clases ID compuestas. |
| `repository` | Interfaces Spring Data JPA para consultar y persistir entidades. |
| `service` | Operaciones CRUD comunes y lógica de aplicación. `CrudService` concentra las operaciones repetidas. |
| `controller` | Endpoints REST. `CrudController` comparte los endpoints CRUD entre controladores. |
| `dto` | Objetos para contratos de entrada y salida de la API. Actualmente contiene `ApiError`; los CRUD todavía reciben y devuelven modelos. |
| `exception` | Manejo centralizado de excepciones y respuestas de error HTTP. |
| `config` | Configuración transversal de Spring; actualmente contiene la cadena de seguridad. |
| `client` | Contrato para integrar servicios externos; actualmente define la interfaz `FirebaseClient`. |
| `resources/db/migration` | Migraciones SQL versionadas que Flyway aplica sobre MariaDB. |

## CRUD REST

Los controladores CRUD usan el prefijo `/api/v1/` y ofrecen estas operaciones para las entidades con ID simple:

| Método | Ruta | Acción |
|---|---|---|
| `GET` | `/api/v1/{recurso}` | Listar registros |
| `GET` | `/api/v1/{recurso}/{id}` | Consultar un registro |
| `POST` | `/api/v1/{recurso}` | Crear un registro |
| `PUT` | `/api/v1/{recurso}/{id}` | Actualizar un registro; el ID del cuerpo debe coincidir con el de la ruta |
| `DELETE` | `/api/v1/{recurso}/{id}` | Eliminar un registro |

Recursos disponibles: `administradores`, `bancos`, `comunas`, `conductores`, `empresas`, `estados-conductor`, `estados-movil`, `estados-pago`, `estados-solicitud`, `fechas-horas`, `medios-pago`, `moviles`, `pagos`, `pasajeros`, `regiones`, `solicitantes`, `tipos-recibo`, `ubicaciones`, `usuarios` y `viajes`.

Las tablas puente usan dos columnas como clave primaria y tienen rutas específicas:

- `/api/v1/habilitaciones-movil-conductor/{movilId}/{conductorId}`
- `/api/v1/pasajeros-viaje/{viajeId}/{pasajeroId}`

En estas tablas, la asociación se crea y elimina como unidad; no tiene una operación `PUT` porque no posee atributos propios que actualizar.

## Persistencia MariaDB

`application.properties` obtiene conexión desde `DB_URL`, `DB_USER` y `DB_PASSWORD`, con valores locales de desarrollo como respaldo. Hibernate valida el esquema y Flyway administra los cambios:

- `V1__crear_esquema_inicial.sql` crea el esquema MariaDB inicial.
- `V2__completar_modelo_belmonte.sql` amplía el esquema hacia el modelo de Data Modeler, que se usa como referencia funcional y no como DDL Oracle ejecutable.

## Estado y alcance actual

- La estructura sigue el patrón clásico global `Controller → Service → Repository`.
- Los CRUD se implementaron como base genérica; no sustituyen reglas del negocio para asignación, cancelación, transiciones de viaje y pago.
- La seguridad configurada usa HTTP Basic y requiere autenticación. JWT y autorización por rol todavía deben implementarse para cumplir el README.
- `FirebaseClient` es solo una interfaz; aún faltan la implementación/configuración de Firebase y la persistencia del token de dispositivo (RF13).
- Faltan DTOs de petición/respuesta para dejar de exponer directamente las entidades en los CRUD.
- `datamodeler/schema.sql` se conserva como referencia Oracle. La aplicación utiliza las migraciones MariaDB de `src/main/resources/db/migration`.
