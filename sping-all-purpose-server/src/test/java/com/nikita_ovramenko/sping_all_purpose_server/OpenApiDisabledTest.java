package com.nikita_ovramenko.sping_all_purpose_server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "SWAGGER_ENABLED=false")
class OpenApiDisabledTest extends AbstractOpenApiTest {
    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html", "/swagger-ui/index.html"})
    void disablingSwaggerRemovesTheDocumentationRoutes(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }
}
