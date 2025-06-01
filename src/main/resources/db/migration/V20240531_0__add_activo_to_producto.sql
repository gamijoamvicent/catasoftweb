-- Verificar si la columna 'activo' ya existe antes de agregarla
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'papeleria_db' AND table_name = 'producto' AND column_name = 'activo');
SET @add_column = IF(@column_exists = 0,
    'ALTER TABLE producto ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE',
    'SELECT "La columna ''activo'' ya existe"');

PREPARE stmt_add_column FROM @add_column;
EXECUTE stmt_add_column;
DEALLOCATE PREPARE stmt_add_column;
