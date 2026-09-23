package com.barstock;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import com.barstock.repository.StockMovementRepository;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @WithMockUser
class SalesReportIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired StockMovementRepository movements;
    private long supplier() throws Exception {return json.readTree(mvc.perform(get("/api/suppliers")).andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();}
    private long product(String sku,String unit,Integer ml) throws Exception {
        String body="""
            {"sku":"%s","name":"%s","category":"Sales test","unit":"%s","kegSizeLitres":30,"volumeMl":%s,
             "minimumStock":0,"sellingPrice":100,"active":true,"supplierSkus":[{"supplierId":%d,"sku":"EXT-%s"}]}
            """.formatted(sku,sku,unit,ml,supplier(),sku);
        return json.readTree(mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    private void receive(long keg,long bottle,String number) throws Exception {
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
            {"invoiceNumber":"%s","supplierId":%d,"items":[{"productId":%d,"quantity":10,"unitCost":50},{"productId":%d,"quantity":10,"unitCost":20}]}
            """.formatted(number,supplier(),keg,bottle))).andExpect(status().isCreated());
    }
    private JsonNode upload(String csv,String period,String date) throws Exception {
        return json.readTree(mvc.perform(multipart("/api/sales-reports").file(new MockMultipartFile("file","ventas.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8)))
            .param("period",period).param("startDate",date).with(csrf())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private double stock(long id) throws Exception {
        JsonNode all=json.readTree(mvc.perform(get("/api/products")).andReturn().getResponse().getContentAsString());
        for(JsonNode p:all)if(p.get("id").asLong()==id)return p.get("stock").asDouble();throw new AssertionError();
    }
    @Test void totalsInMillilitresAreGroupedAndAppliedOnceWithHistoryAndOriginalFile() throws Exception {
        long keg=product("SALE-GUINNESS","keg",null),bottle=product("SALE-SPIRIT","bottle",1000);receive(keg,bottle,"SALE-RECEIVE");
        String csv="\uFEFFSKU;nombre del ítem;cantidad vendida;mililitros vendidos\r\nEXT-SALE-GUINNESS;\"Guinness; draught\";5;500\r\nSALE-GUINNESS;Guinness;4;2000\r\nSALE-SPIRIT;Cocktail ingredient;3;300\r\n";
        JsonNode report=upload(csv,"DAILY","2031-07-01");long id=report.get("id").asLong();
        assertEquals("READY",report.get("status").asText());assertEquals(2,report.get("productCount").asInt());
        assertEquals(2.5,report.get("lines").get(0).get("stockDecrease").asDouble());
        assertEquals(3,report.get("lines").get(1).get("stockDecrease").asDouble());assertEquals(300,stock(keg));
        long before=movements.count();
        mvc.perform(post("/api/sales-reports/"+id+"/apply").with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPLIED"));
        assertEquals(297.5,stock(keg));assertEquals(7,stock(bottle));assertEquals(before+2,movements.count());
        mvc.perform(post("/api/sales-reports/"+id+"/apply").with(csrf())).andExpect(status().isConflict());
        mvc.perform(multipart("/api/sales-reports").file(new MockMultipartFile("file","renamed.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8)))
            .param("period","DAILY").param("startDate","2031-07-01").with(csrf())).andExpect(status().isConflict());
        JsonNode overlap=upload(csv,"WEEKLY","2031-06-29");assertEquals("REJECTED",overlap.get("status").asText());
        mvc.perform(get("/api/sales-reports/"+id+"/file")).andExpect(status().isOk()).andExpect(content().bytes(csv.getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get("/api/sales-reports")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(overlap.get("id").asLong()));
    }
    @Test void unknownSkuOrMalformedCsvRejectsWholeReportAndApplyRechecksStockAtomically() throws Exception {
        long keg=product("SALE-ATOMIC","keg",null),bottle=product("SALE-ATOMIC-B","bottle",700);receive(keg,bottle,"SALE-ATOMIC-RECEIVE");
        String valid="SKU,nombre del ítem,cantidad vendida,mililitros vendidos\nSALE-ATOMIC,Brew,10,1000\nSALE-ATOMIC-B,Spirit,2,50";
        JsonNode invalid=upload(valid+"\nUNKNOWN,Bad,1,10","DAILY","2031-08-01");assertEquals("REJECTED",invalid.get("status").asText());assertEquals(300,stock(keg));
        mvc.perform(post("/api/sales-reports/"+invalid.get("id").asLong()+"/apply").with(csrf())).andExpect(status().isConflict());
        JsonNode malformed=upload("sku,cantidad vendida,mililitros vendidos\n\"unclosed,1,10","MONTHLY","2031-09-18");
        assertEquals("REJECTED",malformed.get("status").asText());assertEquals("2031-09-01",malformed.get("startDate").asText());assertEquals("2031-09-30",malformed.get("endDate").asText());
        JsonNode ready=upload(valid,"DAILY","2031-08-01");assertEquals("READY",ready.get("status").asText());
        assertEquals(2,ready.get("lines").get(1).get("stockDecrease").asDouble());
        mvc.perform(post("/api/products/"+keg+"/adjust").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-300}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/sales-reports/"+ready.get("id").asLong()+"/apply").with(csrf())).andExpect(status().isBadRequest());
        assertEquals(10,stock(bottle));
        long unconfigured=product("SALE-NO-VOLUME","case",null);
        JsonNode missingVolume=upload("SKU,cantidad vendida,mililitros vendidos\nSALE-NO-VOLUME,1,10","DAILY","2031-10-01");
        assertEquals("REJECTED",missingVolume.get("status").asText());assertTrue(missingVolume.get("errors").get(0).asText().contains("Configura los ml"));
    }
}
