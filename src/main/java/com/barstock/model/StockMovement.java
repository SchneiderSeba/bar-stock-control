package com.barstock.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    public enum Type { PURCHASE, SALE, ADJUSTMENT, WASTE }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Product product;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Type movementType;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantityChange;
    private String referenceType;
    private Long referenceId;
    private String reason;
    @Column(nullable = false) private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Type getMovementType() { return movementType; }
    public void setMovementType(Type movementType) { this.movementType = movementType; }
    public BigDecimal getQuantityChange() { return quantityChange; }
    public void setQuantityChange(BigDecimal quantityChange) { this.quantityChange = quantityChange; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
}

