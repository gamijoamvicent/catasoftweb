-- Migración para agregar el campo is_deleted a la tabla de facturas
ALTER TABLE facturas ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
