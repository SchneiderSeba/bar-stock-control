package com.barstock.repository;
import com.barstock.model.ProductSupplierSku;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProductSupplierSkuRepository extends JpaRepository<ProductSupplierSku,Long> {
    boolean existsBySupplierIdAndSkuAndProductIdNot(Long supplierId,String sku,Long productId);
}
