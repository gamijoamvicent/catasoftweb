CREATE TABLE IF NOT EXISTS pagos_credito (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credito_id BIGINT NOT NULL,
    licoreria_id BIGINT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_pago DATETIME NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    referencia VARCHAR(50),
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_credito FOREIGN KEY (credito_id) REFERENCES creditos(id),
    CONSTRAINT fk_pago_licoreria FOREIGN KEY (licoreria_id) REFERENCES licorerias(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pago_credito ON pagos_credito(credito_id);
CREATE INDEX idx_pago_licoreria ON pagos_credito(licoreria_id);
CREATE INDEX idx_pago_fecha ON pagos_credito(fecha_pago); 