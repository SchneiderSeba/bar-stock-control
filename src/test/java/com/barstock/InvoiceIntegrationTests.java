package com.barstock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class InvoiceIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void purchaseUpdatesStockAndInvoiceCanBeReadOutsideTransaction() throws Exception {
        JsonNode products = json.readTree(mvc.perform(get("/api/products"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode product = products.get(0);
        long productId = product.get("id").asLong();
        long supplierId = product.get("supplier").get("id").asLong();
        String body = """
            {"invoiceNumber":"TEST-001","supplierId":%d,"invoiceDate":"2026-09-17",
             "items":[{"productId":%d,"quantity":2,"unitCost":10.50}]}
            """.formatted(supplierId, productId);
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        JsonNode invoices = json.readTree(mvc.perform(get("/api/invoices"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("TEST-001", invoices.get(0).get("invoiceNumber").asText());
        assertEquals(21.0, invoices.get(0).get("total").asDouble());
        assertEquals(productId, invoices.get(0).get("items").get(0).get("product").get("id").asLong());
        JsonNode updated = json.readTree(mvc.perform(get("/api/products"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode item : updated) {
            if (item.get("id").asLong() == productId) {
                assertEquals(product.get("stock").asDouble() + 2, item.get("stock").asDouble());
                assertEquals(10.5, item.get("costPrice").asDouble());
            }
        }
    }
}
