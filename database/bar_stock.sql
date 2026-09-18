CREATE DATABASE IF NOT EXISTS bar_stock
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bar_stock;

CREATE TABLE app_users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(180) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER'
) ENGINE=InnoDB;

CREATE TABLE suppliers (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  contact_name VARCHAR(120),
  email VARCHAR(180),
  phone VARCHAR(40),
  tax_id VARCHAR(60),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_suppliers_name (name)
) ENGINE=InnoDB;

CREATE TABLE products (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  sku VARCHAR(50) NOT NULL,
  keg_size_litres INT,
  image_data LONGBLOB,
  image_content_type VARCHAR(255),
  image_version VARCHAR(255),
  name VARCHAR(140) NOT NULL,
  category VARCHAR(80) NOT NULL,
  unit VARCHAR(30) NOT NULL DEFAULT 'unidad',
  stock DECIMAL(12,3) NOT NULL DEFAULT 0,
  minimum_stock DECIMAL(12,3) NOT NULL DEFAULT 0,
  cost_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  selling_price DECIMAL(12,2) NOT NULL DEFAULT 0,
  supplier_id BIGINT UNSIGNED,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_products_sku (sku),
  KEY idx_products_category (category),
  KEY idx_products_supplier (supplier_id),
  CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
  CONSTRAINT chk_products_stock CHECK (stock >= 0),
  CONSTRAINT chk_products_prices CHECK (cost_price >= 0 AND selling_price >= 0)
) ENGINE=InnoDB;

CREATE TABLE product_supplier_skus (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT UNSIGNED NOT NULL,
  supplier_id BIGINT UNSIGNED NOT NULL,
  sku VARCHAR(50) NOT NULL,
  UNIQUE KEY uk_supplier_sku (supplier_id, sku),
  CONSTRAINT fk_supplier_sku_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_supplier_sku_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
) ENGINE=InnoDB;

CREATE TABLE supplier_invoices (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  invoice_number VARCHAR(80) NOT NULL,
  supplier_id BIGINT UNSIGNED NOT NULL,
  invoice_date DATE NOT NULL,
  status ENUM('PENDING', 'PAID', 'OVERDUE') NOT NULL DEFAULT 'PENDING',
  notes VARCHAR(500),
  total DECIMAL(14,2) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_invoice_supplier_number (supplier_id, invoice_number),
  KEY idx_invoices_date (invoice_date),
  CONSTRAINT fk_invoices_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
  CONSTRAINT chk_invoices_total CHECK (total >= 0)
) ENGINE=InnoDB;

CREATE TABLE supplier_invoice_items (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  supplier_sku VARCHAR(50),
  quantity DECIMAL(12,3) NOT NULL,
  unit_cost DECIMAL(12,2) NOT NULL,
  line_total DECIMAL(14,2) NOT NULL,
  CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES supplier_invoices(id) ON DELETE CASCADE,
  CONSTRAINT fk_invoice_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT chk_invoice_items_values CHECK (quantity > 0 AND unit_cost >= 0)
) ENGINE=InnoDB;

CREATE TABLE stock_movements (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT UNSIGNED NOT NULL,
  movement_type ENUM('PURCHASE', 'SALE', 'ADJUSTMENT', 'WASTE') NOT NULL,
  quantity_change DECIMAL(12,3) NOT NULL,
  reference_type VARCHAR(40),
  reference_id BIGINT UNSIGNED,
  reason VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_movements_product_date (product_id, created_at),
  CONSTRAINT fk_movements_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

INSERT INTO suppliers (name, contact_name, email, phone, tax_id) VALUES
('Emerald Drinks Ltd.', 'Aoife Murphy', 'orders@emeralddrinks.ie', '+353 1 555 0142', 'IE6388047V'),
('Dublin Craft Supply', 'Liam Kelly', 'sales@dublincraft.ie', '+353 1 555 0177', 'IE9216034A');

INSERT INTO products (sku, name, category, unit, keg_size_litres, stock, minimum_stock, cost_price, selling_price, supplier_id) VALUES
('BEER-001', 'Guinness Keg 50L', 'Beer', 'keg', 50, 400, 150, 178.00, 520.00, 1),
('BEER-002', 'Heineken Keg 50L', 'Beer', 'keg', 50, 250, 150, 165.00, 490.00, 1),
('SPIR-001', 'Jameson 700ml', 'Spirits', 'bottle', NULL, 18, 6, 24.50, 112.00, 2),
('MIX-001', 'Tonic Water 200ml', 'Mixers', 'case', NULL, 4, 5, 18.00, 48.00, 2);

INSERT INTO product_supplier_skus (product_id,supplier_id,sku) SELECT id,supplier_id,sku FROM products WHERE supplier_id IS NOT NULL;
