package com.barstock.api;

import com.barstock.model.*;
import com.barstock.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final InvoiceRepository invoices;
    private final StockMovementRepository movements;

    public ApiController(ProductRepository products, SupplierRepository suppliers, InvoiceRepository invoices, StockMovementRepository movements) {
        this.products = products; this.suppliers = suppliers; this.invoices = invoices; this.movements = movements;
    }

    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/products") public List<Product> listProducts() { return products.findAllByOrderByNameAsc(); }
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@Valid @RequestBody ProductInput input) { return products.save(toProduct(new Product(), input)); }
    @PutMapping("/products/{id}") public Product updateProduct(@PathVariable Long id, @Valid @RequestBody ProductInput input) {
        return products.save(toProduct(product(id), input));
    }
    @PostMapping("/products/{id}/adjust") @Transactional
    public Product adjustStock(@PathVariable Long id, @RequestBody StockAdjustment input) {
        Product product = product(id);
        BigDecimal next = product.getStock().add(input.quantity());
        if (next.signum() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        product.setStock(next);
        StockMovement movement = new StockMovement();
        movement.setProduct(product); movement.setMovementType(StockMovement.Type.ADJUSTMENT);
        movement.setQuantityChange(input.quantity()); movement.setReason(input.reason());
        movements.save(movement);
        return products.save(product);
    }

    @GetMapping("/suppliers") public List<Supplier> listSuppliers() { return suppliers.findAllByOrderByNameAsc(); }
    @PostMapping("/suppliers") @ResponseStatus(HttpStatus.CREATED)
    public Supplier createSupplier(@Valid @RequestBody Supplier supplier) { supplier.setId(null); return suppliers.save(supplier); }

    @GetMapping("/invoices") public List<SupplierInvoice> listInvoices() { return invoices.findAllByOrderByInvoiceDateDescIdDesc(); }
    @PostMapping("/invoices") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public SupplierInvoice createInvoice(@Valid @RequestBody InvoiceInput input) {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setInvoiceNumber(input.invoiceNumber());
        invoice.setInvoiceDate(input.invoiceDate() == null ? LocalDate.now() : input.invoiceDate());
        invoice.setSupplier(suppliers.findById(input.supplierId()).orElseThrow(() -> notFound("Supplier")));
        invoice.setStatus(input.status() == null ? SupplierInvoice.Status.PENDING : input.status());
        invoice.setNotes(input.notes());
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLineInput line : input.items()) {
            Product product = product(line.productId());
            SupplierInvoiceItem item = new SupplierInvoiceItem();
            item.setInvoice(invoice); item.setProduct(product); item.setQuantity(line.quantity()); item.setUnitCost(line.unitCost());
            item.setLineTotal(line.quantity().multiply(line.unitCost()));
            invoice.getItems().add(item);
            total = total.add(item.getLineTotal());
            product.setStock(product.getStock().add(line.quantity()));
            product.setCostPrice(line.unitCost());
            products.save(product);
        }
        invoice.setTotal(total);
        SupplierInvoice saved = invoices.save(invoice);
        saved.getItems().forEach(item -> {
            StockMovement movement = new StockMovement();
            movement.setProduct(item.getProduct()); movement.setMovementType(StockMovement.Type.PURCHASE);
            movement.setQuantityChange(item.getQuantity()); movement.setReferenceType("INVOICE"); movement.setReferenceId(saved.getId());
            movement.setReason("Supplier invoice " + saved.getInvoiceNumber()); movements.save(movement);
        });
        return saved;
    }

    @GetMapping("/dashboard") public Dashboard dashboard() {
        List<Product> all = products.findAll();
        BigDecimal stockValue = all.stream().map(p -> p.getStock().multiply(p.getCostPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal revenue = all.stream().map(p -> p.getStock().multiply(p.getSellingPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long low = all.stream().filter(p -> p.getStock().compareTo(p.getMinimumStock()) <= 0).count();
        BigDecimal purchases = invoices.findAll().stream().map(SupplierInvoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Dashboard(stockValue, revenue, revenue.subtract(stockValue), purchases, all.size(), low);
    }

    private Product product(Long id) { return products.findById(id).orElseThrow(() -> notFound("Product")); }
    private ResponseStatusException notFound(String type) { return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found"); }
    private Product toProduct(Product p, ProductInput i) {
        p.setSku(i.sku()); p.setName(i.name()); p.setCategory(i.category()); p.setUnit(i.unit());
        p.setStock(i.stock()); p.setMinimumStock(i.minimumStock()); p.setCostPrice(i.costPrice()); p.setSellingPrice(i.sellingPrice());
        p.setActive(i.active());
        p.setSupplier(i.supplierId() == null ? null : suppliers.findById(i.supplierId()).orElseThrow(() -> notFound("Supplier")));
        return p;
    }

    public record ProductInput(String sku, String name, String category, String unit, BigDecimal stock, BigDecimal minimumStock, BigDecimal costPrice, BigDecimal sellingPrice, Long supplierId, boolean active) {}
    public record StockAdjustment(BigDecimal quantity, String reason) {}
    public record InvoiceLineInput(Long productId, BigDecimal quantity, BigDecimal unitCost) {}
    public record InvoiceInput(String invoiceNumber, Long supplierId, LocalDate invoiceDate, SupplierInvoice.Status status, String notes, List<InvoiceLineInput> items) {}
    public record Dashboard(BigDecimal stockValue, BigDecimal potentialRevenue, BigDecimal potentialProfit, BigDecimal purchases, long productCount, long lowStockCount) {}
}

