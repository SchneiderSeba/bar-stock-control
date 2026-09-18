package com.barstock.model;
import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name="sales_reports")
public class SalesReport {
    @com.fasterxml.jackson.annotation.JsonIgnore @Lob @Column(columnDefinition="LONGBLOB") public byte[] csvData;
    public enum Period { DAILY,WEEKLY,MONTHLY }
    public enum Status { READY,REJECTED,APPLIED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,length=255) public String fileName;
    @Column(nullable=false,unique=true,length=64) public String importKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Period period;
    @Column(nullable=false) public LocalDate startDate;
    @Column(nullable=false) public LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Status status;
    @Column(nullable=false) public Instant uploadedAt=Instant.now();
    public Instant appliedAt;
    @Lob @Column(columnDefinition="LONGTEXT") public String linesJson="[]";
    @Lob @Column(columnDefinition="LONGTEXT") public String errorsJson="[]";
    public int rowCount;
    public int productCount;
}
