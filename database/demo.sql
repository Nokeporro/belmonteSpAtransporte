-- Datos de demostración (T16). No es una migración: Flyway no lo ve.
-- Equivale a DatosDemo.kt del Android (mismas cuentas, vehículos, códigos de abordaje, pasajeros,
-- viajes y pagos), sobre el esquema de las migraciones V1 a V5. Se recarga antes de cada
-- demostración y de FlujosE2ETest; puede ejecutarse las veces que haga falta.
--
-- ATENCIÓN: vacía las tablas de negocio (usuarios, empresas, flota, viajes, pasajeros y pagos), incluido
-- el Administrador inicial admin@belmontespa.cl de V3. No toca catálogos, regiones ni comunas.
--
-- Cargar (la base debe estar migrada; conviene detener el backend mientras se carga):
--   MYSQL_PWD=<clave> mariadb -h127.0.0.1 -P3307 -u<usuario> belmonte < database/demo.sql
--
-- Contraseña ficticia común de las 9 cuentas: Belmonte2026
--   Administradores: recepcion@belmontespa.cl y cherrera@belmontespa.cl
--   Conductores:     ireyes@belmontespa.cl, pmunoz@belmontespa.cl, jlara@belmontespa.cl y
--                    msoto@belmontespa.cl (inactiva: no puede entrar)
--   Solicitantes:    crios@andesconsult.cl y mrojas@pacificoservicios.cl (aprobados) y
--                    rperez@andesconsult.cl (por aprobar: no puede entrar)
--
-- Viaje en ventana de inicio: TR-1005, ASIGNADO a NOW() + 30 min, con 3 pasajeros (1 ya abordado), del
-- Conductor Iván Reyes (ireyes@belmontespa.cl) en el vehículo KXJR42 (código 7F3A9C21B04E58D6). Para
-- abordar: patente KXJR42, ese código y, por ejemplo, el documento P4471023 (Tomás Vidal).
--
-- Folios (los asigna la base en orden de inserción desde 1001):
--   TR-1001 CANCELADO   TR-1002 CANCELADO   TR-1003 PENDIENTE (vencido)   TR-1004 FINALIZADO
--   TR-1005 ASIGNADO    TR-1006 EN_CURSO    TR-1007 PENDIENTE (atrasado)  TR-1008 ASIGNADO
--   TR-1009 PENDIENTE   TR-1010 PENDIENTE   TR-1011 PENDIENTE (sin pasajeros)
--
-- Los dni_hash son HMAC-SHA256 con la clave @dni_secret, igual que el backend. Sin definirla, se usa el
-- valor de desarrollo de application.properties. Si el backend corre con otro DNI_SECRET (la demostración
-- lo fija en .env.demo), el mismo valor debe pasarse al cargar, o el abordaje no encontrará a nadie:
--   mariadb ... --init-command="SET @dni_secret='<DNI_SECRET>'" belmonte < database/demo.sql

SET NAMES utf8mb4;
-- Las fechas del backend son de America/Santiago (DB_TIME_ZONE); con otra zona los viajes relativos a
-- NOW() quedarían fuera de la ventana de inicio de ±1 h.
SET time_zone = 'America/Santiago';
SET @ahora = NOW();
SET @dni_secret = COALESCE(@dni_secret, 'belmonte-dev-dni-secret-cambiar-en-despliegue');
-- BCrypt (coste 10) de Belmonte2026.
SET @clave_demo = '$2a$10$74rjF7HmEbsE7OSa7nzY4ufUngtZRcZDn6bOOOxjHCDEOghwPgS8a';

-- ---------------------------------------------------------------- vaciado

-- TRUNCATE no dispara triggers y reinicia los AUTO_INCREMENT; con las claves foráneas apagadas el
-- orden no importa.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE historial_viaje;
TRUNCATE TABLE detalle_viaje_pasajero;
TRUNCATE TABLE viaje;
TRUNCATE TABLE pago;
TRUNCATE TABLE pasajero;
TRUNCATE TABLE detalle_movil_conductor;
TRUNCATE TABLE movil;
TRUNCATE TABLE conductor;
TRUNCATE TABLE administrador;
TRUNCATE TABLE solicitante;
TRUNCATE TABLE usuario_rol_activo;
TRUNCATE TABLE usuario;
TRUNCATE TABLE empresa;
TRUNCATE TABLE ubicacion;
SET FOREIGN_KEY_CHECKS = 1;

