--------------------------------------------------------------------------------
-- Belmonte SpA · Traslados corporativos (MVP)
-- Modelo relacional — Oracle Database XE 21c
--
-- Entidades:
--   REGION, COMUNA                                   (geografía)
--   TIPO_USUARIO, USUARIO                             (cuentas de acceso)
--   SOLICITANTE, ADMINISTRADOR, CONDUCTOR             (especialización de USUARIO, un actor cada una)
--   UBICACION                                         (direcciones georreferenciadas)
--   PASAJERO                                          (personas transportadas)
--   ESTADO_SOLICITUD_TRASLADO                         (catálogo de estados)
--   SOLICITUD_TRASLADO                                (entidad central)
--   DETALLE_SOLICITUD_PASAJERO                        (asociativa M:N solicitud↔pasajero)
--
-- No se modela el actor "Sistema de navegación externo" (Google Maps/Waze):
-- es externo, invocado por Intent, y no tiene datos propios que persistir.
--
-- Normalización (ver README > Modelamiento del problema > Modelo relacional):
--   - UBICACION separada de SOLICITUD_TRASLADO: latitud/longitud dependen
--     funcionalmente de la dirección, no de la solicitud (dependencia
--     transitiva, viola 3FN si estuvieran embebidas en la solicitud).
--   - PASAJERO + DETALLE_SOLICITUD_PASAJERO separados de SOLICITUD_TRASLADO:
--     un traslado puede llevar más de un pasajero; una sola columna
--     nombre_pasajero sería un grupo repetitivo (viola 1FN) en cuanto
--     hay más de un pasajero por viaje.
--   - TIPO_USUARIO y ESTADO_SOLICITUD_TRASLADO como catálogo (no CHECK):
--     no es una corrección de forma normal (un CHECK sobre columna atómica
--     no viola ninguna FN), es una mejora de diseño para poder agregar o
--     renombrar valores sin alterar el DDL de las tablas que los usan.
--   - COMUNA/REGION: direccion es UNIQUE en UBICACION (llave candidata), por
--     lo que id_ubicacion → direccion → id_comuna no viola 3FN (3FN solo
--     prohíbe dependencias transitivas vía un atributo que NO sea llave
--     candidata). Es una descomposición normal de jerarquía geográfica
--     (COMUNA pertenece a REGION), no una corrección de anomalía.
--
-- Trazabilidad: cada columna referencia el RF (requerimiento funcional) o el
-- caso de uso del README que la origina.
--
-- Alcance: sin datos catálogo precargados (TIPO_USUARIO, ESTADO_SOLICITUD_
-- TRASLADO, REGION, COMUNA quedan vacías). REGION/COMUNA se diseñan de forma
-- genérica para todo Chile, aunque el script no carga filas.
--
-- Uso: sqlplus usuario/clave@XEPDB1 @modelo_relacional.sql
--      (o ejecutar el contenido completo desde SQL Developer / SQLcl)
--------------------------------------------------------------------------------

--------------------------------------------------------------------------------
-- 1. DROP idempotente, en orden inverso de dependencias
--------------------------------------------------------------------------------

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE detalle_solicitud_pasajero CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE solicitud_traslado CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE estado_solicitud_traslado CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE pasajero CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE conductor CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE administrador CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE solicitante CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE usuario CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE tipo_usuario CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE ubicacion CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE comuna CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE region CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN RAISE; END IF;
END;
/

--------------------------------------------------------------------------------
-- 2. REGION / COMUNA
--    Geografía chilena que ubica cada dirección (RF10). Catálogo genérico
--    para las 16 regiones de Chile; el script no carga filas.
--------------------------------------------------------------------------------

CREATE TABLE region (
   id_region  NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
   nombre     VARCHAR2(60) NOT NULL,
   CONSTRAINT pk_region PRIMARY KEY (id_region),
   CONSTRAINT uk_region_nombre UNIQUE (nombre)
);

COMMENT ON TABLE region IS 'Regiones de Chile (catálogo geográfico, RF10).';

CREATE TABLE comuna (
   id_comuna  NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
   nombre     VARCHAR2(80) NOT NULL,
   id_region  NUMBER       NOT NULL,
   CONSTRAINT pk_comuna PRIMARY KEY (id_comuna),
   CONSTRAINT fk_comuna_region FOREIGN KEY (id_region)
      REFERENCES region (id_region),
   CONSTRAINT uk_comuna_nombre_region UNIQUE (nombre, id_region)
);

COMMENT ON TABLE comuna IS 'Comunas de Chile, agrupadas por región (catálogo geográfico, RF10).';

--------------------------------------------------------------------------------
-- 3. UBICACION
--    Direcciones georreferenciadas de origen/destino (RF10).
--------------------------------------------------------------------------------

