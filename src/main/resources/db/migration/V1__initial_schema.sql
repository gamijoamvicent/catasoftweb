-- Crear tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(100),
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Crear tabla de productos
CREATE TABLE IF NOT EXISTS producto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    codigo_unico VARCHAR(50) UNIQUE,
    precio_venta DOUBLE NOT NULL,
    precio_costo DOUBLE NOT NULL,
    cantidad INT NOT NULL DEFAULT 0,
    categoria VARCHAR(50),
    marca VARCHAR(50),
    proveedor VARCHAR(100),
    color VARCHAR(50),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Crear tabla de precio del dólar
CREATE TABLE IF NOT EXISTS preciodolar (
    id_precio_dolar BIGINT AUTO_INCREMENT PRIMARY KEY,
    precio_dolar DOUBLE NOT NULL,
    fecha_dolar TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tipo_tasa VARCHAR(20) DEFAULT 'BCV'
);

-- Insertar usuario SUPER_ADMIN por defecto (password: admin123)
INSERT INTO usuarios (username, password, rol, activo)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'SUPER_ADMIN', true)
ON DUPLICATE KEY UPDATE username = username;

-- Insertar precio del dólar inicial
INSERT INTO preciodolar (precio_dolar, tipo_tasa)
VALUES (1.00, 'BCV')
ON DUPLICATE KEY UPDATE id_precio_dolar = id_precio_dolar; 