-- Agrega el campo tipo_tasa a la tabla producto para identificar con qué tasa se registró cada producto
ALTER TABLE producto ADD COLUMN tipo_tasa VARCHAR(20) NOT NULL DEFAULT 'BCV';