CREATE TABLE ubicacion (
   id_ubicacion  NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
   direccion     VARCHAR2(200) NOT NULL,
   id_comuna     NUMBER        NOT NULL,
   latitud       NUMBER(9,6),
   longitud      NUMBER(9,6),
   CONSTRAINT pk_ubicacion PRIMARY KEY (id_ubicacion),
   CONSTRAINT fk_ubicacion_comuna FOREIGN KEY (id_comuna)
      REFERENCES comuna (id_comuna),
   CONSTRAINT uk_ubicacion_direccion UNIQUE (direccion, id_comuna)
);

COMMENT ON TABLE ubicacion IS 'Direcciones georreferenciadas reutilizables como origen o destino (RF10).';

--------------------------------------------------------------------------------
-- 4. TIPO_USUARIO
--    Catálogo de roles con login (RF01, RF06).
--------------------------------------------------------------------------------

CREATE TABLE tipo_usuario (
   id_tipo_usuario  NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
   nombre_tipo      VARCHAR2(20) NOT NULL,
   CONSTRAINT pk_tipo_usuario PRIMARY KEY (id_tipo_usuario),
   CONSTRAINT uk_tipo_usuario_nombre UNIQUE (nombre_tipo)
);

COMMENT ON TABLE tipo_usuario IS 'Catálogo de roles: SOLICITANTE, ADMINISTRADOR, CONDUCTOR (RF01, RF06).';

--------------------------------------------------------------------------------
-- 5. USUARIO
--    Base para los tres roles con inicio de sesión: Cliente corporativo /
--    Solicitante, Recepcionista/Administrador y Conductor (RF01, RF06).
--------------------------------------------------------------------------------

CREATE TABLE usuario (
   id_usuario       NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
   nombre           VARCHAR2(120)  NOT NULL,
   correo           VARCHAR2(150)  NOT NULL,
   telefono         VARCHAR2(20),
   password_hash    VARCHAR2(200)  NOT NULL,
   id_tipo_usuario  NUMBER         NOT NULL,
   fecha_registro   TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
   CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
   CONSTRAINT uk_usuario_correo UNIQUE (correo),
   CONSTRAINT fk_usuario_tipo FOREIGN KEY (id_tipo_usuario)
      REFERENCES tipo_usuario (id_tipo_usuario)
);

COMMENT ON TABLE usuario IS 'Cuentas de acceso de los tres roles con login (RF01, RF06).';
COMMENT ON COLUMN usuario.password_hash IS 'Hash de la contraseña; nunca se almacena en texto plano.';

--------------------------------------------------------------------------------
-- 6. SOLICITANTE / ADMINISTRADOR / CONDUCTOR
--    Especialización 1:1 de USUARIO, una tabla por actor con login.
--------------------------------------------------------------------------------

CREATE TABLE solicitante (
   id_usuario  NUMBER        NOT NULL,
   empresa     VARCHAR2(150) NOT NULL,
   CONSTRAINT pk_solicitante PRIMARY KEY (id_usuario),
   CONSTRAINT fk_solicitante_usuario FOREIGN KEY (id_usuario)
      REFERENCES usuario (id_usuario)
);

COMMENT ON TABLE solicitante IS 'Datos propios del rol Cliente corporativo/Solicitante (RF01).';
COMMENT ON COLUMN solicitante.empresa IS 'Empresa o área que agenda los traslados.';

CREATE TABLE administrador (
   id_usuario  NUMBER NOT NULL,
   CONSTRAINT pk_administrador PRIMARY KEY (id_usuario),
   CONSTRAINT fk_administrador_usuario FOREIGN KEY (id_usuario)
      REFERENCES usuario (id_usuario)
);

COMMENT ON TABLE administrador IS 'Rol Recepcionista/Administrador. Sin atributos propios documentados aún; tabla de especialización para completar el modelo de actores.';

CREATE TABLE conductor (
   id_usuario  NUMBER NOT NULL,
   CONSTRAINT pk_conductor PRIMARY KEY (id_usuario),
   CONSTRAINT fk_conductor_usuario FOREIGN KEY (id_usuario)
      REFERENCES usuario (id_usuario)
);

COMMENT ON TABLE conductor IS 'Rol Conductor. Sin atributos propios documentados aún; permite que solicitud_traslado.id_conductor referencie solo usuarios con este rol.';

--------------------------------------------------------------------------------
-- 7. PASAJERO
--    Personas transportadas (casos de uso 1 y 5). Separado de
--    SOLICITUD_TRASLADO: un traslado puede llevar más de un pasajero.
--------------------------------------------------------------------------------

CREATE TABLE pasajero (
   id_pasajero  NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
   nombre       VARCHAR2(120) NOT NULL,
   CONSTRAINT pk_pasajero PRIMARY KEY (id_pasajero)
);

COMMENT ON TABLE pasajero IS 'Personas transportadas; puede no coincidir con el solicitante (casos de uso 1 y 5).';

--------------------------------------------------------------------------------
-- 8. ESTADO_SOLICITUD_TRASLADO
--    Catálogo del ciclo de vida del traslado (RF05, RF09).
--------------------------------------------------------------------------------

