-- Añadir columna licoreria_id a la tabla combo_productos
ALTER TABLE combo_productos ADD COLUMN licoreria_id BIGINT;

-- Actualizar la columna licoreria_id con el valor correcto de la licorería del combo
UPDATE combo_productos cp
SET licoreria_id = (SELECT c.licoreria_id FROM combos c WHERE c.id = cp.combo_id);

-- Crear índice para mejorar el rendimiento de las consultas
CREATE INDEX idx_combo_productos_licoreria_id ON combo_productos(licoreria_id);

-- Actualizar constraint para garantizar la integridad referencial
ALTER TABLE combo_productos
ADD CONSTRAINT fk_combo_productos_licoreria
FOREIGN KEY (licoreria_id) REFERENCES licorerias(id);
