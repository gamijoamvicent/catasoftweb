-- Crear tabla de combos
CREATE TABLE combos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    tipo_tasa VARCHAR(50) NOT NULL,
    activo BOOLEAN DEFAULT true,
    licoreria_id BIGINT NOT NULL,
    creado_por BIGINT NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agregar foreign keys a la tabla combos
ALTER TABLE combos
    ADD CONSTRAINT fk_combo_licoreria 
        FOREIGN KEY (licoreria_id) 
        REFERENCES licorerias(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    ADD CONSTRAINT fk_combo_usuario 
        FOREIGN KEY (creado_por) 
        REFERENCES usuarios(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE;

-- Crear tabla de relación entre combos y productos
CREATE TABLE combo_productos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    combo_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agregar foreign keys a la tabla combo_productos
ALTER TABLE combo_productos
    ADD CONSTRAINT fk_combo_productos_combo 
        FOREIGN KEY (combo_id) 
        REFERENCES combos(id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    ADD CONSTRAINT fk_combo_productos_producto 
        FOREIGN KEY (producto_id) 
        REFERENCES producto(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE;

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_combo_productos_combo_id ON combo_productos(combo_id);
CREATE INDEX idx_combo_productos_producto_id ON combo_productos(producto_id);
CREATE INDEX idx_combos_licoreria ON combos(licoreria_id);
CREATE INDEX idx_combos_activo ON combos(activo);
