package com.barstock.api;

import com.barstock.model.*;
import com.barstock.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.nio.*;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.time.*;
import java.text.Normalizer;
import java.util.*;

@RestController @RequestMapping("/api/sales-reports")
public class SalesReportController {
    private final SalesReportRepository reports;
    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final ObjectMapper json;
    private final EntityManager em;
    public SalesReportController(SalesReportRepository reports,ProductRepository products,StockMovementRepository movements,ObjectMapper json,EntityManager em) {
        this.reports=reports;this.products=products;this.movements=movements;this.json=json;this.em=em;
    }
    public record Line(Long productId,String productName,String sku,BigDecimal sold,BigDecimal soldMl,BigDecimal stockDecrease,String stockUnit,Integer volumeMl,BigDecimal revenuePricePerStockUnit) {}
    public record View(Long id,String fileName,SalesReport.Period period,LocalDate startDate,LocalDate endDate,SalesReport.Status status,
                       Instant uploadedAt,Instant appliedAt,int rowCount,int productCount,List<Line> lines,List<String> errors) {}
    private View view(SalesReport r) {
        try {return new View(r.id,r.fileName,r.period,r.startDate,r.endDate,r.status,r.uploadedAt,r.appliedAt,r.rowCount,r.productCount,
            json.readValue(r.linesJson,new TypeReference<List<Line>>(){}),json.readValue(r.errorsJson,new TypeReference<List<String>>(){}));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    @GetMapping public List<View> list(){return reports.findAllByOrderByUploadedAtDescIdDesc().stream().map(this::view).toList();}
    @GetMapping("/{id}/file") public org.springframework.http.ResponseEntity<byte[]> file(@PathVariable Long id) {
        SalesReport r=reports.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Reporte inexistente"));
        return org.springframework.http.ResponseEntity.ok().header("Content-Type","text/csv; charset=UTF-8")
            .header("Content-Disposition",org.springframework.http.ContentDisposition.attachment().filename(r.fileName,StandardCharsets.UTF_8).build().toString())
            .header("Cache-Control","private, no-store").body(r.csvData);
    }
    @PostMapping(consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public View upload(@RequestParam MultipartFile file,@RequestParam SalesReport.Period period,@RequestParam LocalDate startDate) throws Exception {
        if(file.isEmpty()||file.getSize()>2*1024*1024) throw bad("Selecciona un CSV de hasta 2 MB");
        LocalDate end=switch(period){case DAILY -> startDate;case WEEKLY -> startDate.plusDays(6);case MONTHLY -> startDate.withDayOfMonth(1).plusMonths(1).minusDays(1);};
        if(period==SalesReport.Period.MONTHLY) startDate=startDate.withDayOfMonth(1);
        byte[] bytes=file.getBytes();
        String key=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(concat(bytes,(period+":"+startDate).getBytes(StandardCharsets.UTF_8))));
        SalesReport report=reports.findByImportKey(key).orElseGet(SalesReport::new);
        if(report.id!=null&&report.status!=SalesReport.Status.REJECTED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Este archivo ya se cargó para ese período; consulta el historial");
        report.rowCount=0;report.productCount=0;report.uploadedAt=Instant.now();report.csvData=bytes;
        report.period=period;report.startDate=startDate;report.endDate=end;report.importKey=key;
        report.fileName=Optional.ofNullable(file.getOriginalFilename()).orElse("ventas.csv").replaceAll(".*[\\\\/]", "");
        if(report.fileName.length()>255)report.fileName=report.fileName.substring(0,255);
        List<String> errors=new ArrayList<>();List<Line> lines=new ArrayList<>();
        try {
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString().replaceFirst("^\\uFEFF","");
            List<List<String>> rows=parse(text);
            if(rows.isEmpty())throw bad("El CSV está vacío");
            List<String> headers=rows.get(0).stream().map(this::normalize).toList();
            if(new HashSet<>(headers).size()!=headers.size())throw bad("El CSV tiene encabezados repetidos");
            int sku=column(headers,"sku","codigo","productsku","supplier_sku"),qty=column(headers,"cantidad_vendida","quantity","cantidad","qty","sold");
            int ml=column(headers,"mililitros_vendidos","millilitres_sold","milliliters_sold","ml_sold","ml_vendidos","mililitros","ml");
            if(sku<0||qty<0||ml<0)throw bad("El CSV necesita SKU, cantidad vendida y mililitros vendidos");
            Map<String,Set<Product>> codes=new HashMap<>();
            for(Product p:products.findAll()) {
                codes.computeIfAbsent(p.getSku(),ignored->new HashSet<>()).add(p);
                for(ProductSupplierSku link:p.getSupplierSkus())codes.computeIfAbsent(link.getSku(),ignored->new HashSet<>()).add(p);
            }
            for(int i=1;i<rows.size();i++) {
                List<String> row=rows.get(i); if(row.stream().allMatch(String::isBlank))continue;
                report.rowCount++;
                try {
                    if(row.size()!=headers.size())throw bad("cantidad de columnas incorrecta");
                    String code=row.get(sku).trim();Set<Product> matches=codes.getOrDefault(code,Set.of());
                    if(matches.size()!=1)throw bad(matches.isEmpty()?"SKU desconocido: "+code:"SKU ambiguo: "+code+". Usa el SKU interno");
                    Product p=matches.iterator().next();if(!p.isActive())throw bad("producto inactivo: "+code);
                    BigDecimal sold=decimal(row.get(qty));if(sold.signum()<0)throw bad("cantidad negativa");
                    BigDecimal soldMl=decimal(row.get(ml));
                    int capacity=capacity(p);
                    if(soldMl.scale()>3||soldMl.precision()-soldMl.scale()>12)throw bad("mililitros fuera de rango");
                    BigDecimal revenuePrice="keg".equals(p.getUnit())?p.getSellingPrice().divide(BigDecimal.valueOf(p.getKegSizeLitres()),java.math.MathContext.DECIMAL64):p.getSellingPrice();
                    lines.add(new Line(p.getId(),p.getName(),code,sold,soldMl,BigDecimal.ZERO,"keg".equals(p.getUnit())?"L":p.getUnit(),capacity,revenuePrice));
                }catch(Exception e){if(errors.size()<100)errors.add("Fila "+(i+1)+": "+message(e));}
            }
            if(report.rowCount==0)errors.add("El CSV no contiene ventas");
            // The CSV may repeat ingredients or use several supplier SKUs for one product.
            // Sum total millilitres first, then convert once per product.
            Map<Long,Line> grouped=new LinkedHashMap<>();
            for(Line line:lines) {
                Line old=grouped.get(line.productId());
                if(old==null)grouped.put(line.productId(),line);
                else grouped.put(line.productId(),new Line(line.productId(),line.productName(),Arrays.asList(old.sku().split(" / ",-1)).contains(line.sku())?old.sku():old.sku()+" / "+line.sku(),
                    old.sold().add(line.sold()),old.soldMl().add(line.soldMl()),BigDecimal.ZERO,line.stockUnit(),line.volumeMl(),line.revenuePricePerStockUnit()));
            }
            lines.clear();
            for(Line line:grouped.values()) {
                BigDecimal decrease=line.soldMl().divide(BigDecimal.valueOf(line.volumeMl()),6,java.math.RoundingMode.HALF_UP);
                if(decrease.precision()-decrease.scale()>12)errors.add("Cantidad demasiado grande para "+line.productName());
                lines.add(new Line(line.productId(),line.productName(),line.sku(),line.sold(),line.soldMl(),decrease,line.stockUnit(),line.volumeMl(),line.revenuePricePerStockUnit()));
            }
            Map<Long,BigDecimal> totals=new HashMap<>();lines.forEach(line->totals.merge(line.productId(),line.stockDecrease(),BigDecimal::add));
            report.productCount=totals.size();
            for(Product p:products.findAll())if(totals.getOrDefault(p.getId(),BigDecimal.ZERO).compareTo(p.getStock())>0)errors.add("Stock insuficiente para "+p.getName());
            if(overlap(report))errors.add("Este período se superpone con un reporte ya aplicado");
        }catch(Exception e){errors.add(message(e));}
        report.status=errors.isEmpty()?SalesReport.Status.READY:SalesReport.Status.REJECTED;
        report.linesJson=json.writeValueAsString(lines);report.errorsJson=json.writeValueAsString(errors);
        return view(reports.save(report));
    }
    @PostMapping("/{id}/apply") @Transactional(isolation=Isolation.READ_COMMITTED) public View apply(@PathVariable Long id) {
        SalesReport r=reports.locked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Reporte inexistente"));
        if(r.status!=SalesReport.Status.READY)throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo puedes aplicar un reporte listo, una vez");
        View preview=view(r);Map<Long,BigDecimal> totals=new TreeMap<>();preview.lines().forEach(line->totals.merge(line.productId(),line.stockDecrease(),BigDecimal::add));
        // Lock all products in the same order; overlapping reports cannot apply concurrently.
        List<Product> all=products.findAll();all.sort(Comparator.comparing(Product::getId));
        for(Product p:all){em.lock(p,LockModeType.PESSIMISTIC_WRITE);em.refresh(p);}
        if(overlap(r))throw new ResponseStatusException(HttpStatus.CONFLICT,"El período se superpone con un reporte aplicado");
        for(Product p:all)if(totals.containsKey(p.getId())) {
            if(!p.isActive()||p.getStock().compareTo(totals.get(p.getId()))<0)throw bad("Stock insuficiente o producto inactivo: "+p.getName());
            String recorded=preview.lines().stream().filter(line->line.productId().equals(p.getId())).findFirst().orElseThrow().stockUnit();
            if(!recorded.equals("keg".equals(p.getUnit())?"L":p.getUnit()))throw bad("Cambió el formato de "+p.getName()+"; carga un nuevo reporte");
            Integer recordedCapacity=preview.lines().stream().filter(line->line.productId().equals(p.getId())).findFirst().orElseThrow().volumeMl();
            if(recordedCapacity!=capacity(p))throw bad("Cambió la capacidad de "+p.getName()+"; revisa el reporte");
        }
        for(Product p:all)if(totals.containsKey(p.getId())&&totals.get(p.getId()).signum()>0){
            BigDecimal decrease=totals.get(p.getId());p.setStock(p.getStock().subtract(decrease));products.save(p);
            StockMovement m=new StockMovement();m.setProduct(p);m.setMovementType(StockMovement.Type.SALE);m.setQuantityChange(decrease.negate());
            m.setReferenceType("SALES_REPORT");m.setReferenceId(r.id);m.setReason("Ventas "+r.startDate+" / "+r.endDate);movements.save(m);
        }
        r.status=SalesReport.Status.APPLIED;r.appliedAt=Instant.now();return view(reports.save(r));
    }
    private boolean overlap(SalesReport r){return reports.existsByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(SalesReport.Status.APPLIED,r.endDate,r.startDate);}
    private int capacity(Product p){
        if("keg".equals(p.getUnit())||"L".equalsIgnoreCase(p.getUnit()))return 1000;
        if("ml".equalsIgnoreCase(p.getUnit()))return 1;
        if(p.getVolumeMl()==null||p.getVolumeMl()<=0)throw bad("Configura los ml por "+p.getUnit()+" en Productos: "+p.getName());
        return p.getVolumeMl();
    }
    private String normalize(String value){return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","").replace(' ','_');}
    private int column(List<String> headers,String... names){for(String name:names){int i=headers.indexOf(name);if(i>=0)return i;}return -1;}
    private BigDecimal decimal(String text){if(!text.trim().matches("[0-9]+([.,][0-9]+)?"))throw bad("cantidad inválida (sin separador de miles)");return new BigDecimal(text.trim().replace(',','.'));}
    private String message(Exception e){return e instanceof ResponseStatusException r?r.getReason():e instanceof CharacterCodingException?"Guarda el CSV en UTF-8":e instanceof ArithmeticException?"Usa cantidades con precisión de hasta 0,001 L/unidades":"CSV o número inválido";}
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private byte[] concat(byte[] a,byte[] b){byte[] result=Arrays.copyOf(a,a.length+b.length);System.arraycopy(b,0,result,a.length,b.length);return result;}
    private List<List<String>> parse(String text){
        String header=text.lines().findFirst().orElse("");char delimiter=header.contains(";")?';':header.contains("\t")?'\t':',';
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false,closed=false;
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(quoted){if(c=='"'){if(i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}else{quoted=false;closed=true;}}else cell.append(c);}
            else if(c==delimiter||c=='\n'||c=='\r'){row.add(cell.toString());cell.setLength(0);closed=false;if(c!=delimiter){rows.add(row);row=new ArrayList<>();if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;if(rows.size()>10001)throw bad("Máximo 10.000 filas por reporte");}}
            else if(c=='"'){if(cell.length()!=0||closed)throw bad("Comillas CSV inválidas");quoted=true;}
            else {if(closed&&!Character.isWhitespace(c))throw bad("Comillas CSV inválidas");if(!closed)cell.append(c);}
        }
        if(quoted)throw bad("Comillas CSV sin cerrar");if(cell.length()>0||!row.isEmpty()){row.add(cell.toString());rows.add(row);}if(rows.size()>10001)throw bad("Máximo 10.000 filas por reporte");return rows;
    }
}