-- bi_viaje asigna TR-{siguiente}; los folios de la demostración parten en TR-1001.
ALTER SEQUENCE seq_folio_viaje RESTART WITH 1001;

-- ---------------------------------------------------------------- cuentas

-- Contraseña permanente (sin password_temporal_*): entran sin el cambio obligatorio.
INSERT INTO usuario (nombre, correo, telefono, password_hash, fecha_registro) VALUES
  ('Andrea Castro',   'recepcion@belmontespa.cl',    NULL,           @clave_demo, @ahora - INTERVAL 90 DAY),
  ('Camila Herrera',  'cherrera@belmontespa.cl',     '+56955550505', @clave_demo, @ahora - INTERVAL 90 DAY),
  ('Iván Reyes',      'ireyes@belmontespa.cl',       '+56955550303', @clave_demo, @ahora - INTERVAL 90 DAY),
  ('Paula Muñoz',     'pmunoz@belmontespa.cl',       '+56955550202', @clave_demo, @ahora - INTERVAL 90 DAY),
  ('Jorge Lara',      'jlara@belmontespa.cl',        NULL,           @clave_demo, @ahora - INTERVAL 90 DAY),
  ('Marta Soto',      'msoto@belmontespa.cl',        NULL,           @clave_demo, @ahora - INTERVAL 90 DAY),
  ('Constanza Ríos',  'crios@andesconsult.cl',       '+56955550101', @clave_demo, @ahora - INTERVAL 90 DAY),
  ('Rodrigo Pérez',   'rperez@andesconsult.cl',      NULL,           @clave_demo, @ahora - INTERVAL 90 DAY),
  ('Matías Rojas',    'mrojas@pacificoservicios.cl', '+56955550404', @clave_demo, @ahora - INTERVAL 90 DAY);

-- Los triggers bi_*_rol registran el rol en usuario_rol_activo.
INSERT INTO administrador (id_usuario)
SELECT id_usuario FROM usuario
 WHERE correo IN ('recepcion@belmontespa.cl', 'cherrera@belmontespa.cl');

INSERT INTO conductor (id_usuario, estado_conductor_id_estado, activo)
SELECT u.id_usuario, e.id_estado, c.activo
  FROM (SELECT 'ireyes@belmontespa.cl' AS correo, 'DISPONIBLE' AS estado, TRUE AS activo
        UNION ALL SELECT 'pmunoz@belmontespa.cl', 'DISPONIBLE', TRUE
        UNION ALL SELECT 'jlara@belmontespa.cl',  'OCUPADO',    TRUE
        UNION ALL SELECT 'msoto@belmontespa.cl',  'DISPONIBLE', FALSE) c
  JOIN usuario u ON u.correo = c.correo
  JOIN estado_conductor e ON e.estado = c.estado;

INSERT INTO empresa (razon_social, rut) VALUES
  ('Andes Consultores SpA',   '76543210-3'),
  ('Pacífico Servicios Ltda.', '77123456-9');

INSERT INTO solicitante (id_usuario, empresa_id_empresa, estado_solicitante_id)
SELECT u.id_usuario, em.id_empresa, es.id
  FROM (SELECT 'crios@andesconsult.cl' AS correo, '76543210-3' AS rut, 'APROBADO' AS estado
        UNION ALL SELECT 'rperez@andesconsult.cl',      '76543210-3', 'PENDIENTE'
        UNION ALL SELECT 'mrojas@pacificoservicios.cl', '77123456-9', 'APROBADO') s
  JOIN usuario u ON u.correo = s.correo
  JOIN empresa em ON em.rut = s.rut
  JOIN estado_solicitante es ON es.estado = s.estado;

-- ---------------------------------------------------------------- flota

-- 4 vehículos (1 inactivo) con un codigo_abordaje fijo cada uno.
INSERT INTO movil (patente, gps_serial, estado_movil_id_estado_mov, capacidad, activo, codigo_abordaje)
SELECT m.patente, m.gps_serial, e.id_estado_mov, m.capacidad, m.activo, m.codigo
  FROM (SELECT 'KXJR42' AS patente, 50231 AS gps_serial, 'DISPONIBLE' AS estado, 4 AS capacidad,
               TRUE AS activo, '7F3A9C21B04E58D6' AS codigo
        UNION ALL SELECT 'LPHT17', NULL,  'DISPONIBLE', 6, TRUE,  '2B8E41D07C95A316'
        UNION ALL SELECT 'GZRW90', 50477, 'EN_USO',     4, TRUE,  '9D04C6E2A1F7B853'
        UNION ALL SELECT 'HJKL21', NULL,  'DISPONIBLE', 4, FALSE, '5C1A8F3E0B6D2947') m
  JOIN estado_movil e ON e.nombre = m.estado;