CREATE TABLE estado_solicitud_traslado (
   id_estado      NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
   nombre_estado  VARCHAR2(20) NOT NULL,
   CONSTRAINT pk_estado_solicitud PRIMARY KEY (id_estado),
   CONSTRAINT uk_estado_solicitud_nombre UNIQUE (nombre_estado)
);

COMMENT ON TABLE estado_solicitud_traslado IS 'Catálogo de estados: PENDIENTE, ASIGNADO, EN_CURSO, FINALIZADO (RF05/RF09).';

--------------------------------------------------------------------------------
-- 9. SOLICITUD_TRASLADO
--    Entidad central del modelo: una solicitud de traslado corporativo
--    (RF02–RF10, casos de uso 1, 4 y 5).
--------------------------------------------------------------------------------

CREATE TABLE solicitud_traslado (
   id_solicitud        NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
   id_solicitante      NUMBER         NOT NULL,
   id_conductor        NUMBER,
   fecha_hora_traslado TIMESTAMP      NOT NULL,
   id_origen           NUMBER         NOT NULL,
   id_destino          NUMBER         NOT NULL,
   numero_vuelo        VARCHAR2(20),
   id_estado           NUMBER         NOT NULL,
   fecha_creacion      TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
   fecha_asignacion    TIMESTAMP,
   fecha_inicio        TIMESTAMP,
   fecha_fin           TIMESTAMP,
   CONSTRAINT pk_solicitud_traslado PRIMARY KEY (id_solicitud),
   CONSTRAINT fk_solicitud_solicitante FOREIGN KEY (id_solicitante)
      REFERENCES solicitante (id_usuario),
   CONSTRAINT fk_solicitud_conductor FOREIGN KEY (id_conductor)
      REFERENCES conductor (id_usuario),
   CONSTRAINT fk_solicitud_origen FOREIGN KEY (id_origen)
      REFERENCES ubicacion (id_ubicacion),
   CONSTRAINT fk_solicitud_destino FOREIGN KEY (id_destino)
      REFERENCES ubicacion (id_ubicacion),
   CONSTRAINT fk_solicitud_estado FOREIGN KEY (id_estado)
      REFERENCES estado_solicitud_traslado (id_estado)
);

COMMENT ON TABLE solicitud_traslado IS 'Solicitud de traslado corporativo (RF02-RF10; casos de uso 1, 4 y 5).';
COMMENT ON COLUMN solicitud_traslado.id_conductor IS 'NULL hasta que el Administrador asigna un conductor (RF05).';
COMMENT ON COLUMN solicitud_traslado.numero_vuelo IS 'Opcional; solo aplica a traslados desde/hacia el aeropuerto.';
COMMENT ON COLUMN solicitud_traslado.id_estado IS 'Ciclo de vida del traslado (RF05/RF09); ver estado_solicitud_traslado.';
COMMENT ON COLUMN solicitud_traslado.fecha_asignacion IS 'Momento en que el Administrador asigna un conductor.';
COMMENT ON COLUMN solicitud_traslado.fecha_inicio IS 'Momento en que el Conductor marca el viaje como En Curso.';
COMMENT ON COLUMN solicitud_traslado.fecha_fin IS 'Momento en que el Conductor marca el viaje como Finalizado.';

-- La disponibilidad del conductor no se guarda como columna: se deriva de si
-- tiene alguna solicitud_traslado propia en estado EN_CURSO (evita datos
-- redundantes que se puedan desincronizar del estado real de los viajes).

--------------------------------------------------------------------------------
-- 10. DETALLE_SOLICITUD_PASAJERO
--     Asociativa M:N: un traslado puede llevar varios pasajeros y un
--     pasajero puede aparecer en varios traslados (casos de uso 1 y 5).
--------------------------------------------------------------------------------

CREATE TABLE detalle_solicitud_pasajero (
   id_solicitud  NUMBER NOT NULL,
   id_pasajero   NUMBER NOT NULL,
   CONSTRAINT pk_detalle_sol_pasajero PRIMARY KEY (id_solicitud, id_pasajero),
   CONSTRAINT fk_detalle_solicitud FOREIGN KEY (id_solicitud)
      REFERENCES solicitud_traslado (id_solicitud),
   CONSTRAINT fk_detalle_pasajero FOREIGN KEY (id_pasajero)
      REFERENCES pasajero (id_pasajero)
);

COMMENT ON TABLE detalle_solicitud_pasajero IS 'Detalle M:N entre solicitud_traslado y pasajero: qué pasajeros viajan en cada traslado.';

--------------------------------------------------------------------------------
-- 11. Índices de apoyo a las vistas del README
--------------------------------------------------------------------------------

-- Panel de solicitudes pendientes (RF04)
CREATE INDEX ix_solicitud_estado ON solicitud_traslado (id_estado);

-- Viajes del día del conductor (RF07)
CREATE INDEX ix_solicitud_conductor_fecha ON solicitud_traslado (id_conductor, fecha_hora_traslado);

-- Traslados de un pasajero (búsqueda inversa; la PK compuesta ya cubre id_solicitud)
CREATE INDEX ix_detalle_pasajero ON detalle_solicitud_pasajero (id_pasajero);
