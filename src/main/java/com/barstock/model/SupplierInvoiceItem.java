package com.barstock.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "supplier_invoice_items")
public class SupplierInvoiceItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore @ManyToOne(optional = false) private SupplierInvoice invoice;
    @ManyToOne(optional = false) private Product product;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitCost;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal lineTotal;

    public Long getId() { return id; }
    public SupplierInvoice getInvoice() { return invoice; }
    public void setInvoice(SupplierInvoice invoice) { this.invoice = invoice; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}

