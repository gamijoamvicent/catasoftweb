-- Crear tabla de créditos
CREATE TABLE IF NOT EXISTS creditos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    venta_id BIGINT NOT NULL,
    licoreria_id BIGINT NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    monto_pagado DECIMAL(10,2) NOT NULL DEFAULT 0,
    saldo_pendiente DECIMAL(10,2) NOT NULL,
    fecha_limite_pago DATETIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_credito_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_credito_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_credito_licoreria FOREIGN KEY (licoreria_id) REFERENCES licorerias(id),
    CONSTRAINT uk_credito_venta UNIQUE (venta_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_credito_cliente ON creditos(cliente_id);
CREATE INDEX idx_credito_licoreria ON creditos(licoreria_id);
CREATE INDEX idx_credito_estado ON creditos(estado);

-- Modificar la tabla de ventas para agregar el tipo de venta
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS tipo_venta VARCHAR(20) NOT NULL DEFAULT 'CONTADO';
ALTER TABLE ventas ADD COLUMN IF NOT EXISTS cliente_id BIGINT NULL;
ALTER TABLE ventas ADD CONSTRAINT fk_venta_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id); 