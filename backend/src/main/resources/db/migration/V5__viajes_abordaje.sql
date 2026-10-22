-- T08: lleva viajes y abordaje al modelo del README ("Modelo de datos", "Máquinas de estado" y
-- "Abordaje"). V1 a V4 son históricas y no se editan; esta migración corre también sobre bases con
-- datos. MariaDB no revierte el DDL: si falla a mitad de camino, se restaura el respaldo previo.

-- ---------------------------------------------------------------- columnas nuevas

-- Motivo de la cancelación del Administrador (V7) o del rechazo del Conductor (V8).
ALTER TABLE viaje
  ADD COLUMN motivo VARCHAR(200) NULL AFTER numero_vuelo;

-- Marca de abordaje de cada pasajero: con fecha, Abordado; NULL, Por abordar.
ALTER TABLE detalle_viaje_pasajero
  ADD COLUMN fh_abordaje DATETIME NULL;

-- Código del QR de abordaje de cada vehículo: 16 hexadecimales en mayúsculas. Los vehículos que ya
-- existen reciben uno al azar antes de volverse obligatorio y único.
ALTER TABLE movil
  ADD COLUMN codigo_abordaje VARCHAR(16) NULL;

UPDATE movil
   SET codigo_abordaje = HEX(RANDOM_BYTES(8));

ALTER TABLE movil
  MODIFY COLUMN codigo_abordaje VARCHAR(16) NOT NULL;

ALTER TABLE movil
  ADD CONSTRAINT uk_movil_codigo_abordaje UNIQUE (codigo_abordaje);

-- ---------------------------------------------------------------- folio de los viajes

-- Correlativo del folio: TR-1001, TR-1002, etc. Una secuencia es atómica y no bloquea inserciones
-- concurrentes. NOCACHE evita que un reinicio del servidor salte hasta 1000 números; si una
-- transacción se revierte igual queda un hueco.
CREATE SEQUENCE seq_folio_viaje START WITH 1001 INCREMENT BY 1 NOCACHE;

-- Los triggers de viaje se recrean más abajo. bu_viaje hace inmutable el folio, así que tiene que
-- desaparecer mientras se renumeran los viajes que ya existen.
DROP TRIGGER IF EXISTS bi_viaje;
DROP TRIGGER IF EXISTS bu_viaje;
DROP TRIGGER IF EXISTS au_viaje;

-- Hasta V4 el folio era VJ más un UUID. Los viajes existentes pasan al formato nuevo en orden de
-- creación, y la secuencia queda lista para los siguientes.
UPDATE viaje
   SET folio = CONCAT('TR-', NEXT VALUE FOR seq_folio_viaje)
 ORDER BY id_viaje;

-- ---------------------------------------------------------------- triggers de viaje

-- El folio siempre lo asigna la base de datos; el cliente no lo elige.
CREATE TRIGGER bi_viaje
BEFORE INSERT ON viaje
FOR EACH ROW
BEGIN
  SET NEW.folio = CONCAT('TR-', NEXT VALUE FOR seq_folio_viaje);
END;

-- Inmutables, transiciones de estado, habilitación conductor↔móvil y capacidad del móvil.
-- Respecto de V2 suma V8: el Conductor rechaza un viaje ASIGNADO y vuelve a PENDIENTE.
CREATE TRIGGER bu_viaje
BEFORE UPDATE ON viaje
FOR EACH ROW
BEGIN
  DECLARE estado_actual VARCHAR(20);
  DECLARE estado_nuevo VARCHAR(20);
  DECLARE capacidad_movil INT;

  IF NOT (OLD.folio <=> NEW.folio) OR NOT (OLD.fh_creacion <=> NEW.fh_creacion) THEN
    SIGNAL SQLSTATE '23000' SET MESSAGE_TEXT = 'folio y fh_creacion son inmutables';
  END IF;

  -- Repetir el estado (p. ej. reasignar un viaje ASIGNADO) no es una transición.
  IF NOT (OLD.id_estado <=> NEW.id_estado) THEN
    SELECT nombre_estado INTO estado_actual FROM estado_solicitud WHERE id_estado = OLD.id_estado;
    SELECT nombre_estado INTO estado_nuevo FROM estado_solicitud WHERE id_estado = NEW.id_estado;
    IF NOT ((estado_actual = 'PENDIENTE' AND estado_nuevo IN ('ASIGNADO', 'CANCELADO'))
        OR (estado_actual = 'ASIGNADO' AND estado_nuevo IN ('EN_CURSO', 'CANCELADO', 'PENDIENTE'))
        OR (estado_actual = 'EN_CURSO' AND estado_nuevo = 'FINALIZADO')) THEN
      SIGNAL SQLSTATE '23000' SET MESSAGE_TEXT = 'Transición de estado de viaje no permitida';
    END IF;
  END IF;

  IF NEW.conductor_id_usuario IS NOT NULL AND NEW.movil_id_movil IS NOT NULL
      AND (NOT (OLD.conductor_id_usuario <=> NEW.conductor_id_usuario)
        OR NOT (OLD.movil_id_movil <=> NEW.movil_id_movil))
      AND NOT EXISTS (SELECT 1 FROM detalle_movil_conductor d
                      WHERE d.id_movil = NEW.movil_id_movil
                        AND d.id_usuario = NEW.conductor_id_usuario) THEN
    SIGNAL SQLSTATE '23000' SET MESSAGE_TEXT = 'El conductor no está habilitado para el móvil';
  END IF;

  IF NEW.movil_id_movil IS NOT NULL THEN
    SELECT capacidad INTO capacidad_movil FROM movil WHERE id_movil = NEW.movil_id_movil;
    IF NEW.cantidad_pasajeros > capacidad_movil THEN
      SIGNAL SQLSTATE '23000' SET MESSAGE_TEXT = 'Los pasajeros superan la capacidad del móvil';
    END IF;
  END IF;
END;

-- HISTORIAL_VIAJE: una fila por cada cambio de estado, conductor, movil, pago, cantidad_pasajeros
-- o motivo (HU05). Las fechas, el origen, el destino y el número de vuelo ya no se auditan.
-- NULL se registra como (sin valor).
CREATE TRIGGER au_viaje
AFTER UPDATE ON viaje
FOR EACH ROW
BEGIN
  IF NOT (OLD.id_estado <=> NEW.id_estado) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'estado',
            (SELECT nombre_estado FROM estado_solicitud WHERE id_estado = NEW.id_estado));
  END IF;
  IF NOT (OLD.conductor_id_usuario <=> NEW.conductor_id_usuario) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'conductor', COALESCE(
            (SELECT correo FROM usuario WHERE id_usuario = NEW.conductor_id_usuario), '(sin valor)'));
  END IF;
  IF NOT (OLD.movil_id_movil <=> NEW.movil_id_movil) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'movil', COALESCE(
            (SELECT patente FROM movil WHERE id_movil = NEW.movil_id_movil), '(sin valor)'));
  END IF;
  IF NOT (OLD.pago_id <=> NEW.pago_id) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'pago', COALESCE(
            (SELECT hash FROM pago WHERE id = NEW.pago_id), '(sin valor)'));
  END IF;
  IF NOT (OLD.cantidad_pasajeros <=> NEW.cantidad_pasajeros) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'cantidad_pasajeros', CAST(NEW.cantidad_pasajeros AS CHAR));
  END IF;
  IF NOT (OLD.motivo <=> NEW.motivo) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'motivo', COALESCE(NEW.motivo, '(sin valor)'));
  END IF;
END;
