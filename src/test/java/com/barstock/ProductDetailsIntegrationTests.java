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

@SpringBootTest @AutoConfigureMockMvc @WithMockUser
class ProductDetailsIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Test void stockHistoryIsFilteredOrderedAndHasInvoiceReferences() throws Exception {
        String body="""
            {"sku":"DETAILS-TEST","name":"Details test","category":"Beer","unit":"keg","kegSizeLitres":30,
             "minimumStock":0,"sellingPrice":100,"active":true}
            """;
        long id=json.readTree(mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/products/"+id+"/movements")).andExpect(status().isOk()).andExpect(content().json("[]"));
        long supplier=json.readTree(mvc.perform(get("/api/suppliers")).andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
            {"invoiceNumber":"DETAILS-INVOICE","supplierId":%d,"items":[{"productId":%d,"quantity":2,"unitCost":50}]}
            """.formatted(supplier,id))).andExpect(status().isCreated());
        mvc.perform(post("/api/products/"+id+"/adjust").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-1,\"reason\":\"Details consumption\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/products/"+id+"/movements")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].movementType").value("ADJUSTMENT"))
            .andExpect(jsonPath("$[0].quantityChange").value(-1)).andExpect(jsonPath("$[0].product").doesNotExist())
            .andExpect(jsonPath("$[1].movementType").value("PURCHASE")).andExpect(jsonPath("$[1].quantityChange").value(60))
            .andExpect(jsonPath("$[1].referenceType").value("INVOICE")).andExpect(jsonPath("$[1].referenceId").isNumber());
        mvc.perform(get("/api/products/999999/movements")).andExpect(status().isNotFound());
        mvc.perform(get("/products/"+id)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }
}
