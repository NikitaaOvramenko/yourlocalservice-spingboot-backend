package com.nikita_ovramenko.sping_all_purpose_server;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/** Real transactions and JWT authentication; no test transaction masks lazy-loading errors. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminWorkflowTest extends AbstractPostgresTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void anAdminCanPriceAPhoneLeadAndConvertItToWork() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"tcs.ovramenko@gmail.com\",\"password\":\"test\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String bearer = "Bearer " + json.readTree(login).get("accessToken").asText();
        String email = "admin-workflow-" + UUID.randomUUID() + "@example.com";
        String request = """
                {"orgSlug":"yourlocaljunkremoval","quote":{
                  "client":{"firstName":"Jane","lastName":"Doe","email":"%s"},
                  "location":{"country":"CANADA","provinceState":"ON","city":"Toronto",
                              "street":"1 Main St","postalCode":"M5H 1A1"},
                  "services":[{"serviceId":21,"quantity":2,"description":"Two sofas"}],
                  "description":"Phone lead","pictureKeys":["orgs/yourlocaljunkremoval/test-photo.jpg"]
                }}
                """.formatted(email);
        String created = mvc.perform(post("/api/admin/quotes").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long quoteId = json.readTree(created).get("id").asLong();
        long lineId = json.readTree(created).get("services").get(0).get("id").asLong();
        mvc.perform(patch("/api/admin/quotes/" + quoteId + "/items/" + lineId)
                .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitPrice\":125.50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lineTotal").value(251.00));
        mvc.perform(patch("/api/admin/quotes/" + quoteId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pictureKeys[0]").exists());
        // A full page forces the specification's separate count query.
        mvc.perform(get("/api/admin/quotes").header("Authorization", bearer)
                .param("orgSlug", "yourlocaljunkremoval").param("clientEmail", email)
                .param("status", "ACCEPTED").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].itemCount").value(1))
                .andExpect(jsonPath("$.content[0].total").value(251.00));
        String jobRequest = "{\"quoteId\":" + quoteId + ",\"scheduledAt\":\"2026-10-01T14:00:00Z\"}";
        String job = mvc.perform(post("/api/admin/jobs").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(jobRequest))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(251.00))
                .andExpect(jsonPath("$.client.email").value(email))
                .andReturn().getResponse().getContentAsString();
        long jobId = json.readTree(job).get("id").asLong();
        mvc.perform(post("/api/admin/jobs").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(jobRequest))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/jobs").header("Authorization", bearer)
                .param("orgSlug", "yourlocaljunkremoval").param("clientEmail", email).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].total").value(251.00));
        mvc.perform(patch("/api/admin/quotes/" + quoteId + "/items/" + lineId)
                .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"unitPrice\":1}")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/jobs/" + jobId).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(251.00));
        mvc.perform(get("/api/admin/users").header("Authorization", bearer)
                .param("role", "ADMIN").param("verified", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/admin/quotes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orgs/yourlocaljunkremoval/quotes")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(mailSender);
    }
}