-- Cada Conductor activo queda habilitado para un vehículo activo.
INSERT INTO detalle_movil_conductor (id_movil, id_usuario)
SELECT mo.id_movil, u.id_usuario
  FROM (SELECT 'KXJR42' AS patente, 'ireyes@belmontespa.cl' AS correo
        UNION ALL SELECT 'LPHT17', 'ireyes@belmontespa.cl'
        UNION ALL SELECT 'KXJR42', 'pmunoz@belmontespa.cl'
        UNION ALL SELECT 'GZRW90', 'jlara@belmontespa.cl'
        UNION ALL SELECT 'HJKL21', 'msoto@belmontespa.cl') h
  JOIN movil mo ON mo.patente = h.patente
  JOIN usuario u ON u.correo = h.correo;

-- ---------------------------------------------------------------- ubicaciones

INSERT INTO ubicacion (direccion, id_comuna, latitud, longitud)
SELECT l.direccion, c.id_comuna, l.latitud, l.longitud
  FROM (SELECT 'Aeropuerto Arturo Merino Benítez' AS direccion, 'Pudahuel' AS comuna,
               'Metropolitana de Santiago' AS region, -33.392975 AS latitud, -70.785803 AS longitud
        UNION ALL SELECT 'Av. Apoquindo 5400',            'Las Condes',  'Metropolitana de Santiago', -33.411520, -70.575384
        UNION ALL SELECT 'Av. Isidora Goyenechea 3000',   'Las Condes',  'Metropolitana de Santiago', -33.414710, -70.602530
        UNION ALL SELECT 'Av. Vitacura 2939',             'Las Condes',  'Metropolitana de Santiago', -33.413820, -70.601170
        UNION ALL SELECT 'Av. El Bosque Norte 500',       'Las Condes',  'Metropolitana de Santiago', -33.414400, -70.598300
        UNION ALL SELECT 'Av. Vitacura 5250',             'Vitacura',    'Metropolitana de Santiago', -33.399010, -70.590230
        UNION ALL SELECT 'Av. Irarrázaval 3400',          'Ñuñoa',       'Metropolitana de Santiago', -33.454600, -70.596700
        UNION ALL SELECT 'Av. Providencia 1760',          'Providencia', 'Metropolitana de Santiago', -33.425480, -70.611990
        UNION ALL SELECT 'Av. San Martín 199',            'Viña del Mar', 'Valparaíso',               -33.019800, -71.552100
        UNION ALL SELECT 'Muelle Prat',                   'Valparaíso',  'Valparaíso',                -33.037900, -71.627600) l
  JOIN region r ON r.nombre = l.region
  JOIN comuna c ON c.nombre = l.comuna AND c.id_region = r.id_region;

-- ---------------------------------------------------------------- pasajeros

-- dni_hash = Base64url sin relleno de HMAC-SHA256(@dni_secret, documento normalizado), como
-- PasajeroService.hashDni. MariaDB no trae HMAC: se arma con SHA2 y las claves ipad (0x36) y opad (0x5C)
-- de 64 bytes; una clave más larga se reemplaza antes por su SHA-256.
SET @clave_hmac = RPAD(
  IF(LENGTH(CONVERT(@dni_secret USING binary)) > 64, UNHEX(SHA2(@dni_secret, 256)), CONVERT(@dni_secret USING binary)),
  64, CHAR(0));
SET @ipad = (SELECT GROUP_CONCAT(CHAR(ASCII(SUBSTRING(@clave_hmac, seq, 1)) ^ 54) ORDER BY seq SEPARATOR '')
               FROM seq_1_to_64);
SET @opad = (SELECT GROUP_CONCAT(CHAR(ASCII(SUBSTRING(@clave_hmac, seq, 1)) ^ 92) ORDER BY seq SEPARATOR '')
               FROM seq_1_to_64);

