-- Completa el modelo de Data Modeler sobre la base MariaDB de V1.
-- Se conserva V1 para respetar el historial Flyway y se adapta su esquema inicial.

CREATE TABLE empresa (
  id_empresa BIGINT NOT NULL AUTO_INCREMENT,
  razon_social VARCHAR(50) NOT NULL,
  rut VARCHAR(15) NOT NULL,
  PRIMARY KEY (id_empresa),
  UNIQUE KEY uk_empresa_razon_rut (razon_social, rut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO empresa (razon_social, rut)
SELECT DISTINCT LEFT(empresa, 50), CONCAT('MIG-', LEFT(SHA2(CAST(id_usuario AS CHAR), 256), 11))
FROM solicitante;

ALTER TABLE solicitante ADD COLUMN EMPRESA_id_empresa BIGINT NULL;
UPDATE solicitante s
JOIN empresa e ON e.rut = CONCAT('MIG-', LEFT(SHA2(CAST(s.id_usuario AS CHAR), 256), 11))
SET s.EMPRESA_id_empresa = e.id_empresa;
ALTER TABLE solicitante MODIFY EMPRESA_id_empresa BIGINT NOT NULL;
ALTER TABLE solicitante DROP COLUMN empresa;
ALTER TABLE solicitante ADD CONSTRAINT fk_solicitante_empresa
  FOREIGN KEY (EMPRESA_id_empresa) REFERENCES empresa(id_empresa);

ALTER TABLE usuario DROP FOREIGN KEY fk_usuario_tipo;
ALTER TABLE usuario DROP COLUMN id_tipo_usuario;

ALTER TABLE pasajero ADD COLUMN rut_hash VARCHAR(50) NULL;
UPDATE pasajero SET rut_hash = SHA2(CONCAT('PASAJERO-', id_pasajero), 256);
ALTER TABLE pasajero MODIFY rut_hash VARCHAR(50) NOT NULL;
ALTER TABLE pasajero ADD CONSTRAINT uk_pasajero_rut_hash UNIQUE (rut_hash);

CREATE TABLE estado_conductor (
  id_estado BIGINT NOT NULL AUTO_INCREMENT,
  estado VARCHAR(20),
  PRIMARY KEY (id_estado), UNIQUE KEY uk_estado_conductor (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO estado_conductor (estado) VALUES ('DISPONIBLE'), ('OCUPADO');

ALTER TABLE conductor ADD COLUMN ESTADO_CONDUCTOR_id_estado BIGINT NULL;
UPDATE conductor SET ESTADO_CONDUCTOR_id_estado = (SELECT id_estado FROM estado_conductor WHERE estado = 'DISPONIBLE' LIMIT 1);
ALTER TABLE conductor MODIFY ESTADO_CONDUCTOR_id_estado BIGINT NOT NULL;
ALTER TABLE conductor ADD CONSTRAINT fk_conductor_estado
  FOREIGN KEY (ESTADO_CONDUCTOR_id_estado) REFERENCES estado_conductor(id_estado);

CREATE TABLE estado_movil (
  id_estado_mov BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(20) NOT NULL,
  PRIMARY KEY (id_estado_mov), UNIQUE KEY uk_estado_movil (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO estado_movil (nombre) VALUES ('DISPONIBLE'), ('EN_USO'), ('MANTENCION');

CREATE TABLE estado_pago (
  id BIGINT NOT NULL AUTO_INCREMENT,
  estado VARCHAR(20) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_estado_pago (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO estado_pago (estado) VALUES ('PENDIENTE'), ('PAGADO'), ('RECHAZADO'), ('ANULADO');

CREATE TABLE estado_solicitud (
  id_estado BIGINT NOT NULL AUTO_INCREMENT,
  nombre_estado VARCHAR(20) NOT NULL,
  PRIMARY KEY (id_estado), UNIQUE KEY uk_estado_solicitud_nombre (nombre_estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO estado_solicitud (nombre_estado) VALUES ('PENDIENTE'), ('ASIGNADO'), ('EN_CURSO'), ('FINALIZADO'), ('CANCELADO');

CREATE TABLE banco (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(50) NOT NULL,
  cod_sbif INT NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_banco_nombre_codigo (nombre, cod_sbif)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medio_pago (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(20) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_medio_pago_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tipo_recibo (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(20) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_tipo_recibo_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fecha_hora (
  id BIGINT NOT NULL AUTO_INCREMENT,
  data DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE movil (
  id_movil BIGINT NOT NULL AUTO_INCREMENT,
  patente VARCHAR(6) NOT NULL,
  gps_serial BIGINT NULL,
  ESTADO_MOVIL_id_estado_mov BIGINT NOT NULL,
  capacidad INT NOT NULL,
  PRIMARY KEY (id_movil), UNIQUE KEY uk_movil_patente (patente),
  CONSTRAINT fk_movil_estado FOREIGN KEY (ESTADO_MOVIL_id_estado_mov) REFERENCES estado_movil(id_estado_mov)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pago (
  id BIGINT NOT NULL AUTO_INCREMENT,
  hash VARCHAR(50) NOT NULL,
  monto DECIMAL(12,2) NOT NULL,
  ESTADO_PAGO_id BIGINT NOT NULL,
  BANCO_id BIGINT NOT NULL,
  EMPRESA_id_empresa BIGINT NOT NULL,
  MEDIO_PAGO_id BIGINT NOT NULL,
  TIPO_RECIBO_nombre VARCHAR(20) NOT NULL,
  FECHA_HORA_id BIGINT NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_pago_hash (hash),
  CONSTRAINT fk_pago_estado FOREIGN KEY (ESTADO_PAGO_id) REFERENCES estado_pago(id),
  CONSTRAINT fk_pago_banco FOREIGN KEY (BANCO_id) REFERENCES banco(id),
  CONSTRAINT fk_pago_empresa FOREIGN KEY (EMPRESA_id_empresa) REFERENCES empresa(id_empresa),
  CONSTRAINT fk_pago_medio FOREIGN KEY (MEDIO_PAGO_id) REFERENCES medio_pago(id),
  CONSTRAINT fk_pago_tipo_recibo FOREIGN KEY (TIPO_RECIBO_nombre) REFERENCES tipo_recibo(nombre),
  CONSTRAINT fk_pago_fecha_hora FOREIGN KEY (FECHA_HORA_id) REFERENCES fecha_hora(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE viaje (
  id_viaje BIGINT NOT NULL AUTO_INCREMENT,
  id_solicitante BIGINT NOT NULL,
  id_origen BIGINT NOT NULL,
  id_destino BIGINT NOT NULL,
  id_estado BIGINT NOT NULL,
  MOVIL_id_movil BIGINT NULL,
  PAGO_id BIGINT NULL,
  FH_creacion BIGINT NOT NULL,
  FH_agenda BIGINT NOT NULL,
  FH_asignacion BIGINT NULL,
  FH_inicio BIGINT NULL,
  FH_fin BIGINT NULL,
  CONDUCTOR_id_usuario BIGINT NULL,
  numero_vuelo VARCHAR(20) NULL,
  PRIMARY KEY (id_viaje), KEY ix_viaje_estado (id_estado), KEY ix_viaje_conductor (CONDUCTOR_id_usuario),
  CONSTRAINT fk_viaje_solicitante FOREIGN KEY (id_solicitante) REFERENCES solicitante(id_usuario),
  CONSTRAINT fk_viaje_origen FOREIGN KEY (id_origen) REFERENCES ubicacion(id_ubicacion),
  CONSTRAINT fk_viaje_destino FOREIGN KEY (id_destino) REFERENCES ubicacion(id_ubicacion),
  CONSTRAINT fk_viaje_estado FOREIGN KEY (id_estado) REFERENCES estado_solicitud(id_estado),
  CONSTRAINT fk_viaje_movil FOREIGN KEY (MOVIL_id_movil) REFERENCES movil(id_movil),
  CONSTRAINT fk_viaje_pago FOREIGN KEY (PAGO_id) REFERENCES pago(id),
  CONSTRAINT fk_viaje_conductor FOREIGN KEY (CONDUCTOR_id_usuario) REFERENCES conductor(id_usuario),
  CONSTRAINT fk_viaje_fecha_creacion FOREIGN KEY (FH_creacion) REFERENCES fecha_hora(id),
  CONSTRAINT fk_viaje_fecha_agenda FOREIGN KEY (FH_agenda) REFERENCES fecha_hora(id),
  CONSTRAINT fk_viaje_fecha_asignacion FOREIGN KEY (FH_asignacion) REFERENCES fecha_hora(id),
  CONSTRAINT fk_viaje_fecha_inicio FOREIGN KEY (FH_inicio) REFERENCES fecha_hora(id),
  CONSTRAINT fk_viaje_fecha_fin FOREIGN KEY (FH_fin) REFERENCES fecha_hora(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE detalle_movil_conductor (
  id_movil BIGINT NOT NULL,
  id_usuario BIGINT NOT NULL,
  PRIMARY KEY (id_movil, id_usuario),
  CONSTRAINT fk_detalle_movil FOREIGN KEY (id_movil) REFERENCES movil(id_movil),
  CONSTRAINT fk_detalle_conductor FOREIGN KEY (id_usuario) REFERENCES conductor(id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE detalle_viaje_pasajero (
  id_viaje BIGINT NOT NULL,
  id_pasajero BIGINT NOT NULL,
  PRIMARY KEY (id_viaje, id_pasajero),
  CONSTRAINT fk_detalle_viaje FOREIGN KEY (id_viaje) REFERENCES viaje(id_viaje),
  CONSTRAINT fk_detalle_pasajero FOREIGN KEY (id_pasajero) REFERENCES pasajero(id_pasajero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
