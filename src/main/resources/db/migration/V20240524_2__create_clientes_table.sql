-- Crear tabla de clientes
CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cedula VARCHAR(15) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    telefono VARCHAR(15),
    direccion TEXT,
    credito_maximo DOUBLE NOT NULL DEFAULT 0,
    credito_disponible DOUBLE NOT NULL DEFAULT 0,
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    licoreria_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cliente_licoreria FOREIGN KEY (licoreria_id) REFERENCES licorerias(id),
    CONSTRAINT uk_cedula_licoreria UNIQUE (cedula, licoreria_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_cliente_licoreria ON clientes(licoreria_id);
CREATE INDEX idx_cliente_estado ON clientes(estado);
CREATE INDEX idx_cliente_nombre_apellido ON clientes(nombre, apellido); 