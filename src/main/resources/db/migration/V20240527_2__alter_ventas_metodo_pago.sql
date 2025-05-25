-- Modificar la columna metodo_pago para aceptar los nuevos valores
ALTER TABLE ventas MODIFY COLUMN metodo_pago VARCHAR(20); 