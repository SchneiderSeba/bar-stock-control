package com.barstock;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @WithMockUser
class SupplierSkuIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private long supplier(String name) throws Exception {
        return json.readTree(mvc.perform(post("/api/suppliers").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\""+name+"\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void supplierCodesReceiveIntoOneProductAndRemainOnHistoricalInvoices() throws Exception {
        long a=supplier("SKU supplier A"),b=supplier("SKU supplier B");
        String body="""
            {"sku":"GUINNESS-INTERNAL","name":"Guinness","category":"Beer","unit":"keg","kegSizeLitres":50,
             "minimumStock":0,"sellingPrice":500,"active":true,
             "supplierSkus":[{"supplierId":%d,"sku":"50055"},{"supplierId":%d,"sku":"9878"}]}
            """.formatted(a,b);
        long product=json.readTree(mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.supplierSkus.length()").value(2))
            .andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/suppliers/"+a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"SKU supplier A edited\",\"contactName\":\"Ana\",\"email\":\"ana@example.com\",\"phone\":\"123\",\"taxId\":\"ABC\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(a)).andExpect(jsonPath("$.contactName").value("Ana"));
        String invoice="""
            {"invoiceNumber":"SKU-%s","supplierId":%d,"items":[{"productId":%d,"supplierSku":"%s","quantity":1,"unitCost":100}]}
            """;
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invoice.formatted("A",a,product,"50055")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.items[0].supplierSku").value("50055"));
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invoice.formatted("B",b,product,"9878")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.items[0].product.stock").value(100));
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invoice.formatted("BAD",a,product,"9878")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.replace("GUINNESS-INTERNAL","OTHER-INTERNAL")))
            .andExpect(status().isConflict());
        // Re-saving unchanged mappings must keep them valid; supplier codes preserve leading zeros.
        mvc.perform(put("/api/products/"+product).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(100));
        mvc.perform(put("/api/products/"+product).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.replace("50055","0050055")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.supplierSkus[?(@.sku == '0050055')].sku").value(org.hamcrest.Matchers.hasItem("0050055")));
        JsonNode invoices=json.readTree(mvc.perform(get("/api/invoices")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        boolean found=false;
        for(JsonNode saved:invoices) if(saved.get("invoiceNumber").asText().equals("SKU-A")) {
            assertEquals("50055",saved.get("items").get(0).get("supplierSku").asText()); found=true;
        }
        assertTrue(found);
        mvc.perform(put("/api/suppliers/999999").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Missing\"}"))
            .andExpect(status().isNotFound());
    }
}
