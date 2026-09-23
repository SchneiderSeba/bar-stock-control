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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc @WithMockUser
class DashboardMonthlyComparisonIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private JsonNode dashboard() throws Exception {return json.readTree(mvc.perform(get("/api/dashboard")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
    private double value(JsonNode node,String month,String field){return node.get("monthlyComparison").get(month).get(field).asDouble();}
    private long uploadAndApply(String rows,LocalDate date) throws Exception {
        String csv="SKU,nombre del ítem,cantidad vendida,mililitros vendidos\n"+rows;
        JsonNode report=json.readTree(mvc.perform(multipart("/api/sales-reports")
            .file(new MockMultipartFile("file","analytics-"+date+".csv","text/csv",csv.getBytes(StandardCharsets.UTF_8)))
            .param("period","DAILY").param("startDate",date.toString()).with(csrf()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("READY"))
            .andReturn().getResponse().getContentAsString());
        long id=report.get("id").asLong();
        mvc.perform(post("/api/sales-reports/"+id+"/apply").with(csrf())).andExpect(status().isOk());
        return id;
    }
    @Test void dashboardComparesAppliedSalesPurchaseSpendAndSnapshotProfitByMonth() throws Exception {
        LocalDate current=LocalDate.now(java.time.ZoneId.of("Europe/Dublin")).withDayOfMonth(1),previous=current.minusMonths(1);
        long supplier=json.readTree(mvc.perform(get("/api/suppliers")).andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        String product="""
            {"sku":"MONTHLY-ANALYTICS","name":"Monthly analytics","category":"Test","unit":"bottle","volumeMl":1000,
             "minimumStock":0,"sellingPrice":100,"active":true}
            """;
        long productId=json.readTree(mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(product))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        String kegProduct="""
            {"sku":"MONTHLY-KEG","name":"Monthly keg","category":"Test","unit":"keg","kegSizeLitres":30,
             "minimumStock":0,"sellingPrice":300,"active":true}
            """;
        long kegId=json.readTree(mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(kegProduct))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        JsonNode before=dashboard();
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
            {"invoiceNumber":"MONTHLY-PREVIOUS","supplierId":%d,"invoiceDate":"%s","items":[{"productId":%d,"quantity":10,"unitCost":40}]}
            """.formatted(supplier,previous,productId))).andExpect(status().isCreated());
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
            {"invoiceNumber":"MONTHLY-CURRENT","supplierId":%d,"invoiceDate":"%s","items":[{"productId":%d,"quantity":10,"unitCost":30},{"productId":%d,"quantity":1,"unitCost":100}]}
            """.formatted(supplier,current,productId,kegId))).andExpect(status().isCreated());
        uploadAndApply("MONTHLY-ANALYTICS,Analytics,1,1000",previous);
        uploadAndApply("MONTHLY-ANALYTICS,Analytics,1,2000\nMONTHLY-KEG,Keg,1,30000",current);
        // Changing the catalogue price must not rewrite historical sales.
        mvc.perform(put("/api/products/"+productId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(product.replace("\"sellingPrice\":100","\"sellingPrice\":999"))).andExpect(status().isOk());
        JsonNode after=dashboard();
        assertEquals(100,value(after,"previous","sales")-value(before,"previous","sales"),0.001);
        assertEquals(400,value(after,"previous","purchases")-value(before,"previous","purchases"),0.001);
        assertEquals(-300,value(after,"previous","profit")-value(before,"previous","profit"),0.001);
        assertEquals(400,value(after,"current","sales")-value(before,"current","sales"),0.001);
        assertEquals(400,value(after,"current","purchases")-value(before,"current","purchases"),0.001);
        assertEquals(0,value(after,"current","profit")-value(before,"current","profit"),0.001);
        assertEquals(value(before,"previous","appliedReportCount")+1,value(after,"previous","appliedReportCount"),0.001);
        assertEquals(value(before,"current","appliedReportCount")+1,value(after,"current","appliedReportCount"),0.001);
        assertEquals(previous.toString(),after.at("/monthlyComparison/previous/startDate").asText());
        assertEquals(current.toString(),after.at("/monthlyComparison/current/startDate").asText());
    }
}
