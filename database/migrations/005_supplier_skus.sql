-- Ejecutar una sola vez en bases existentes, después de 004.
USE bar_stock;
CREATE TABLE product_supplier_skus (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT UNSIGNED NOT NULL,
  supplier_id BIGINT UNSIGNED NOT NULL,
  sku VARCHAR(50) NOT NULL,
  UNIQUE KEY uk_supplier_sku (supplier_id, sku),
  CONSTRAINT fk_supplier_sku_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_supplier_sku_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
) ENGINE=InnoDB;
INSERT INTO product_supplier_skus (product_id,supplier_id,sku)
SELECT id,supplier_id,sku FROM products WHERE supplier_id IS NOT NULL;
ALTER TABLE supplier_invoice_items ADD COLUMN supplier_sku VARCHAR(50);
UPDATE supplier_invoice_items item
JOIN supplier_invoices invoice ON invoice.id=item.invoice_id
JOIN product_supplier_skus mapping ON mapping.product_id=item.product_id AND mapping.supplier_id=invoice.supplier_id
SET item.supplier_sku=mapping.sku;
