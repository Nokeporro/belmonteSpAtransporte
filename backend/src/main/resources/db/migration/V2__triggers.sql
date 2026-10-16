-- Reglas que la base de datos valida y audita (README, "Decisiones de modelo" y "CRUD REST").
-- Los rechazos usan SQLSTATE '23000' (violación de integridad): Spring los traduce a
-- DataIntegrityViolationException y ApiExceptionHandler responde 409.

-- ---------------------------------------------------------------- exclusividad de rol

-- Cada rol registra al usuario en usuario_rol_activo; la PK rechaza un segundo rol.
CREATE TRIGGER bi_solicitante_rol
BEFORE INSERT ON solicitante
FOR EACH ROW
BEGIN
  INSERT INTO usuario_rol_activo (id_usuario) VALUES (NEW.id_usuario);
END;

CREATE TRIGGER bi_administrador_rol
BEFORE INSERT ON administrador
FOR EACH ROW
BEGIN
  INSERT INTO usuario_rol_activo (id_usuario) VALUES (NEW.id_usuario);
END;

CREATE TRIGGER bi_conductor_rol
BEFORE INSERT ON conductor
FOR EACH ROW
BEGIN
  INSERT INTO usuario_rol_activo (id_usuario) VALUES (NEW.id_usuario);
END;

-- ---------------------------------------------------------------- viaje

-- Respaldo: el backend genera el folio en Viaje.@PrePersist con el mismo formato.
CREATE TRIGGER bi_viaje
BEFORE INSERT ON viaje
FOR EACH ROW
BEGIN
  IF NEW.folio IS NULL OR NEW.folio = '' THEN
    SET NEW.folio = CONCAT('VJ', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 18)));
  END IF;
END;

-- Inmutables, transiciones de estado (CU-05/CU-08), habilitación conductor↔móvil (CU-04)
-- y capacidad del móvil.
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
        OR (estado_actual = 'ASIGNADO' AND estado_nuevo IN ('EN_CURSO', 'CANCELADO'))
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

-- HISTORIAL_VIAJE: una fila por cada campo que cambia (HU05). folio y fh_creacion son
-- inmutables y nunca generan fila. NULL se registra como '(sin valor)'.
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
  IF NOT (OLD.id_origen <=> NEW.id_origen) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'origen',
            (SELECT direccion FROM ubicacion WHERE id_ubicacion = NEW.id_origen));
  END IF;
  IF NOT (OLD.id_destino <=> NEW.id_destino) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'destino',
            (SELECT direccion FROM ubicacion WHERE id_ubicacion = NEW.id_destino));
  END IF;
  IF NOT (OLD.fh_agenda <=> NEW.fh_agenda) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'fh_agenda', CAST(NEW.fh_agenda AS CHAR));
  END IF;
  IF NOT (OLD.fh_asignacion <=> NEW.fh_asignacion) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'fh_asignacion', COALESCE(CAST(NEW.fh_asignacion AS CHAR), '(sin valor)'));
  END IF;
  IF NOT (OLD.fh_inicio <=> NEW.fh_inicio) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'fh_inicio', COALESCE(CAST(NEW.fh_inicio AS CHAR), '(sin valor)'));
  END IF;
  IF NOT (OLD.fh_fin <=> NEW.fh_fin) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'fh_fin', COALESCE(CAST(NEW.fh_fin AS CHAR), '(sin valor)'));
  END IF;
  IF NOT (OLD.numero_vuelo <=> NEW.numero_vuelo) THEN
    INSERT INTO historial_viaje (id_viaje, campo_modificado, valor)
    VALUES (NEW.id_viaje, 'numero_vuelo', COALESCE(NEW.numero_vuelo, '(sin valor)'));
  END IF;
END;

-- ---------------------------------------------------------------- pasajeros del viaje

-- Solo en viajes PENDIENTE o ASIGNADO y sin superar la capacidad del móvil asignado.
CREATE TRIGGER bi_detalle_viaje_pasajero
BEFORE INSERT ON detalle_viaje_pasajero
FOR EACH ROW
BEGIN
  DECLARE estado_viaje VARCHAR(20);
  DECLARE cantidad INT;
  DECLARE capacidad_movil INT;

  SELECT e.nombre_estado, v.cantidad_pasajeros, m.capacidad
    INTO estado_viaje, cantidad, capacidad_movil
    FROM viaje v
    JOIN estado_solicitud e ON e.id_estado = v.id_estado
    LEFT JOIN movil m ON m.id_movil = v.movil_id_movil
   WHERE v.id_viaje = NEW.id_viaje;

  IF estado_viaje NOT IN ('PENDIENTE', 'ASIGNADO') THEN
    SIGNAL SQLSTATE '23000' SET MESSAGE_TEXT = 'Solo se agregan pasajeros a viajes PENDIENTE o ASIGNADO';
  END IF;
  IF capacidad_movil IS NOT NULL AND cantidad >= capacidad_movil THEN
    SIGNAL SQLSTATE '23000' SET MESSAGE_TEXT = 'El viaje ya alcanzó la capacidad del móvil';
  END IF;
END;

-- VIAJE.cantidad_pasajeros es una denormalización deliberada (RNF03).
CREATE TRIGGER ai_detalle_viaje_pasajero
AFTER INSERT ON detalle_viaje_pasajero
FOR EACH ROW
BEGIN
  UPDATE viaje SET cantidad_pasajeros = cantidad_pasajeros + 1 WHERE id_viaje = NEW.id_viaje;
END;

CREATE TRIGGER ad_detalle_viaje_pasajero
AFTER DELETE ON detalle_viaje_pasajero
FOR EACH ROW
BEGIN
  UPDATE viaje SET cantidad_pasajeros = GREATEST(cantidad_pasajeros - 1, 0) WHERE id_viaje = OLD.id_viaje;
END;

-- ---------------------------------------------------------------- pago

-- CU-12/RF19: PENDIENTE → PAGADO | RECHAZADO | ANULADO; PAGADO → ANULADO (CU-08).
-- RECHAZADO y ANULADO son terminales.
CREATE TRIGGER bu_pago
BEFORE UPDATE ON pago
FOR EACH ROW
BEGIN
  DECLARE estado_actual VARCHAR(20);
  DECLARE estado_nuevo VARCHAR(20);

  IF NOT (OLD.estado_pago_id <=> NEW.estado_pago_id) THEN
    SELECT estado INTO estado_actual FROM estado_pago WHERE id = OLD.estado_pago_id;
    SELECT estado INTO estado_nuevo FROM estado_pago WHERE id = NEW.estado_pago_id;
    IF NOT ((estado_actual = 'PENDIENTE' AND estado_nuevo IN ('PAGADO', 'RECHAZADO', 'ANULADO'))
        OR (estado_actual = 'PAGADO' AND estado_nuevo = 'ANULADO')) THEN
      SIGNAL SQLSTATE '23000' SET MESSAGE_TEXT = 'Transición de estado de pago no permitida';
    END IF;
  END IF;
END;
