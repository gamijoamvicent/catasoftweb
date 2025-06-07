-- Crear tabla de combos
CREATE TABLE IF NOT EXISTS combos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    precio DOUBLE NOT NULL,
    licoreria_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (licoreria_id) REFERENCES licorerias(id)
);

-- Crear tabla de productos en combos
CREATE TABLE IF NOT EXISTS combo_productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    combo_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    FOREIGN KEY (combo_id) REFERENCES combos(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
); 