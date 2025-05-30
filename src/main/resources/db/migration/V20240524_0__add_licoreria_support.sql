-- Verificar si las tablas existen antes de proceder
SET @usuario_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'papeleria_db' AND table_name = 'usuarios');
SET @producto_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'papeleria_db' AND table_name = 'producto');
SET @precio_dolar_exists = (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'papeleria_db' AND table_name = 'preciodolar');

-- Crear tabla de licorerías
CREATE TABLE IF NOT EXISTS licorerias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    telefono VARCHAR(20),
    ip_local VARCHAR(15),
    configuracion_impresora VARCHAR(200),
    estado BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Crear licorería por defecto
INSERT INTO licorerias (nombre, direccion, telefono, estado)
VALUES ('Licorería Principal', 'Dirección Principal', '000-000-0000', true)
ON DUPLICATE KEY UPDATE id = id;

-- Obtener el ID de la licorería por defecto
SET @licoreria_default_id = (SELECT id FROM licorerias WHERE nombre = 'Licorería Principal' LIMIT 1);

-- Agregar columna licoreria_id a las tablas existentes solo si existen
SET @sql_usuario = IF(@usuario_exists > 0, 
    'ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS licoreria_id BIGINT',
    'SELECT "Tabla usuarios no existe"');
SET @sql_producto = IF(@producto_exists > 0,
    'ALTER TABLE producto ADD COLUMN IF NOT EXISTS licoreria_id BIGINT',
    'SELECT "Tabla producto no existe"');
SET @sql_precio_dolar = IF(@precio_dolar_exists > 0,
    'ALTER TABLE preciodolar ADD COLUMN IF NOT EXISTS licoreria_id BIGINT',
    'SELECT "Tabla preciodolar no existe"');

PREPARE stmt_usuario FROM @sql_usuario;
PREPARE stmt_producto FROM @sql_producto;
PREPARE stmt_precio_dolar FROM @sql_precio_dolar;

EXECUTE stmt_usuario;
EXECUTE stmt_producto;
EXECUTE stmt_precio_dolar;

DEALLOCATE PREPARE stmt_usuario;
DEALLOCATE PREPARE stmt_producto;
DEALLOCATE PREPARE stmt_precio_dolar;

-- Asignar licorería por defecto a registros existentes
SET @update_usuario = IF(@usuario_exists > 0,
    CONCAT('UPDATE usuarios SET licoreria_id = ', @licoreria_default_id, ' WHERE licoreria_id IS NULL'),
    'SELECT "Tabla usuarios no existe"');
SET @update_producto = IF(@producto_exists > 0,
    CONCAT('UPDATE producto SET licoreria_id = ', @licoreria_default_id, ' WHERE licoreria_id IS NULL'),
    'SELECT "Tabla producto no existe"');
SET @update_precio_dolar = IF(@precio_dolar_exists > 0,
    CONCAT('UPDATE preciodolar SET licoreria_id = ', @licoreria_default_id, ' WHERE licoreria_id IS NULL'),
    'SELECT "Tabla preciodolar no existe"');

PREPARE stmt_update_usuario FROM @update_usuario;
PREPARE stmt_update_producto FROM @update_producto;
PREPARE stmt_update_precio_dolar FROM @update_precio_dolar;

EXECUTE stmt_update_usuario;
EXECUTE stmt_update_producto;
EXECUTE stmt_update_precio_dolar;

DEALLOCATE PREPARE stmt_update_usuario;
DEALLOCATE PREPARE stmt_update_producto;
DEALLOCATE PREPARE stmt_update_precio_dolar;

-- Hacer las columnas NOT NULL después de asignar valores por defecto
SET @alter_usuario = IF(@usuario_exists > 0,
    'ALTER TABLE usuarios MODIFY licoreria_id BIGINT NULL',
    'SELECT "Tabla usuarios no existe"');
SET @alter_producto = IF(@producto_exists > 0,
    'ALTER TABLE producto MODIFY licoreria_id BIGINT NOT NULL',
    'SELECT "Tabla producto no existe"');
