-- T07: lleva cuentas, flota y pagos al modelo del README ("Modelo de datos" y "Máquinas de estado").
-- V1 a V3 son históricas y no se editan; esta migración corre también sobre bases con datos.
-- MariaDB no revierte el DDL: si falla a mitad de camino, se restaura el respaldo previo.

-- ---------------------------------------------------------------- usuario

ALTER TABLE usuario
  ADD COLUMN password_temporal_hash VARCHAR(200) NULL AFTER password_hash;

-- Hasta V3 la temporal (RF21) se escribía sobre password_hash. Pasa a su propia columna y
-- password_hash queda inutilizable: el asterisco no tiene formato BCrypt, así que nunca coincide.
-- Incluye al Administrador inicial, cuya contraseña de V3 es una temporal de desarrollo.
UPDATE usuario
   SET password_temporal_hash = password_hash,
       password_hash = '*'
 WHERE password_temporal_expira IS NOT NULL;

-- Un token FCM pertenece a un solo usuario (RF13). Si dos cuentas lo comparten, ninguna lo
-- conserva y lo registra de nuevo en su próximo login.
UPDATE usuario u
  JOIN (SELECT token_fcm FROM usuario WHERE token_fcm IS NOT NULL
         GROUP BY token_fcm HAVING COUNT(*) > 1) d ON d.token_fcm = u.token_fcm
   SET u.token_fcm = NULL;

ALTER TABLE usuario
  ADD CONSTRAINT uk_usuario_token_fcm UNIQUE (token_fcm);

-- ---------------------------------------------------------------- solicitante

CREATE TABLE estado_solicitante (
  id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  estado VARCHAR(20)     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_estado_solicitante (estado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO estado_solicitante (estado) VALUES ('PENDIENTE'), ('APROBADO'), ('RECHAZADO');

-- Los Solicitantes que ya existen quedan aprobados: hasta ahora podían operar sin revisión.
ALTER TABLE solicitante
  ADD COLUMN estado_solicitante_id BIGINT UNSIGNED NULL;

UPDATE solicitante
   SET estado_solicitante_id = (SELECT id FROM estado_solicitante WHERE estado = 'APROBADO');

ALTER TABLE solicitante
  MODIFY COLUMN estado_solicitante_id BIGINT UNSIGNED NOT NULL,
  ADD CONSTRAINT fk_solicitante_estado FOREIGN KEY (estado_solicitante_id)
    REFERENCES estado_solicitante (id);

-- ---------------------------------------------------------------- flota

-- RF25: Conductores y vehículos nacen activos.
ALTER TABLE conductor
  ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE movil
  ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

-- El README no tiene el estado MANTENCION. Un vehículo en mantención no se puede asignar, que
-- es lo que significa estar inactivo: pasa a Disponible pero inactivo.
UPDATE movil
   SET activo = FALSE,
       estado_movil_id_estado_mov = (SELECT id_estado_mov FROM estado_movil WHERE nombre = 'DISPONIBLE')
 WHERE estado_movil_id_estado_mov = (SELECT id_estado_mov FROM estado_movil WHERE nombre = 'MANTENCION');

DELETE FROM estado_movil WHERE nombre = 'MANTENCION';

-- ---------------------------------------------------------------- pago

-- fecha_pago se fija al pasar a PAGADO (P2), así que un pago nuevo no la tiene.
-- monto_reembolsado lo fija P4 al reembolsar, en V5 o V7.
ALTER TABLE pago
  MODIFY COLUMN fecha_pago DATETIME NULL,
  ADD COLUMN monto_reembolsado INT NULL AFTER fecha_pago;

INSERT INTO estado_pago (estado) VALUES ('REEMBOLSADO');

-- P2 a P4: PENDIENTE pasa a PAGADO, RECHAZADO o ANULADO, y PAGADO pasa a ANULADO o REEMBOLSADO.
-- RECHAZADO, ANULADO y REEMBOLSADO son terminales.
DROP TRIGGER IF EXISTS bu_pago;

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
        OR (estado_actual = 'PAGADO' AND estado_nuevo IN ('ANULADO', 'REEMBOLSADO'))) THEN
      SIGNAL SQLSTATE '23000' SET MESSAGE_TEXT = 'Transición de estado de pago no permitida';
    END IF;
  END IF;
END;

-- ---------------------------------------------------------------- viaje

-- Un viaje tiene a lo más un pago y un pago pertenece a un solo viaje.
ALTER TABLE viaje
  ADD CONSTRAINT uk_viaje_pago UNIQUE (pago_id);
