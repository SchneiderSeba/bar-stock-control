package com.barstock.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, unique = true)
    private String sku;
    @NotBlank @Column(nullable = false)
    private String name;
    @NotBlank @Column(nullable = false)
    private String category;
    @NotBlank @Column(nullable = false)
    private String unit = "unit";
    @DecimalMin("0.0") @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal stock = BigDecimal.ZERO;
    @DecimalMin("0.0") @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal minimumStock = BigDecimal.ZERO;
    @DecimalMin("0.0") @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;
    @DecimalMin("0.0") @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.EAGER)
    private Supplier supplier;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) { this.stock = stock; }
    public BigDecimal getMinimumStock() { return minimumStock; }
    public void setMinimumStock(BigDecimal minimumStock) { this.minimumStock = minimumStock; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

