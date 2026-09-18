package com.barstock.config;

import com.barstock.model.Product;
import com.barstock.model.Supplier;
import com.barstock.model.ProductSupplierSku;
import com.barstock.repository.ProductRepository;
import com.barstock.repository.SupplierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;

@Configuration
public class DemoDataConfig {
    @Bean CommandLineRunner demoData(ProductRepository products, SupplierRepository suppliers) {
        return args -> {
            if (products.count() > 0) return;
            Supplier emerald = supplier("Emerald Drinks Ltd.", "Aoife Murphy", "orders@emeralddrinks.ie", "+353 1 555 0142", suppliers);
            Supplier craft = supplier("Dublin Craft Supply", "Liam Kelly", "sales@dublincraft.ie", "+353 1 555 0177", suppliers);
            product("BEER-001", "Guinness Keg 50L", "Beer", "keg", "8", "3", "178", "520", emerald, products);
            product("BEER-002", "Heineken Keg 50L", "Beer", "keg", "5", "3", "165", "490", emerald, products);
            product("SPIR-001", "Jameson 700ml", "Spirits", "bottle", "18", "6", "24.50", "112", craft, products);
            product("MIX-001", "Tonic Water 200ml", "Mixers", "case", "4", "5", "18", "48", craft, products);
        };
    }
    private Supplier supplier(String n, String c, String e, String p, SupplierRepository r) { Supplier s = new Supplier(); s.setName(n); s.setContactName(c); s.setEmail(e); s.setPhone(p); return r.save(s); }
    private void product(String sku, String n, String c, String u, String st, String min, String cost, String sell, Supplier s, ProductRepository r) { Product p = new Product(); p.setSku(sku); p.setName(n); p.setCategory(c); p.setUnit(u); p.setKegSizeLitres("keg".equals(u) ? 50 : null); BigDecimal factor = "keg".equals(u) ? BigDecimal.valueOf(50) : BigDecimal.ONE; p.setStock(new BigDecimal(st).multiply(factor)); p.setMinimumStock(new BigDecimal(min).multiply(factor)); p.setCostPrice(new BigDecimal(cost)); p.setSellingPrice(new BigDecimal(sell)); ProductSupplierSku link=new ProductSupplierSku(); link.setSupplier(s); link.setSku(sku); p.replaceSupplierSkus(java.util.List.of(link)); r.save(p); }
}
