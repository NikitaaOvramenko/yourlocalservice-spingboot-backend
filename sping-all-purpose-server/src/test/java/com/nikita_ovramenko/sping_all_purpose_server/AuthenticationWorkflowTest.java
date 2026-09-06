package com.nikita_ovramenko.sping_all_purpose_server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

/** Real signatures, transactions, Flyway migration and PostgreSQL session locking. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationWorkflowTest extends AbstractPostgresTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode login() throws Exception {
        return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"tcs.ovramenko@gmail.com\",\"password\":\"test\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String body(String token) { return json.writeValueAsString(java.util.Map.of("refreshToken", token)); }

    @Test
    void refreshRotatesAndLogoutRevokesAccessAndRefreshTokens() throws Exception {
        JsonNode first = login();
        String oldRefresh = first.get("refreshToken").asText();
        JsonNode next = json.readTree(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(body(oldRefresh))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body(oldRefresh)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + next.get("accessToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        // Logout with the pre-rotation token still revokes the whole session.
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(body(oldRefresh)))
                .andExpect(status().isNoContent());
        for (String access : List.of(first.get("accessToken").asText(), next.get("accessToken").asText())) {
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(body(next.get("refreshToken").asText()))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(body(oldRefresh)))
                .andExpect(status().isNoContent());
    }

    @Test
    void tokenTypesCannotBeInterchangedAndMalformedRefreshIs401() throws Exception {
        JsonNode tokens = login();
        mvc.perform(get("/api/admin/quotes").header("Authorization", "Bearer " + tokens.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(body(tokens.get("accessToken").asText()))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body("broken")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void concurrentRefreshesCanConsumeATokenOnlyOnce() throws Exception {
        String token = login().get("refreshToken").asText();
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> refresh = () -> mvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON).content(body(token))).andReturn().getResponse().getStatus();
            var results = executor.invokeAll(List.of(refresh, refresh));
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 401);
        } finally { executor.shutdownNow(); }
    }

    @Test
    void loggingOutOneSessionDoesNotLogOutAnother() throws Exception {
        JsonNode first = login();
        JsonNode second = login();
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(body(first.get("refreshToken").asText()))).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + second.get("accessToken").asText()))
                .andExpect(status().isOk());
    }
}
