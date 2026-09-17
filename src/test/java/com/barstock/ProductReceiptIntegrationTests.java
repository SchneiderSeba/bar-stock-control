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
class ProductReceiptIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private long create(String sku,String unit,Integer size) throws Exception {
        String body="""
            {"sku":"%s","name":"Receipt test","category":"Beer","unit":"%s","kegSizeLitres":%s,
             "stock":999,"costPrice":999,"minimumStock":10,"sellingPrice":200,"active":true}
            """.formatted(sku,unit,size);
        return json.readTree(mvc.perform(post("/api/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.costPrice").value(0)).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void catalogueIsSeparateFromStockAndMultiItemInvoiceReceivesAllItemsAtomically() throws Exception {
        long keg=create("RECEIPT-KEG","keg",30), bottle=create("RECEIPT-BOTTLE","bottle",null);
        long supplier=json.readTree(mvc.perform(get("/api/suppliers")).andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        String invoice="""
            {"invoiceNumber":"RECEIPT-MULTI-001","supplierId":%d,
             "items":[{"productId":%d,"quantity":2,"unitCost":120},{"productId":%d,"quantity":5,"unitCost":10}]}
            """.formatted(supplier,keg,bottle);
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invoice))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(290))
                .andExpect(jsonPath("$.items[0].product.stock").value(60))
                .andExpect(jsonPath("$.items[1].product.stock").value(5));
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invoice)).andExpect(status().isConflict());
        mvc.perform(post("/api/products/"+keg+"/adjust").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":10,\"reason\":\"Uninvoiced stock\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/products/"+keg).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"RECEIPT-KEG\",\"name\":\"Edited receipt\",\"category\":\"Beer\",\"unit\":\"keg\",\"kegSizeLitres\":30,\"stock\":999,\"costPrice\":999,\"minimumStock\":5,\"sellingPrice\":250,\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(60)).andExpect(jsonPath("$.costPrice").value(120));
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(invoice.replace("RECEIPT-MULTI-001","RECEIPT-INVALID").replace("\"quantity\":2","\"quantity\":-2")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/invoices").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(invoice.replace("RECEIPT-MULTI-001","RECEIPT-ROLLBACK").replace("\"productId\":"+bottle,"\"productId\":999999")))
                .andExpect(status().isNotFound());
        JsonNode products=json.readTree(mvc.perform(get("/api/products")).andReturn().getResponse().getContentAsString());
        for(JsonNode p:products) if(p.get("id").asLong()==keg) assertEquals(60,p.get("stock").asInt());
    }
}