SET @alter_precio_dolar = IF(@precio_dolar_exists > 0,
    'ALTER TABLE preciodolar MODIFY licoreria_id BIGINT NOT NULL',
    'SELECT "Tabla preciodolar no existe"');

PREPARE stmt_alter_usuario FROM @alter_usuario;
PREPARE stmt_alter_producto FROM @alter_producto;
PREPARE stmt_alter_precio_dolar FROM @alter_precio_dolar;

EXECUTE stmt_alter_usuario;
EXECUTE stmt_alter_producto;
EXECUTE stmt_alter_precio_dolar;

DEALLOCATE PREPARE stmt_alter_usuario;
DEALLOCATE PREPARE stmt_alter_producto;
DEALLOCATE PREPARE stmt_alter_precio_dolar;

-- Agregar restricciones de clave foránea (sintaxis corregida para MariaDB)
SET @fk_usuario = IF(@usuario_exists > 0,
    'ALTER TABLE usuarios ADD CONSTRAINT fk_usuario_licoreria FOREIGN KEY (licoreria_id) REFERENCES licorerias(id) ON DELETE RESTRICT ON UPDATE CASCADE',
    'SELECT "Tabla usuarios no existe"');
SET @fk_producto = IF(@producto_exists > 0,
    'ALTER TABLE producto ADD CONSTRAINT fk_producto_licoreria FOREIGN KEY (licoreria_id) REFERENCES licorerias(id) ON DELETE RESTRICT ON UPDATE CASCADE',
    'SELECT "Tabla producto no existe"');
SET @fk_precio_dolar = IF(@precio_dolar_exists > 0,
    'ALTER TABLE preciodolar ADD CONSTRAINT fk_precio_dolar_licoreria FOREIGN KEY (licoreria_id) REFERENCES licorerias(id) ON DELETE RESTRICT ON UPDATE CASCADE',
    'SELECT "Tabla preciodolar no existe"');

PREPARE stmt_fk_usuario FROM @fk_usuario;
PREPARE stmt_fk_producto FROM @fk_producto;
PREPARE stmt_fk_precio_dolar FROM @fk_precio_dolar;

EXECUTE stmt_fk_usuario;
EXECUTE stmt_fk_producto;
EXECUTE stmt_fk_precio_dolar;

DEALLOCATE PREPARE stmt_fk_usuario;
DEALLOCATE PREPARE stmt_fk_producto;
DEALLOCATE PREPARE stmt_fk_precio_dolar;

-- Crear índices para mejorar el rendimiento
SET @idx_usuario = IF(@usuario_exists > 0,
    'CREATE INDEX IF NOT EXISTS idx_usuario_licoreria ON usuarios(licoreria_id)',
    'SELECT "Tabla usuarios no existe"');
SET @idx_producto = IF(@producto_exists > 0,
    'CREATE INDEX IF NOT EXISTS idx_producto_licoreria ON producto(licoreria_id)',
    'SELECT "Tabla producto no existe"');
SET @idx_precio_dolar = IF(@precio_dolar_exists > 0,
    'CREATE INDEX IF NOT EXISTS idx_precio_dolar_licoreria ON preciodolar(licoreria_id)',
    'SELECT "Tabla preciodolar no existe"');

PREPARE stmt_idx_usuario FROM @idx_usuario;
PREPARE stmt_idx_producto FROM @idx_producto;
PREPARE stmt_idx_precio_dolar FROM @idx_precio_dolar;

EXECUTE stmt_idx_usuario;
EXECUTE stmt_idx_producto;
EXECUTE stmt_idx_precio_dolar;

DEALLOCATE PREPARE stmt_idx_usuario;
DEALLOCATE PREPARE stmt_idx_producto;
DEALLOCATE PREPARE stmt_idx_precio_dolar;

-- Actualizar el usuario SUPER_ADMIN existente para que no tenga licorería asignada
SET @update_admin = IF(@usuario_exists > 0,
    'UPDATE usuarios SET licoreria_id = NULL WHERE rol = "SUPER_ADMIN"',
    'SELECT "Tabla usuarios no existe"');

PREPARE stmt_update_admin FROM @update_admin;
EXECUTE stmt_update_admin;
DEALLOCATE PREPARE stmt_update_admin; 