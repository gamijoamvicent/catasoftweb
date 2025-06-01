-- Crear tabla clientes
CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(200),
    telefono VARCHAR(20),
    licoreria_id BIGINT NOT NULL,
    FOREIGN KEY (licoreria_id) REFERENCES licorerias(id)
);

-- Crear índices para mejorar el rendimiento
CREATE INDEX IF NOT EXISTS idx_cliente_licoreria ON clientes(licoreria_id);
