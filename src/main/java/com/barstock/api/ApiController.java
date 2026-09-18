package com.barstock.api;

import com.barstock.model.*;
import com.barstock.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
import java.util.Arrays;
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
    private final ProductSupplierSkuRepository supplierSkus;

    public ApiController(ProductRepository products, SupplierRepository suppliers, InvoiceRepository invoices, StockMovementRepository movements, ProductSupplierSkuRepository supplierSkus) {
        this.supplierSkus=supplierSkus;
        this.products = products; this.suppliers = suppliers; this.invoices = invoices; this.movements = movements;
    }

    @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok"); }

    @GetMapping("/products") public List<Product> listProducts() { return products.findAllByOrderByNameAsc(); }
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    @Transactional public Product createProduct(@Valid @RequestBody ProductInput input) { return products.save(toProduct(new Product(), input)); }
    @PutMapping("/products/{id}") @Transactional public Product updateProduct(@PathVariable Long id, @Valid @RequestBody ProductInput input) {
        return products.save(toProduct(product(id), input));
    }
    @GetMapping("/products/{id}/image") public ResponseEntity<byte[]> image(@PathVariable Long id) {
        Product p = product(id);
        if (p.getImageData() == null) throw notFound("Image");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.getImageContentType()))
                .header("Cache-Control", "private, no-cache").body(p.getImageData());
    }
    @PostMapping(value = "/products/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Product uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe pesar como máximo 2 MB");
        byte[] data = file.getBytes();
        String type;
        if (data.length >= 8 && Arrays.equals(Arrays.copyOf(data, 8), new byte[]{(byte)137,80,78,71,13,10,26,10})) type = "image/png";
        else if (data.length >= 3 && data[0] == (byte)255 && data[1] == (byte)216 && data[2] == (byte)255) type = "image/jpeg";
        else if (data.length >= 12 && data[0]=='R' && data[1]=='I' && data[2]=='F' && data[3]=='F'
                && data[8]=='W' && data[9]=='E' && data[10]=='B' && data[11]=='P') type = "image/webp";
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen PNG, JPEG o WebP");
        Product p = product(id); p.setImageData(data); p.setImageContentType(type); p.setImageVersion(UUID.randomUUID().toString());
        return products.save(p);
    }
    @DeleteMapping("/products/{id}/image") public Product deleteImage(@PathVariable Long id) {
        Product p = product(id); p.setImageData(null); p.setImageContentType(null); p.setImageVersion(null); return products.save(p);
    }
    @PostMapping("/products/{id}/adjust") @Transactional
    public Product adjustStock(@PathVariable Long id, @RequestBody StockAdjustment input) {
        if (input.quantity() == null || input.quantity().signum() >= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las entradas de stock deben registrarse mediante una factura. Para una salida, ingresa una cantidad negativa");
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
    @PutMapping("/suppliers/{id}") public Supplier updateSupplier(@PathVariable Long id,@Valid @RequestBody Supplier input) {
        Supplier supplier=suppliers.findById(id).orElseThrow(() -> notFound("Supplier"));
        supplier.setName(input.getName()); supplier.setContactName(input.getContactName());
        supplier.setEmail(input.getEmail()); supplier.setPhone(input.getPhone()); supplier.setTaxId(input.getTaxId());
        return suppliers.save(supplier);
    }

    @GetMapping("/invoices") @Transactional public List<SupplierInvoice> listInvoices() {
        List<SupplierInvoice> result=invoices.findAllByOrderByInvoiceDateDescIdDesc();
        result.forEach(invoice -> invoice.getItems().forEach(item -> item.getProduct().getSupplierSkus().size()));
        return result;
    }
    @PostMapping("/invoices") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public SupplierInvoice createInvoice(@Valid @RequestBody InvoiceInput input) {
        if (invoices.existsBySupplierIdAndInvoiceNumber(input.supplierId(), input.invoiceNumber().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una factura con ese número para el proveedor");
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setInvoiceNumber(input.invoiceNumber().trim());
        invoice.setInvoiceDate(input.invoiceDate() == null ? LocalDate.now() : input.invoiceDate());
        invoice.setSupplier(suppliers.findById(input.supplierId()).orElseThrow(() -> notFound("Supplier")));
        invoice.setStatus(input.status() == null ? SupplierInvoice.Status.PENDING : input.status());
        invoice.setNotes(input.notes());
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLineInput line : input.items()) {
            Product product = product(line.productId());
            String supplierSku=line.supplierSku();
            if(supplierSku != null) {
                supplierSku=supplierSku.trim();
                String code=supplierSku;
                if(product.getSupplierSkus().stream().noneMatch(entry -> entry.getSupplier().getId().equals(input.supplierId()) && entry.getSku().equals(code)))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El SKU no está asignado a este producto y proveedor");
            } else {
                supplierSku=product.getSupplierSkus().stream().filter(entry -> entry.getSupplier().getId().equals(input.supplierId()))
                    .map(ProductSupplierSku::getSku).findFirst().orElse(null);
            }
            SupplierInvoiceItem item = new SupplierInvoiceItem();
            item.setSupplierSku(supplierSku);
            item.setInvoice(invoice); item.setProduct(product); item.setQuantity(line.quantity()); item.setUnitCost(line.unitCost());
            item.setLineTotal(line.quantity().multiply(line.unitCost()));
            invoice.getItems().add(item);
            total = total.add(item.getLineTotal());
            BigDecimal stockReceived = "keg".equals(product.getUnit())
                    ? line.quantity().multiply(BigDecimal.valueOf(product.getKegSizeLitres())) : line.quantity();
            product.setStock(product.getStock().add(stockReceived));
            product.setCostPrice(line.unitCost());
            products.save(product);
        }
        invoice.setTotal(total);
        SupplierInvoice saved = invoices.save(invoice);
        saved.getItems().forEach(item -> {
            StockMovement movement = new StockMovement();
            movement.setProduct(item.getProduct()); movement.setMovementType(StockMovement.Type.PURCHASE);
            Product purchased = item.getProduct();
            movement.setQuantityChange("keg".equals(purchased.getUnit())
                    ? item.getQuantity().multiply(BigDecimal.valueOf(purchased.getKegSizeLitres())) : item.getQuantity());
            movement.setReferenceType("INVOICE"); movement.setReferenceId(saved.getId());
            movement.setReason("Supplier invoice " + saved.getInvoiceNumber()); movements.save(movement);
        });
        return saved;
    }

    @GetMapping("/dashboard") public Dashboard dashboard() {
        List<Product> all = products.findAll();
        BigDecimal stockValue = all.stream().map(p -> p.getPricedQuantity().multiply(p.getCostPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal revenue = all.stream().map(p -> p.getPricedQuantity().multiply(p.getSellingPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long low = all.stream().filter(p -> p.getStock().compareTo(p.getMinimumStock()) <= 0).count();
        BigDecimal purchases = invoices.findAll().stream().map(SupplierInvoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Dashboard(stockValue, revenue, revenue.subtract(stockValue), purchases, all.size(), low);
    }

    private Product product(Long id) { return products.findById(id).orElseThrow(() -> notFound("Product")); }
    private ResponseStatusException notFound(String type) { return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found"); }
    private Product toProduct(Product p, ProductInput i) {
        if ("keg".equalsIgnoreCase(i.unit().trim()) && (i.kegSizeLitres() == null || !List.of(20,30,50).contains(i.kegSizeLitres())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un keg de 50 L, 30 L o 20 L");
        p.setSku(i.sku()); p.setName(i.name()); p.setCategory(i.category()); p.setUnit(i.unit());
        if ("keg".equalsIgnoreCase(i.unit().trim())) p.setUnit("keg");
        p.setKegSizeLitres("keg".equals(p.getUnit()) ? i.kegSizeLitres() : null);
        p.setMinimumStock(i.minimumStock()); p.setSellingPrice(i.sellingPrice());
        p.setActive(i.active());
        if(i.supplierSkus()!=null) {
            java.util.Set<String> seen=new java.util.HashSet<>();
            java.util.List<ProductSupplierSku> entries=new java.util.ArrayList<>();
            for(SupplierSkuInput link:i.supplierSkus()) {
                String code=link.sku().trim();
                if(!seen.add(link.supplierId()+":"+code)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Proveedor y SKU repetidos");
                if(supplierSkus.existsBySupplierIdAndSkuAndProductIdNot(link.supplierId(),code,p.getId()==null?-1L:p.getId()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"Ese SKU del proveedor ya pertenece a otro producto");
                ProductSupplierSku entry=p.getSupplierSkus().stream().filter(old -> old.getId()!=null && old.getSupplier().getId().equals(link.supplierId()) && old.getSku().equals(code)).findFirst().orElseGet(ProductSupplierSku::new);
                entry.setSupplier(suppliers.findById(link.supplierId()).orElseThrow(() -> notFound("Supplier"))); entry.setSku(code); entries.add(entry);
            }
            p.replaceSupplierSkus(entries);
            p.setSupplier(null);
        } else if(i.supplierId()!=null) p.setSupplier(suppliers.findById(i.supplierId()).orElseThrow(() -> notFound("Supplier")));
        return p;
    }

    public record ProductInput(@NotBlank @Size(max=50) String sku,
            @NotBlank @Size(max=140) String name, @NotBlank @Size(max=80) String category, @NotBlank @Size(max=30) String unit,
            @NotNull @DecimalMin("0") BigDecimal minimumStock,
            @NotNull @DecimalMin("0") BigDecimal sellingPrice,
            Long supplierId, boolean active, Integer kegSizeLitres, List<@Valid SupplierSkuInput> supplierSkus) {}
    public record SupplierSkuInput(@NotNull Long supplierId,@NotBlank @Size(max=50) String sku) {}
    public record StockAdjustment(BigDecimal quantity, String reason) {}
    public record InvoiceLineInput(@NotNull Long productId, @NotNull @DecimalMin(value="0", inclusive=false) BigDecimal quantity, @NotNull @DecimalMin("0") BigDecimal unitCost, @Size(max=50) String supplierSku) {}
    public record InvoiceInput(@NotBlank String invoiceNumber, @NotNull Long supplierId, LocalDate invoiceDate, SupplierInvoice.Status status, String notes, @NotEmpty List<@Valid InvoiceLineInput> items) {}
    public record Dashboard(BigDecimal stockValue, BigDecimal potentialRevenue, BigDecimal potentialProfit, BigDecimal purchases, long productCount, long lowStockCount) {}
}
