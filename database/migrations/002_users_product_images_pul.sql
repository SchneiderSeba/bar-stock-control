-- Ejecutar una sola vez sobre la base de datos de la versión anterior.
USE bar_stock;
CREATE TABLE app_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(180) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER'
) ENGINE=InnoDB;
ALTER TABLE products
  ADD COLUMN pul_code VARCHAR(80),
  ADD COLUMN image_data LONGBLOB,
  ADD COLUMN image_content_type VARCHAR(255),
  ADD COLUMN image_version VARCHAR(255);
-- Reemplazar los marcadores temporales por los PUL reales de los proveedores.
UPDATE products SET pul_code = CONCAT('PENDING-', sku);
ALTER TABLE products MODIFY pul_code VARCHAR(80) NOT NULL;
ALTER TABLE supplier_invoice_items MODIFY line_total DECIMAL(14,2) NOT NULL;
