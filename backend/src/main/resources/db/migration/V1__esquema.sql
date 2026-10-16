-- Esquema del modelo de README.md ("Modelamiento del problema").
-- Nombres en minúscula, iguales a los @Table/@Column/@JoinColumn de model/
-- (Hibernate valida con ddl-auto=validate). La API no borra filas: las FK quedan
-- en RESTRICT.

-- ---------------------------------------------------------------- geografía

CREATE TABLE region (
  id_region BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre    VARCHAR(60)     NOT NULL,
  PRIMARY KEY (id_region),
  UNIQUE KEY uk_region_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comuna (
  id_comuna BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre    VARCHAR(80)     NOT NULL,
  id_region BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id_comuna),
  UNIQUE KEY uk_comuna_nombre_region (nombre, id_region),
  CONSTRAINT fk_comuna_region FOREIGN KEY (id_region) REFERENCES region (id_region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dirección georreferenciada reutilizable como origen o destino (RF10).
CREATE TABLE ubicacion (
  id_ubicacion BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  direccion    VARCHAR(200)    NOT NULL,
  id_comuna    BIGINT UNSIGNED NOT NULL,
  latitud      DECIMAL(9,6)    NOT NULL,
  longitud     DECIMAL(9,6)    NOT NULL,
  PRIMARY KEY (id_ubicacion),
  UNIQUE KEY uk_ubicacion (direccion, id_comuna, latitud, longitud),
  CONSTRAINT fk_ubicacion_comuna FOREIGN KEY (id_comuna) REFERENCES comuna (id_comuna)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------- catálogos

CREATE TABLE estado_conductor (
  id_estado BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  estado    VARCHAR(20),
  PRIMARY KEY (id_estado),
  UNIQUE KEY uk_estado_conductor (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE estado_movil (
  id_estado_mov BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre        VARCHAR(20)     NOT NULL,
  PRIMARY KEY (id_estado_mov),
  UNIQUE KEY uk_estado_movil (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE estado_solicitud (
  id_estado     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre_estado VARCHAR(20)     NOT NULL,
  PRIMARY KEY (id_estado),
  UNIQUE KEY uk_estado_solicitud (nombre_estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE banco (
  id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre   VARCHAR(50)     NOT NULL,
  cod_sbif INT             NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_banco_cod_sbif (cod_sbif)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE estado_pago (
  id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  estado VARCHAR(20)     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_estado_pago (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medio_pago (
  id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(20)     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_medio_pago (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tipo_recibo (
  id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(20)     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tipo_recibo (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------- usuarios y roles

CREATE TABLE usuario (
  id_usuario               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre                   VARCHAR(120)    NOT NULL,
  correo                   VARCHAR(150)    NOT NULL,
  telefono                 VARCHAR(20),
  password_hash            VARCHAR(200)    NOT NULL,
  -- Con valor, password_hash es una contraseña temporal válida hasta ese instante (RF21).
  password_temporal_expira DATETIME,
  fecha_registro           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- Token push del dispositivo actual; se reemplaza en cada login (RF13).
  token_fcm                VARCHAR(255),
  PRIMARY KEY (id_usuario),
  UNIQUE KEY uk_usuario_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Exclusividad de rol: la llenan los triggers BEFORE INSERT de los tres roles (V2);
-- la PK rechaza un segundo rol para el mismo usuario.
CREATE TABLE usuario_rol_activo (
  id_usuario BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id_usuario),
  CONSTRAINT fk_rol_activo_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE empresa (
  id_empresa   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  razon_social VARCHAR(50)     NOT NULL,
  rut          VARCHAR(15)     NOT NULL,
  PRIMARY KEY (id_empresa),
  UNIQUE KEY uk_empresa_rut (rut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE solicitante (
  id_usuario         BIGINT UNSIGNED NOT NULL,
  empresa_id_empresa BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id_usuario),
  CONSTRAINT fk_solicitante_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  CONSTRAINT fk_solicitante_empresa FOREIGN KEY (empresa_id_empresa) REFERENCES empresa (id_empresa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE administrador (
  id_usuario BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id_usuario),
  CONSTRAINT fk_administrador_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conductor (
  id_usuario                 BIGINT UNSIGNED NOT NULL,
  estado_conductor_id_estado BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id_usuario),
  CONSTRAINT fk_conductor_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
  CONSTRAINT fk_conductor_estado FOREIGN KEY (estado_conductor_id_estado)
    REFERENCES estado_conductor (id_estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------- flota

CREATE TABLE movil (
  id_movil                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  patente                    VARCHAR(6)      NOT NULL,
  gps_serial                 INT,
  estado_movil_id_estado_mov BIGINT UNSIGNED NOT NULL,
  capacidad                  INT             NOT NULL,
  PRIMARY KEY (id_movil),
  UNIQUE KEY uk_movil_patente (patente),
  CONSTRAINT fk_movil_estado FOREIGN KEY (estado_movil_id_estado_mov)
    REFERENCES estado_movil (id_estado_mov)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Habilitación vehículo↔conductor (CU-10).
CREATE TABLE detalle_movil_conductor (
  id_movil   BIGINT UNSIGNED NOT NULL,
  id_usuario BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id_movil, id_usuario),
  CONSTRAINT fk_dmc_movil FOREIGN KEY (id_movil) REFERENCES movil (id_movil),
  CONSTRAINT fk_dmc_conductor FOREIGN KEY (id_usuario) REFERENCES conductor (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------- pasajeros y pagos

CREATE TABLE pasajero (
  id_pasajero BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  -- HMAC Base64url: distingue mayúsculas, por eso collation binaria.
  dni_hash    VARCHAR(50)     NOT NULL COLLATE utf8mb4_bin,
  nombre      VARCHAR(120)    NOT NULL,
  PRIMARY KEY (id_pasajero),
  UNIQUE KEY uk_pasajero_dni_hash (dni_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pago (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hash               VARCHAR(50)     NOT NULL,
  monto              INT             NOT NULL,
  estado_pago_id     BIGINT UNSIGNED NOT NULL,
  banco_id           BIGINT UNSIGNED NOT NULL,
  empresa_id_empresa BIGINT UNSIGNED NOT NULL,
  medio_pago_id      BIGINT UNSIGNED NOT NULL,
  tipo_recibo_id     BIGINT UNSIGNED NOT NULL,
  fecha_pago         DATETIME        NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pago_hash (hash),
  CONSTRAINT fk_pago_estado FOREIGN KEY (estado_pago_id) REFERENCES estado_pago (id),
  CONSTRAINT fk_pago_banco FOREIGN KEY (banco_id) REFERENCES banco (id),
  CONSTRAINT fk_pago_empresa FOREIGN KEY (empresa_id_empresa) REFERENCES empresa (id_empresa),
  CONSTRAINT fk_pago_medio FOREIGN KEY (medio_pago_id) REFERENCES medio_pago (id),
  CONSTRAINT fk_pago_tipo_recibo FOREIGN KEY (tipo_recibo_id) REFERENCES tipo_recibo (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------- viajes

CREATE TABLE viaje (
  id_viaje             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  -- Identificador público e inmutable; lo genera el backend (trigger de respaldo en V2).
  folio                VARCHAR(20)     NOT NULL,
  id_solicitante       BIGINT UNSIGNED NOT NULL,
  id_origen            BIGINT UNSIGNED NOT NULL,
  id_destino           BIGINT UNSIGNED NOT NULL,
  id_estado            BIGINT UNSIGNED NOT NULL,
  movil_id_movil       BIGINT UNSIGNED,
  pago_id              BIGINT UNSIGNED,
  -- Mantenido por trigger desde detalle_viaje_pasajero; nunca lo escribe el cliente.
  cantidad_pasajeros   INT             NOT NULL DEFAULT 0,
  fh_creacion          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fh_agenda            DATETIME        NOT NULL,
  fh_asignacion        DATETIME,
  fh_inicio            DATETIME,
  fh_fin               DATETIME,
  conductor_id_usuario BIGINT UNSIGNED,
  numero_vuelo         VARCHAR(20),
  PRIMARY KEY (id_viaje),
  UNIQUE KEY uk_viaje_folio (folio),
  KEY ix_viaje_solicitante_agenda (id_solicitante, fh_agenda),
  KEY ix_viaje_conductor_agenda (conductor_id_usuario, fh_agenda),
  KEY ix_viaje_estado_agenda (id_estado, fh_agenda),
  CONSTRAINT fk_viaje_solicitante FOREIGN KEY (id_solicitante) REFERENCES solicitante (id_usuario),
  CONSTRAINT fk_viaje_origen FOREIGN KEY (id_origen) REFERENCES ubicacion (id_ubicacion),
  CONSTRAINT fk_viaje_destino FOREIGN KEY (id_destino) REFERENCES ubicacion (id_ubicacion),
  CONSTRAINT fk_viaje_estado FOREIGN KEY (id_estado) REFERENCES estado_solicitud (id_estado),
  CONSTRAINT fk_viaje_movil FOREIGN KEY (movil_id_movil) REFERENCES movil (id_movil),
  CONSTRAINT fk_viaje_pago FOREIGN KEY (pago_id) REFERENCES pago (id),
  CONSTRAINT fk_viaje_conductor FOREIGN KEY (conductor_id_usuario) REFERENCES conductor (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE detalle_viaje_pasajero (
  id_viaje    BIGINT UNSIGNED NOT NULL,
  id_pasajero BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id_viaje, id_pasajero),
  CONSTRAINT fk_dvp_viaje FOREIGN KEY (id_viaje) REFERENCES viaje (id_viaje),
  CONSTRAINT fk_dvp_pasajero FOREIGN KEY (id_pasajero) REFERENCES pasajero (id_pasajero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Una fila por campo de VIAJE que cambia en cada UPDATE; la escriben los triggers (V2).
CREATE TABLE historial_viaje (
  id_historial     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_viaje         BIGINT UNSIGNED NOT NULL,
  campo_modificado VARCHAR(30)     NOT NULL,
  valor            VARCHAR(200)    NOT NULL,
  fecha_cambio     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_historial),
  KEY ix_historial_viaje (id_viaje, fecha_cambio),
  CONSTRAINT fk_historial_viaje FOREIGN KEY (id_viaje) REFERENCES viaje (id_viaje)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