INSERT INTO pasajero (dni_hash, nombre)
SELECT REPLACE(REPLACE(TRIM(TRAILING '=' FROM TO_BASE64(UNHEX(SHA2(CONCAT(@opad, UNHEX(SHA2(CONCAT(@ipad,
         UPPER(REPLACE(REPLACE(REPLACE(d.documento, '.', ''), '-', ''), ' ', ''))), 256))), 256)))), '+', '-'), '/', '_'),
       d.nombre
  FROM (SELECT 'Constanza Ríos' AS nombre, '15.234.567-4' AS documento
        UNION ALL SELECT 'Tomás Vidal',     'P4471023'
        UNION ALL SELECT 'Marcela Soto',    '16.789.012-1'
        UNION ALL SELECT 'Roberto Aguilar', '12.345.678-5'
        UNION ALL SELECT 'Paula Lagos',     '13.579.246-2'
        UNION ALL SELECT 'Luis Pino',       '14.682.035-2'
        UNION ALL SELECT 'Diego Fuentes',   '17.246.810-1'
        UNION ALL SELECT 'Felipe Mora',     'X9087654'
        UNION ALL SELECT 'Javiera Núñez',   '18.364.925-6') d;

-- ---------------------------------------------------------------- pagos

-- bu_pago solo valida los UPDATE: cada pago nace en su estado final. El monto reembolsado del
-- REEMBOLSADO es el 50 % (P4).
INSERT INTO pago (hash, monto, estado_pago_id, banco_id, empresa_id_empresa, medio_pago_id, tipo_recibo_id,
                  fecha_pago, monto_reembolsado)
SELECT p.hash, p.monto, ep.id, b.id, em.id_empresa, mp.id, tr.id, p.fecha_pago, p.monto_reembolsado
  FROM (SELECT '9a4c2e7b1d5f3086c2a9e4b7d1f5c3e2' AS hash, 40000 AS monto, 'REEMBOLSADO' AS estado,
               12 AS cod_sbif, 'TRANSFERENCIA' AS medio, 'FACTURA' AS recibo, '76543210-3' AS rut,
               @ahora - INTERVAL 4 DAY AS fecha_pago, 20000 AS monto_reembolsado
        UNION ALL SELECT '5d3b1f8e2a7c4096d1e5b3a8c7f2e014', 35000, 'ANULADO',   1,  'TARJETA_CREDITO', 'BOLETA',  '77123456-9', NULL, NULL
        UNION ALL SELECT 'b71e4d2a9c3f5086a1d7e2c4b9f03a58', 52000, 'PAGADO',    37, 'TRANSFERENCIA',   'FACTURA', '76543210-3', @ahora - INTERVAL 2 DAY, NULL
        UNION ALL SELECT '0c5e8a3d7f1b4926b8e2d5a7c1f0e963', 45000, 'RECHAZADO', 16, 'TARJETA_DEBITO',  'FACTURA', '76543210-3', NULL, NULL
        UNION ALL SELECT '3f9a1c7e5b2d4a608e1f7c3b9d2a5e41', 48000, 'PAGADO',    12, 'TRANSFERENCIA',   'FACTURA', '76543210-3', @ahora - INTERVAL 1 DAY, NULL
        UNION ALL SELECT 'e2a7c9f1d4b6083a5c7e1b9d2f4a6c80', 38000, 'PENDIENTE', 39, 'EFECTIVO',        'BOLETA',  '76543210-3', NULL, NULL) p
  JOIN estado_pago ep ON ep.estado = p.estado
  JOIN banco b ON b.cod_sbif = p.cod_sbif
  JOIN medio_pago mp ON mp.nombre = p.medio
  JOIN tipo_recibo tr ON tr.nombre = p.recibo
  JOIN empresa em ON em.rut = p.rut;

-- ---------------------------------------------------------------- viajes

-- Una fila por viaje, en el orden de los folios (orden 1 = TR-1001). Los estados EN_CURSO, FINALIZADO y
-- CANCELADO se alcanzan más abajo por transiciones válidas de bu_viaje, porque
-- bi_detalle_viaje_pasajero solo admite pasajeros en viajes PENDIENTE o ASIGNADO.
CREATE TEMPORARY TABLE tmp_viaje (
  orden         INT          NOT NULL,
  estado        VARCHAR(20)  NOT NULL,
  solicitante   VARCHAR(150) NOT NULL,
  origen        VARCHAR(200) NOT NULL,
  destino       VARCHAR(200) NOT NULL,
  fh_agenda     DATETIME     NOT NULL,
  numero_vuelo  VARCHAR(20)  NULL,
  motivo        VARCHAR(200) NULL,
  conductor     VARCHAR(150) NULL,
  patente       VARCHAR(6)   NULL,
  fh_asignacion DATETIME     NULL,
  fh_inicio     DATETIME     NULL,
  fh_fin        DATETIME     NULL,
  pago          VARCHAR(50)  NULL,
  id_viaje      BIGINT UNSIGNED NULL,
  PRIMARY KEY (orden)
);

