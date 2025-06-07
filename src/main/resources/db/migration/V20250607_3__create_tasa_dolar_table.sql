CREATE TABLE tasa_dolar (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tasa DECIMAL(10,2) NOT NULL,
    fecha DATETIME NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE
);

-- Insertar tasa inicial
INSERT INTO tasa_dolar (tasa, fecha, activa) VALUES (35.00, NOW(), true); 