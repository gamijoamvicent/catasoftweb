-- Script de datos de prueba para sistema de licorería
-- Creación de Licorerías
INSERT INTO licorerias (nombre, direccion, telefono, email, estado) VALUES
('Licorería El Barril', 'Av. 10 de Agosto N45-12', '0987654321', 'barril@email.com', 'ACTIVO'),
('Licores Express', 'Calle Juan León Mera 234', '0998765432', 'express@email.com', 'ACTIVO'),
('La Bodega Premium', 'Av. República E7-123', '0976543210', 'bodega@email.com', 'ACTIVO'),
('Licorería Central', 'Av. 6 de Diciembre N34-45', '0965432109', 'central@email.com', 'ACTIVO'),
('El Rincón del Licor', 'Av. Amazonas N23-56', '0954321098', 'rincon@email.com', 'ACTIVO');

-- Creación de Usuarios Administradores
INSERT INTO usuarios (username, password, nombre, apellido, email, telefono, estado, rol, licoreria_id) VALUES
('admin1', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Juan', 'Pérez', 'juan@email.com', '0987654321', 'ACTIVO', 'ROLE_ADMIN', 1),
('admin2', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'María', 'López', 'maria@email.com', '0998765432', 'ACTIVO', 'ROLE_ADMIN', 2),
('admin3', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Carlos', 'González', 'carlos@email.com', '0976543210', 'ACTIVO', 'ROLE_ADMIN', 3),
('admin4', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Ana', 'Martínez', 'ana@email.com', '0965432109', 'ACTIVO', 'ROLE_ADMIN', 4),
('admin5', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Pedro', 'Sánchez', 'pedro@email.com', '0954321098', 'ACTIVO', 'ROLE_ADMIN', 5);

-- Creación de Usuarios Cajeros
INSERT INTO usuarios (username, password, nombre, apellido, email, telefono, estado, rol, licoreria_id) VALUES
('cajero1', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Luis', 'García', 'luis@email.com', '0912345678', 'ACTIVO', 'ROLE_CAJERO', 1),
('cajero2', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Carmen', 'Rodríguez', 'carmen@email.com', '0923456789', 'ACTIVO', 'ROLE_CAJERO', 2),
('cajero3', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Jorge', 'Flores', 'jorge@email.com', '0934567890', 'ACTIVO', 'ROLE_CAJERO', 3),
('cajero4', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Diana', 'Torres', 'diana@email.com', '0945678901', 'ACTIVO', 'ROLE_CAJERO', 4),
('cajero5', '$2a$10$jBjKd1Y48FSo5WDBij7Uz.mUjMgyVmK3j8hclJNLwfHhd3N0UOtXG', 'Roberto', 'Vargas', 'roberto@email.com', '0956789012', 'ACTIVO', 'ROLE_CAJERO', 5);

-- Creación de Clientes (3 por licorería)
INSERT INTO clientes (cedula, nombre, apellido, email, telefono, direccion, estado, limite_credito, credito_disponible, licoreria_id) VALUES
('1234567890', 'Andrea', 'Morales', 'andrea@email.com', '0987123456', 'Calle A N12-34', 'ACTIVO', 500.00, 500.00, 1),
('1234567891', 'Miguel', 'Castro', 'miguel@email.com', '0987123457', 'Calle B N23-45', 'ACTIVO', 300.00, 300.00, 1),
('1234567892', 'Patricia', 'Ruiz', 'patricia@email.com', '0987123458', 'Calle C N34-56', 'ACTIVO', 400.00, 400.00, 1),
('1234567893', 'Fernando', 'Díaz', 'fernando@email.com', '0987123459', 'Calle D N45-67', 'ACTIVO', 600.00, 600.00, 2),
('1234567894', 'Lucía', 'Paredes', 'lucia@email.com', '0987123460', 'Calle E N56-78', 'ACTIVO', 350.00, 350.00, 2),
('1234567895', 'Ricardo', 'Mendoza', 'ricardo@email.com', '0987123461', 'Calle F N67-89', 'ACTIVO', 450.00, 450.00, 2),
('1234567896', 'Sofía', 'Jiménez', 'sofia@email.com', '0987123462', 'Calle G N78-90', 'ACTIVO', 550.00, 550.00, 3),
('1234567897', 'Gabriel', 'Ortiz', 'gabriel@email.com', '0987123463', 'Calle H N89-01', 'ACTIVO', 320.00, 320.00, 3),
('1234567898', 'Valeria', 'Zambrano', 'valeria@email.com', '0987123464', 'Calle I N90-12', 'ACTIVO', 480.00, 480.00, 3),
('1234567899', 'Eduardo', 'Paz', 'eduardo@email.com', '0987123465', 'Calle J N01-23', 'ACTIVO', 520.00, 520.00, 4),
('1234567900', 'Carla', 'Vega', 'carla@email.com', '0987123466', 'Calle K N12-34', 'ACTIVO', 380.00, 380.00, 4),
('1234567901', 'Andrés', 'Luna', 'andres@email.com', '0987123467', 'Calle L N23-45', 'ACTIVO', 420.00, 420.00, 4),
('1234567902', 'Mónica', 'Reyes', 'monica@email.com', '0987123468', 'Calle M N34-56', 'ACTIVO', 580.00, 580.00, 5),
('1234567903', 'Daniel', 'Chávez', 'daniel@email.com', '0987123469', 'Calle N N45-67', 'ACTIVO', 340.00, 340.00, 5),
('1234567904', 'Isabel', 'Guerrero', 'isabel@email.com', '0987123470', 'Calle O N56-78', 'ACTIVO', 460.00, 460.00, 5);

-- Creación de Categorías
INSERT INTO categorias (nombre, descripcion, estado) VALUES
('Cervezas', 'Cervezas nacionales e importadas', 'ACTIVO'),
('Vinos', 'Vinos tintos, blancos y rosados', 'ACTIVO'),
('Whisky', 'Whisky de diferentes años y marcas', 'ACTIVO'),
('Ron', 'Ron nacional e importado', 'ACTIVO'),
('Vodka', 'Vodka de diferentes marcas', 'ACTIVO'),
('Tequila', 'Tequila de diferentes tipos', 'ACTIVO');

-- Creación de Productos (varios por categoría y licorería)
INSERT INTO productos (nombre, descripcion, precio_compra, precio_venta, stock, estado, categoria_id, licoreria_id) VALUES
('Cerveza Pilsener', 'Cerveza nacional 600ml', 1.00, 1.50, 100, 'ACTIVO', 1, 1),
('Cerveza Club', 'Cerveza nacional premium 600ml', 1.20, 1.80, 80, 'ACTIVO', 1, 1),
('Vino Casillero del Diablo', 'Vino tinto Cabernet Sauvignon 750ml', 12.00, 18.00, 30, 'ACTIVO', 2, 1),
('Whisky Jack Daniels', 'Whisky americano 750ml', 25.00, 35.00, 20, 'ACTIVO', 3, 1),
('Ron Abuelo', 'Ron añejo 750ml', 15.00, 22.00, 25, 'ACTIVO', 4, 1),
('Cerveza Corona', 'Cerveza mexicana 355ml', 1.50, 2.25, 90, 'ACTIVO', 1, 2),
('Vino Santa Rita', 'Vino tinto Merlot 750ml', 10.00, 15.00, 35, 'ACTIVO', 2, 2),
('Whisky Chivas Regal', 'Whisky escocés 12 años 750ml', 30.00, 45.00, 15, 'ACTIVO', 3, 2),
('Ron Flor de Caña', 'Ron 7 años 750ml', 18.00, 27.00, 20, 'ACTIVO', 4, 2),
('Vodka Absolut', 'Vodka sueco 750ml', 20.00, 30.00, 25, 'ACTIVO', 5, 2),
('Cerveza Heineken', 'Cerveza holandesa 355ml', 1.80, 2.70, 85, 'ACTIVO', 1, 3),
('Vino Concha y Toro', 'Vino tinto Carmenere 750ml', 11.00, 16.50, 40, 'ACTIVO', 2, 3),
('Whisky Johnnie Walker', 'Whisky escocés Black Label 750ml', 35.00, 52.50, 18, 'ACTIVO', 3, 3),
('Ron Bacardi', 'Ron blanco 750ml', 16.00, 24.00, 30, 'ACTIVO', 4, 3),
('Tequila José Cuervo', 'Tequila reposado 750ml', 22.00, 33.00, 20, 'ACTIVO', 6, 3),
('Cerveza Budweiser', 'Cerveza americana 355ml', 1.40, 2.10, 95, 'ACTIVO', 1, 4),
('Vino Trapiche', 'Vino tinto Malbec 750ml', 13.00, 19.50, 25, 'ACTIVO', 2, 4),
('Whisky Ballantine\'s', 'Whisky escocés 750ml', 28.00, 42.00, 22, 'ACTIVO', 3, 4),
('Ron Zacapa', 'Ron 23 años 750ml', 45.00, 67.50, 10, 'ACTIVO', 4, 4),
('Vodka Grey Goose', 'Vodka francés 750ml', 35.00, 52.50, 15, 'ACTIVO', 5, 4),
('Cerveza Stella Artois', 'Cerveza belga 355ml', 1.90, 2.85, 75, 'ACTIVO', 1, 5),
('Vino Navarro Correas', 'Vino tinto Cabernet 750ml', 14.00, 21.00, 28, 'ACTIVO', 2, 5),
('Whisky Macallan', 'Whisky escocés 12 años 750ml', 50.00, 75.00, 8, 'ACTIVO', 3, 5),
('Ron Diplomatico', 'Ron reserva exclusiva 750ml', 40.00, 60.00, 12, 'ACTIVO', 4, 5),
('Tequila Patrón', 'Tequila silver 750ml', 45.00, 67.50, 15, 'ACTIVO', 6, 5);

-- Nota: La contraseña hasheada ($2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbQGchql6b7KWlDJeie) corresponde a "password123" 