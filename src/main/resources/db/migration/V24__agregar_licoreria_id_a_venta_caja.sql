-- Añadir columna licoreria_id a la tabla venta_caja
ALTER TABLE venta_caja ADD COLUMN licoreria_id BIGINT;

-- Actualizar los registros existentes con el ID de licorería de la venta relacionada
UPDATE venta_caja vc
INNER JOIN ventas v ON vc.venta_id = v.id
SET vc.licoreria_id = v.licoreria_id;

-- Crear índice para mejorar el rendimiento de las consultas por licoreria_id
CREATE INDEX idx_venta_caja_licoreria_id ON venta_caja(licoreria_id);