INSERT INTO tmp_viaje (orden, estado, solicitante, origen, destino, fh_agenda, numero_vuelo, motivo,
                       conductor, patente, fh_asignacion, fh_inicio, fh_fin, pago) VALUES
  (1,  'CANCELADO',  'crios@andesconsult.cl',       'Av. El Bosque Norte 500',  'Aeropuerto Arturo Merino Benítez', @ahora - INTERVAL 1 DAY,
       NULL, NULL, NULL, NULL, NULL, NULL, NULL, '9a4c2e7b1d5f3086c2a9e4b7d1f5c3e2'),
  (2,  'CANCELADO',  'mrojas@pacificoservicios.cl', 'Av. San Martín 199',       'Muelle Prat',                      @ahora + INTERVAL 2 DAY,
       NULL, 'Cliente reprogramó el traslado', NULL, NULL, NULL, NULL, NULL, '5d3b1f8e2a7c4096d1e5b3a8c7f2e014'),
  (3,  'PENDIENTE',  'crios@andesconsult.cl',       'Av. Irarrázaval 3400',     'Aeropuerto Arturo Merino Benítez', @ahora - INTERVAL 3 HOUR,
       NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'b71e4d2a9c3f5086a1d7e2c4b9f03a58'),
  (4,  'FINALIZADO', 'crios@andesconsult.cl',       'Av. Apoquindo 5400',       'Aeropuerto Arturo Merino Benítez', @ahora - INTERVAL 5 HOUR,
       NULL, NULL, 'ireyes@belmontespa.cl', 'LPHT17', @ahora - INTERVAL 2 DAY,
       @ahora - INTERVAL 305 MINUTE, @ahora - INTERVAL 4 HOUR, '0c5e8a3d7f1b4926b8e2d5a7c1f0e963'),
  (5,  'ASIGNADO',   'crios@andesconsult.cl',       'Aeropuerto Arturo Merino Benítez', 'Av. Apoquindo 5400',       @ahora + INTERVAL 30 MINUTE,
       'LA1502', NULL, 'ireyes@belmontespa.cl', 'KXJR42', @ahora - INTERVAL 1 DAY, NULL, NULL, '3f9a1c7e5b2d4a608e1f7c3b9d2a5e41'),
  (6,  'EN_CURSO',   'mrojas@pacificoservicios.cl', 'Av. Providencia 1760',     'Aeropuerto Arturo Merino Benítez', @ahora - INTERVAL 20 MINUTE,
       NULL, NULL, 'jlara@belmontespa.cl', 'GZRW90', @ahora - INTERVAL 1 DAY, @ahora - INTERVAL 25 MINUTE, NULL, NULL),
  (7,  'PENDIENTE',  'crios@andesconsult.cl',       'Av. Providencia 1760',     'Aeropuerto Arturo Merino Benítez', @ahora - INTERVAL 30 MINUTE,
       NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  (8,  'ASIGNADO',   'crios@andesconsult.cl',       'Av. Isidora Goyenechea 3000', 'Aeropuerto Arturo Merino Benítez', @ahora + INTERVAL 255 MINUTE,
       NULL, NULL, 'ireyes@belmontespa.cl', 'LPHT17', @ahora - INTERVAL 1 DAY, NULL, NULL, NULL),
  (9,  'PENDIENTE',  'crios@andesconsult.cl',       'Av. Vitacura 5250',        'Aeropuerto Arturo Merino Benítez', @ahora + INTERVAL 1 DAY,
       NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'e2a7c9f1d4b6083a5c7e1b9d2f4a6c80'),
  (10, 'PENDIENTE',  'mrojas@pacificoservicios.cl', 'Av. San Martín 199',       'Aeropuerto Arturo Merino Benítez', @ahora + INTERVAL 3 DAY,
       NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  (11, 'PENDIENTE',  'crios@andesconsult.cl',       'Av. Vitacura 2939',        'Aeropuerto Arturo Merino Benítez', @ahora + INTERVAL 2 DAY,
       NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- Todo viaje nace PENDIENTE (los que se cancelarán, sin conductor) o ASIGNADO (con conductor y vehículo).
-- El orden del ORDER BY fija el del folio; el '' solo satisface el NOT NULL, porque bi_viaje lo reemplaza.
INSERT INTO viaje (folio, id_solicitante, id_origen, id_destino, id_estado, movil_id_movil, pago_id,
                   fh_creacion, fh_agenda, fh_asignacion, conductor_id_usuario, numero_vuelo, motivo)
SELECT '', s.id_usuario, o.id_ubicacion, d.id_ubicacion, es.id_estado, m.id_movil, p.id,
       LEAST(t.fh_agenda, @ahora) - INTERVAL 2 DAY, t.fh_agenda, t.fh_asignacion, c.id_usuario,
       t.numero_vuelo, t.motivo
  FROM tmp_viaje t
  JOIN usuario s ON s.correo = t.solicitante
  JOIN ubicacion o ON o.direccion = t.origen
  JOIN ubicacion d ON d.direccion = t.destino
  JOIN estado_solicitud es ON es.nombre_estado = IF(t.conductor IS NULL, 'PENDIENTE', 'ASIGNADO')
  LEFT JOIN usuario c ON c.correo = t.conductor
  LEFT JOIN movil m ON m.patente = t.patente
  LEFT JOIN pago p ON p.hash = t.pago
 ORDER BY t.orden;

-- Los triggers de detalle_viaje_pasajero actualizan viaje: la consulta que los dispara no puede leerla,
-- así que los ids se resuelven antes.
UPDATE tmp_viaje t
  JOIN viaje v ON v.folio = CONCAT('TR-', 1000 + t.orden)
   SET t.id_viaje = v.id_viaje;

-- ---------------------------------------------------------------- pasajeros de cada viaje

-- fh_abordaje con fecha = Abordado; NULL = Por abordar.
CREATE TEMPORARY TABLE tmp_pasajero_viaje (
  orden        INT          NOT NULL,
  pasajero     VARCHAR(120) NOT NULL,
  fh_abordaje  DATETIME     NULL
);

INSERT INTO tmp_pasajero_viaje (orden, pasajero, fh_abordaje) VALUES
  (1,  'Paula Lagos',     NULL),
  (2,  'Javiera Núñez',   NULL),
  (3,  'Luis Pino',       NULL),
  (4,  'Roberto Aguilar', @ahora - INTERVAL 310 MINUTE),
  (5,  'Constanza Ríos',  @ahora - INTERVAL 5 MINUTE),
  (5,  'Tomás Vidal',     NULL),
  (5,  'Marcela Soto',    NULL),
  (6,  'Javiera Núñez',   @ahora - INTERVAL 30 MINUTE),
  (7,  'Diego Fuentes',   NULL),
  (8,  'Felipe Mora',     NULL),
  (9,  'Marcela Soto',    NULL),
  (9,  'Tomás Vidal',     NULL),
  (10, 'Javiera Núñez',   NULL);

-- ai_detalle_viaje_pasajero mantiene viaje.cantidad_pasajeros.
INSERT INTO detalle_viaje_pasajero (id_viaje, id_pasajero, fh_abordaje)
SELECT t.id_viaje, p.id_pasajero, pv.fh_abordaje
  FROM tmp_pasajero_viaje pv
  JOIN tmp_viaje t ON t.orden = pv.orden
  JOIN pasajero p ON p.nombre = pv.pasajero
 ORDER BY pv.orden;

-- ---------------------------------------------------------------- estados finales

-- ASIGNADO → EN_CURSO → FINALIZADO, y PENDIENTE → CANCELADO (au_viaje deja el historial).
UPDATE viaje v
  JOIN tmp_viaje t ON t.id_viaje = v.id_viaje
  JOIN estado_solicitud es ON es.nombre_estado = 'EN_CURSO'
   SET v.id_estado = es.id_estado, v.fh_inicio = t.fh_inicio
 WHERE t.estado IN ('EN_CURSO', 'FINALIZADO');

UPDATE viaje v
  JOIN tmp_viaje t ON t.id_viaje = v.id_viaje
  JOIN estado_solicitud es ON es.nombre_estado = 'FINALIZADO'
   SET v.id_estado = es.id_estado, v.fh_fin = t.fh_fin
 WHERE t.estado = 'FINALIZADO';

UPDATE viaje v
  JOIN tmp_viaje t ON t.id_viaje = v.id_viaje
  JOIN estado_solicitud es ON es.nombre_estado = 'CANCELADO'
   SET v.id_estado = es.id_estado
 WHERE t.estado = 'CANCELADO';

DROP TEMPORARY TABLE tmp_pasajero_viaje;
DROP TEMPORARY TABLE tmp_viaje;
