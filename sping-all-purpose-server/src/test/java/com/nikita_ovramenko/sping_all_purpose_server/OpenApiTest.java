package com.nikita_ovramenko.sping_all_purpose_server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "SWAGGER_ENABLED=true")
class OpenApiTest extends AbstractOpenApiTest {
    @Test
    void documentationIsPublicAndDescribesEveryAdminPathAndBearerAuthentication() throws Exception {
        var result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray());
        for (String path : new String[] {
                "/quotes", "/quotes/{id}", "/quotes/{quoteId}/items", "/quotes/{quoteId}/items/{itemId}",
                "/jobs", "/jobs/{id}", "/jobs/{jobId}/items", "/jobs/{jobId}/items/{itemId}",
                "/users", "/users/{id}", "/users/{id}/verify",
                "/organizations", "/organizations/{id}", "/organizations/{id}/services",
                "/services", "/services/{id}" }) {
            result.andExpect(jsonPath("$.paths['/api/admin" + path + "']").exists());
        }
        result.andExpect(jsonPath("$.components.schemas.JobCreateRequest.properties.orgSlug").exists())
                .andExpect(jsonPath("$.components.schemas.OrganizationUpdateRequest.properties.mailSettings").exists());
    }

    @Test
    void swaggerUiAndItsConfigurationArePublic() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
    }
}
