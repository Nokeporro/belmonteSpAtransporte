-- Ajustes acumulativos del contrato API. Se conservan las columnas antiguas
-- mientras se completa la migración de los campos que el CRUD ya no expone.

ALTER TABLE usuario
  ADD COLUMN password_temporal_expira DATETIME NULL,
  ADD COLUMN token_fcm VARCHAR(255) NULL;

ALTER TABLE pasajero ADD COLUMN dni_hash VARCHAR(64) NULL;
UPDATE pasajero SET dni_hash = rut_hash WHERE dni_hash IS NULL;
ALTER TABLE pasajero MODIFY dni_hash VARCHAR(64) NOT NULL;
ALTER TABLE pasajero ADD CONSTRAINT uk_pasajero_dni_hash UNIQUE (dni_hash);

ALTER TABLE empresa ADD CONSTRAINT uk_empresa_rut UNIQUE (rut);

ALTER TABLE viaje ADD COLUMN folio VARCHAR(20) NULL;
UPDATE viaje SET folio = CONCAT('VJ', LPAD(id_viaje, 18, '0')) WHERE folio IS NULL;
ALTER TABLE viaje MODIFY folio VARCHAR(20) NOT NULL;
ALTER TABLE viaje ADD CONSTRAINT uk_viaje_folio UNIQUE (folio);
ALTER TABLE viaje ADD COLUMN cantidad_pasajeros INT NOT NULL DEFAULT 0;
UPDATE viaje v
SET cantidad_pasajeros = (SELECT COUNT(*) FROM detalle_viaje_pasajero d WHERE d.id_viaje = v.id_viaje);

CREATE TRIGGER bi_viaje_folio
BEFORE INSERT ON viaje
FOR EACH ROW
BEGIN
  IF NEW.folio IS NULL OR NEW.folio = '' THEN
    SET NEW.folio = CONCAT('VJ', UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 18)));
  END IF;
END;

CREATE TRIGGER ai_detalle_viaje_pasajero_cantidad
AFTER INSERT ON detalle_viaje_pasajero
FOR EACH ROW
BEGIN
  UPDATE viaje SET cantidad_pasajeros = cantidad_pasajeros + 1
  WHERE id_viaje = NEW.id_viaje;
END;

CREATE TRIGGER ad_detalle_viaje_pasajero_cantidad
AFTER DELETE ON detalle_viaje_pasajero
FOR EACH ROW
BEGIN
  UPDATE viaje SET cantidad_pasajeros = GREATEST(cantidad_pasajeros - 1, 0)
  WHERE id_viaje = OLD.id_viaje;
END;
