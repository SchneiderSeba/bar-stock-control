-- Ejecutar una sola vez sobre la versión anterior con stock de kegs por unidades.
USE bar_stock;
ALTER TABLE products ADD COLUMN keg_size_litres INT;
-- Revisar tamaños antes de convertir: se infieren del nombre; si no aparece, se usa 50 L.
UPDATE products SET keg_size_litres = CASE
  WHEN LOWER(name) REGEXP '30[[:space:]]*l' THEN 30
  WHEN LOWER(name) REGEXP '20[[:space:]]*l' THEN 20
  ELSE 50 END WHERE LOWER(TRIM(unit)) = 'keg';
UPDATE stock_movements m JOIN products p ON p.id = m.product_id
  SET m.quantity_change = m.quantity_change * p.keg_size_litres
  WHERE p.keg_size_litres IS NOT NULL;
UPDATE products SET stock = stock * keg_size_litres,
  minimum_stock = minimum_stock * keg_size_litres, unit = 'keg'
  WHERE keg_size_litres IS NOT NULL;
-- Precios y líneas de facturas siguen expresados por keg. No se convierten.
