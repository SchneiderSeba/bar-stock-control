-- Solo para instalaciones que ya ejecutaron la migración 002.
-- Elimina el código PUL; los SKU, productos e imágenes se conservan.
USE bar_stock;
ALTER TABLE products DROP COLUMN pul_code;
