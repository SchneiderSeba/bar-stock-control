package com.barstock.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier_invoices")
public class SupplierInvoice {
    public enum Status { PENDING, PAID, OVERDUE }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String invoiceNumber;
    @ManyToOne(optional = false) private Supplier supplier;
    @Column(nullable = false) private LocalDate invoiceDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.PENDING;
    private String notes;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierInvoiceItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<SupplierInvoiceItem> getItems() { return items; }
    public void setItems(List<SupplierInvoiceItem> items) { this.items = items; }
}

