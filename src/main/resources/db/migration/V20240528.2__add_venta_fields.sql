-- Agregar campos faltantes a la tabla ventas
ALTER TABLE ventas
    ADD COLUMN IF NOT EXISTS total_venta_bs DECIMAL(10,2) AFTER total_venta,
    ADD COLUMN IF NOT EXISTS tipo_venta VARCHAR(20) NOT NULL DEFAULT 'CONTADO' AFTER metodo_pago;

-- Agregar campos faltantes a la tabla detalle_venta
ALTER TABLE detalle_venta
    ADD COLUMN IF NOT EXISTS tasa_cambio_usado DECIMAL(10,2) AFTER subtotal,
    ADD COLUMN IF NOT EXISTS subtotal_bolivares DECIMAL(10,2) AFTER tasa_cambio_usado,
    ADD COLUMN IF NOT EXISTS tipo_tasa_usado VARCHAR(20) AFTER subtotal_bolivares; 