-- Esquema inicial de Belmonte SpA, compatible con MariaDB/InnoDB.
-- Oracle Data Modeler permanece como fuente del modelo visual; este archivo
-- es la fuente ejecutable y versionada del esquema de la aplicación.

CREATE TABLE region (
    id_region BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    PRIMARY KEY (id_region),
    CONSTRAINT uk_region_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comuna (
    id_comuna BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    id_region BIGINT NOT NULL,
    PRIMARY KEY (id_comuna),
    CONSTRAINT uk_comuna_nombre_region UNIQUE (nombre, id_region),
    CONSTRAINT fk_comuna_region FOREIGN KEY (id_region) REFERENCES region (id_region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ubicacion (
    id_ubicacion BIGINT NOT NULL AUTO_INCREMENT,
    direccion VARCHAR(200) NOT NULL,
    id_comuna BIGINT NOT NULL,
    latitud DECIMAL(9,6),
    longitud DECIMAL(9,6),
    PRIMARY KEY (id_ubicacion),
    CONSTRAINT uk_ubicacion_direccion_comuna UNIQUE (direccion, id_comuna),
    CONSTRAINT fk_ubicacion_comuna FOREIGN KEY (id_comuna) REFERENCES comuna (id_comuna)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tipo_usuario (
    id_tipo_usuario BIGINT NOT NULL AUTO_INCREMENT,
    nombre_tipo VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_tipo_usuario),
    CONSTRAINT uk_tipo_usuario_nombre UNIQUE (nombre_tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usuario (
    id_usuario BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    password_hash VARCHAR(200) NOT NULL,
    id_tipo_usuario BIGINT NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_tipo FOREIGN KEY (id_tipo_usuario) REFERENCES tipo_usuario (id_tipo_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE solicitante (
    id_usuario BIGINT NOT NULL,
    empresa VARCHAR(150) NOT NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_solicitante_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE administrador (
    id_usuario BIGINT NOT NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_administrador_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conductor (
    id_usuario BIGINT NOT NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_conductor_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pasajero (
    id_pasajero BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    PRIMARY KEY (id_pasajero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE estado_solicitud_traslado (
    id_estado BIGINT NOT NULL AUTO_INCREMENT,
    nombre_estado VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_estado),
    CONSTRAINT uk_estado_solicitud_nombre UNIQUE (nombre_estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE solicitud_traslado (
    id_solicitud BIGINT NOT NULL AUTO_INCREMENT,
    id_solicitante BIGINT NOT NULL,
    id_conductor BIGINT,
    fecha_hora_traslado DATETIME NOT NULL,
    id_origen BIGINT NOT NULL,
    id_destino BIGINT NOT NULL,
    numero_vuelo VARCHAR(20),
    id_estado BIGINT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_asignacion DATETIME,
    fecha_inicio DATETIME,
    fecha_fin DATETIME,
    PRIMARY KEY (id_solicitud),
    KEY ix_solicitud_estado (id_estado),
    KEY ix_solicitud_conductor_fecha (id_conductor, fecha_hora_traslado),
    CONSTRAINT fk_solicitud_solicitante FOREIGN KEY (id_solicitante) REFERENCES solicitante (id_usuario),
    CONSTRAINT fk_solicitud_conductor FOREIGN KEY (id_conductor) REFERENCES conductor (id_usuario),
    CONSTRAINT fk_solicitud_origen FOREIGN KEY (id_origen) REFERENCES ubicacion (id_ubicacion),
    CONSTRAINT fk_solicitud_destino FOREIGN KEY (id_destino) REFERENCES ubicacion (id_ubicacion),
    CONSTRAINT fk_solicitud_estado FOREIGN KEY (id_estado) REFERENCES estado_solicitud_traslado (id_estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE detalle_solicitud_pasajero (
    id_solicitud BIGINT NOT NULL,
    id_pasajero BIGINT NOT NULL,
    PRIMARY KEY (id_solicitud, id_pasajero),
    KEY ix_detalle_pasajero (id_pasajero),
    CONSTRAINT fk_detalle_solicitud FOREIGN KEY (id_solicitud) REFERENCES solicitud_traslado (id_solicitud),
    CONSTRAINT fk_detalle_pasajero FOREIGN KEY (id_pasajero) REFERENCES pasajero (id_pasajero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tipo_usuario (nombre_tipo)
VALUES ('SOLICITANTE'), ('ADMINISTRADOR'), ('CONDUCTOR');

INSERT INTO estado_solicitud_traslado (nombre_estado)
VALUES ('PENDIENTE'), ('ASIGNADO'), ('EN_CURSO'), ('FINALIZADO');
