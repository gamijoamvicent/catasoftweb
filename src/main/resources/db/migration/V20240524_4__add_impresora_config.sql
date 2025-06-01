-- Verificar si la tabla ya existe antes de crearla
CREATE TABLE IF NOT EXISTS configuraciones_impresora (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    licoreria_id BIGINT NOT NULL,
    puerto_com VARCHAR(50),
    deteccion_automatica BOOLEAN DEFAULT TRUE,
    velocidad_baudios INT DEFAULT 9600,
    bits_datos INT DEFAULT 8,
    bits_parada INT DEFAULT 1,
    paridad VARCHAR(10) DEFAULT 'NONE',
    ancho_papel INT DEFAULT 80,
    dpi INT DEFAULT 203,
    corte_automatico BOOLEAN DEFAULT TRUE,
    imprimir_logo BOOLEAN DEFAULT FALSE,
    ruta_logo VARCHAR(255),
    activa BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (licoreria_id) REFERENCES licorerias(id)
);
