-- Eliminar tablas si existen (en orden inverso a las dependencias)
DROP TABLE IF EXISTS detalle_venta;
DROP TABLE IF EXISTS ventas;

-- Crear tabla de ventas
CREATE TABLE IF NOT EXISTS ventas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_venta DATETIME NOT NULL,
    total_venta DECIMAL(10,2) NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    licoreria_id BIGINT NOT NULL,
    FOREIGN KEY (licoreria_id) REFERENCES licorerias(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear tabla de detalle de ventas
CREATE TABLE IF NOT EXISTS detalle_venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (producto_id) REFERENCES producto(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_ventas_licoreria_fecha ON ventas(licoreria_id, fecha_venta);
CREATE INDEX idx_detalle_venta_venta ON detalle_venta(venta_id);
CREATE INDEX idx_detalle_venta_producto ON detalle_venta(producto_id); 