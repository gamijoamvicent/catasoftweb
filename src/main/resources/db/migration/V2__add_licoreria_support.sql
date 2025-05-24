-- Crear tabla de licorerías
CREATE TABLE IF NOT EXISTS licoreria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(200) NOT NULL,
    telefono VARCHAR(20),
    ip_local VARCHAR(15),
    configuracion_impresora VARCHAR(200),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Crear licorería por defecto
INSERT INTO licoreria (nombre, direccion, telefono, activo)
VALUES ('Licorería Principal', 'Dirección Principal', '000-000-0000', true);

-- Obtener el ID de la licorería por defecto
SET @licoreria_default_id = LAST_INSERT_ID();

-- Agregar columna licoreria_id a las tablas existentes
ALTER TABLE usuario ADD COLUMN licoreria_id BIGINT;
ALTER TABLE producto ADD COLUMN licoreria_id BIGINT;
ALTER TABLE precio_dolar ADD COLUMN licoreria_id BIGINT;

-- Asignar licorería por defecto a registros existentes
UPDATE usuario SET licoreria_id = @licoreria_default_id WHERE licoreria_id IS NULL;
UPDATE producto SET licoreria_id = @licoreria_default_id WHERE licoreria_id IS NULL;
UPDATE precio_dolar SET licoreria_id = @licoreria_default_id WHERE licoreria_id IS NULL;

-- Hacer las columnas NOT NULL después de asignar valores por defecto
ALTER TABLE usuario MODIFY licoreria_id BIGINT NOT NULL;
ALTER TABLE producto MODIFY licoreria_id BIGINT NOT NULL;
ALTER TABLE precio_dolar MODIFY licoreria_id BIGINT NOT NULL;

-- Agregar restricciones de clave foránea
ALTER TABLE usuario
    ADD CONSTRAINT fk_usuario_licoreria
    FOREIGN KEY (licoreria_id) REFERENCES licoreria(id);

ALTER TABLE producto
    ADD CONSTRAINT fk_producto_licoreria
    FOREIGN KEY (licoreria_id) REFERENCES licoreria(id);

ALTER TABLE precio_dolar
    ADD CONSTRAINT fk_precio_dolar_licoreria
    FOREIGN KEY (licoreria_id) REFERENCES licoreria(id);

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_usuario_licoreria ON usuario(licoreria_id);
CREATE INDEX idx_producto_licoreria ON producto(licoreria_id);
CREATE INDEX idx_precio_dolar_licoreria ON precio_dolar(licoreria_id);

-- Actualizar el usuario SUPER_ADMIN existente para que no tenga licorería asignada
UPDATE usuario 
SET licoreria_id = NULL 
WHERE rol = 'SUPER_ADMIN';

-- Modificar la columna licoreria_id en usuario para permitir NULL
ALTER TABLE usuario MODIFY licoreria_id BIGINT NULL; 