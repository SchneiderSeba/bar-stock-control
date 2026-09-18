package com.barstock.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name="product_supplier_skus", uniqueConstraints=@UniqueConstraint(columnNames={"supplier_id","sku"}))
public class ProductSupplierSku {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @JsonIgnore @ManyToOne(optional=false) private Product product;
    @ManyToOne(optional=false) private Supplier supplier;
    @Column(nullable=false,length=50) private String sku;
    public Long getId() { return id; }
    public void setProduct(Product product) { this.product=product; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier=supplier; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku=sku; }
}
