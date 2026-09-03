package com.nikita_ovramenko.sping_all_purpose_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nikita_ovramenko.sping_all_purpose_server.client.repository.ClientRepo;

/**
 * The currently deployed frontends must keep working byte-for-byte against
 * POST /api/email/form while they migrate to the org-scoped endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegacyQuoteEndpointTest extends AbstractPostgresTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepo clientRepo;

    @MockitoBean
    private JavaMailSender javaMailSender;

    private static String legacyBody(String email, String workType, String service) {
        return """
                {
                  "name": "John",
                  "lastname": "Doe",
                  "email": "%s",
                  "phone": "+14165551234",
                  "workType": "%s",
                  "service": ["%s"],
                  "country": "CANADA",
                  "town": "Toronto",
                  "street": "5 King St",
                  "postal_code": "M5H 1A1",
                  "description": "Basement cleanout",
                  "images": []
                }
                """.formatted(email, workType, service);
    }

    @Test
    void legacyPayloadResolvesWorkTypeAndServiceStrings() throws Exception {
        mockMvc.perform(post("/api/email/form")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(legacyBody("legacy@example.com", "YourLocalJunkRemoval", "Furniture Removal")))
                .andExpect(status().isOk())
                // Previously this field returned the client's LAST NAME, because the old
                // QuoteMapper put getLastname() in the email slot.
                .andExpect(jsonPath("$.to").value("legacy@example.com"));

        assertThat(clientRepo.findByEmailIgnoreCase("legacy@example.com")).isPresent();
    }

    @Test
    void unmappedWorkTypeIsRejectedRatherThanSilentlyMisdelivered() throws Exception {
        // "Plumbing" matches no organization slug or name.
        mockMvc.perform(post("/api/email/form")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(legacyBody("bad@example.com", "Plumbing", "Furniture Removal")))
                .andExpect(status().isNotFound());
    }

    @Test
    void serviceNameOutsideTheCatalogIsRejected() throws Exception {
        mockMvc.perform(post("/api/email/form")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(legacyBody("bad2@example.com", "YourLocalJunkRemoval", "Leak Fix")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogEndpointListsOnlyThatOrganizationsServices() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/orgs/yourlocalhandyman/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[?(@.slug == 'furniture-removal')]").isEmpty());
    }

    @Test
    void unknownOrganizationSlugReturns404() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/orgs/does-not-exist/services"))
                .andExpect(status().isNotFound());
    }
}
