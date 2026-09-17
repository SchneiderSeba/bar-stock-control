package com.barstock.repository;
import com.barstock.model.SupplierInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
public interface InvoiceRepository extends JpaRepository<SupplierInvoice, Long> {
    @EntityGraph(attributePaths = {"supplier", "items", "items.product", "items.product.supplier"})
    List<SupplierInvoice> findAllByOrderByInvoiceDateDescIdDesc();
}
