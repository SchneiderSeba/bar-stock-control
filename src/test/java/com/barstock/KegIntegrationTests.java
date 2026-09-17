package com.barstock;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc @WithMockUser
class KegIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private JsonNode dashboard() throws Exception { return json.readTree(mvc.perform(get("/api/dashboard")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()); }
    private String input(int size) { return """
        {"sku":"KEG-TEST-%d","name":"Keg test","category":"Beer","unit":"keg","kegSizeLitres":%d,
         "stock":75,"minimumStock":10,"costPrice":100,"sellingPrice":200,"active":true}
        """.formatted(size,size); }
    @Test void sizesValueLitresCorrectlyAndAllowPartialStockAdjustments() throws Exception {
        for (int size : new int[]{50,30,20}) {
            JsonNode before=dashboard();
            JsonNode product=json.readTree(mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(input(size)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.kegSizeLitres").value(size)).andReturn().getResponse().getContentAsString());
            JsonNode after=dashboard();
            assertEquals(75.0 / size * 100, after.get("stockValue").asDouble()-before.get("stockValue").asDouble(), .000001);
            assertEquals(75.0 / size * 200, after.get("potentialRevenue").asDouble()-before.get("potentialRevenue").asDouble(), .000001);
            mvc.perform(post("/api/products/"+product.get("id").asLong()+"/adjust").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":-1.25,\"reason\":\"Served beer\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(73.75));
            long supplierId=json.readTree(mvc.perform(get("/api/suppliers")).andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
            String invoice="""
                {"invoiceNumber":"KEG-PURCHASE-%d","supplierId":%d,
                 "items":[{"productId":%d,"quantity":2,"unitCost":120}]}
                """.formatted(size,supplierId,product.get("id").asLong());
            mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invoice))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(240))
                    .andExpect(jsonPath("$.items[0].product.stock").value(73.75+2*size))
                    .andExpect(jsonPath("$.items[0].product.costPrice").value(120));
        }
        mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(input(40))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(input(50).replace(",\"kegSizeLitres\":50",""))).andExpect(status().isBadRequest());
    }
}
