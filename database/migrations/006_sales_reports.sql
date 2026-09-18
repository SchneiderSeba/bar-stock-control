-- Ejecutar una sola vez, después de 005.
USE bar_stock;
ALTER TABLE products ADD COLUMN volume_ml INT;
ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE products MODIFY stock DECIMAL(18,6) NOT NULL DEFAULT 0;
ALTER TABLE stock_movements MODIFY quantity_change DECIMAL(18,6) NOT NULL;
-- Configura volume_ml con la capacidad real de cada botella/caja.
CREATE TABLE sales_reports (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  import_key VARCHAR(64) NOT NULL UNIQUE,
  period VARCHAR(20) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  uploaded_at DATETIME(6) NOT NULL,
  applied_at DATETIME(6),
  row_count INT NOT NULL DEFAULT 0,
  product_count INT NOT NULL DEFAULT 0,
  csv_data LONGBLOB,
  lines_json LONGTEXT,
  errors_json LONGTEXT
) ENGINE=InnoDB;
