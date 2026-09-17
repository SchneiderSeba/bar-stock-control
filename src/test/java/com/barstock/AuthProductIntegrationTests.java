package com.barstock;

import com.barstock.repository.UserRepository;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties={"ADMIN_EMAIL=admin@test.example", "ADMIN_PASSWORD=TestAdmin-Password-123"})
@AutoConfigureMockMvc
class AuthProductIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Test void anonymousRequestsAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
    @Test void registrationSessionSkuAndImageLifecycle() throws Exception {
        String registration = """
            {"name":"Test User","email":"Employee@Test.Example","password":"StrongPassword-123","role":"ADMIN"}
            """;
        var result = mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);
        assertNotNull(session);
        assertNotEquals("StrongPassword-123", users.findByEmail("employee@test.example").orElseThrow().getPasswordHash());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("employee@test.example"));
        mvc.perform(get("/api/admin/users").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registration)).andExpect(status().isConflict());
        String product="""
            {"sku":"TEST-IMAGE","name":"Test product","category":"Beer","unit":"unit",
             "stock":2,"minimumStock":1,"costPrice":3,"sellingPrice":5,"active":true}
            """;
        mvc.perform(post("/api/products").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(product.replace("TEST-IMAGE", "")))
                .andExpect(status().isBadRequest());
        JsonNode saved=json.readTree(mvc.perform(post("/api/products").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id=saved.get("id").asLong();
        mvc.perform(put("/api/products/"+id).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(product.replace("Test product","Renamed product")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("TEST-IMAGE")).andExpect(jsonPath("$.pulCode").doesNotExist());
        byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWQAAAABJRU5ErkJggg==");
        mvc.perform(multipart("/api/products/"+id+"/image").file(new MockMultipartFile("file","image.png","image/png",png)).session(session).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imageVersion").isString()).andExpect(jsonPath("$.imageData").doesNotExist());
        mvc.perform(get("/api/products/"+id+"/image").session(session)).andExpect(status().isOk()).andExpect(content().bytes(png));
        mvc.perform(get("/api/products/"+id+"/image")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/products/"+id+"/image").file(new MockMultipartFile("file","fake.png","image/png","<script>alert(1)</script>".getBytes())).session(session).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/products/"+id+"/image").session(session).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/products/"+id+"/image").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
        assertTrue(session.isInvalid());
    }
    @Test void adminCanLoginAndChangePassword() throws Exception {
        String login="{\"email\":\"admin@test.example\",\"password\":\"TestAdmin-Password-123\"}";
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login.replace("TestAdmin-Password-123","wrong")))
                .andExpect(status().isUnauthorized());
        var result=mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN")).andReturn();
        MockHttpSession session=(MockHttpSession)result.getRequest().getSession(false);
        mvc.perform(get("/api/admin/users").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/password").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"TestAdmin-Password-123\",\"newPassword\":\"ChangedPassword-123\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login.replace("TestAdmin-Password-123","ChangedPassword-123")))
                .andExpect(status().isOk());
    }
}
