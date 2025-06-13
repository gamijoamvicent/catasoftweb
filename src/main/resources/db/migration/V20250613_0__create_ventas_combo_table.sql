-- Crear tabla de ventas de combos
CREATE TABLE ventas_combo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    combo_id BIGINT NOT NULL,
    valor_venta_usd DECIMAL(10,2) NOT NULL,
    valor_venta_bs DECIMAL(10,2) NOT NULL,
    tasa_conversion DECIMAL(10,2) NOT NULL,
    licoreria_id BIGINT NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    fecha_venta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agregar foreign keys a la tabla ventas_combo
ALTER TABLE ventas_combo
    ADD CONSTRAINT fk_ventas_combo_combo 
        FOREIGN KEY (combo_id) 
        REFERENCES combos(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE,
    ADD CONSTRAINT fk_ventas_combo_licoreria 
        FOREIGN KEY (licoreria_id) 
        REFERENCES licorerias(id) 
        ON DELETE RESTRICT 
        ON UPDATE CASCADE;

-- Crear índices para mejorar el rendimiento en búsquedas comunes
CREATE INDEX idx_ventas_combo_fecha ON ventas_combo(fecha_venta);
CREATE INDEX idx_ventas_combo_licoreria ON ventas_combo(licoreria_id);
CREATE INDEX idx_ventas_combo_combo ON ventas_combo(combo_id);
