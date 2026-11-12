package com.partidoya.platform.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("migration-test")
class AuthenticationSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jwtIdentityPreventsReadingAnotherAccountAndPlanMutation() throws Exception {
        var first = register("first-" + UUID.randomUUID() + "@test.com");
        var second = register("second-" + UUID.randomUUID() + "@test.com");
        var token = login(first.email());

        mockMvc.perform(get("/api/v1/users/{id}", second.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/users/{id}/plan", first.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"JUGADOR_PLUS\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpointRejectsMissingAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/1").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teamDiscoveryIsPublicButPersonalTeamAreaRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/teams"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/teams/mine"))
                .andExpect(status().isUnauthorized());
    }

    private RegisteredUser register(String email) throws Exception {
        var body = """
                {
                  "email":"%s",
                  "firstName":"Usuario",
                  "lastName":"Seguro",
                  "password":"Segura#1234",
                  "role":"JUGADOR",
                  "planCode":"JUGADOR_BASICO",
                  "phone":"+51987654321",
                  "district":"Miraflores",
                  "primarySport":"Fútbol",
                  "skillLevel":"INTERMEDIATE",
                  "availability":["EVENING"],
                  "termsAccepted":true,
                  "privacyAccepted":true,
                  "marketingConsent":false
                }
                """.formatted(email);
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new RegisteredUser(json.path("id").asLong(), email);
    }

    private String login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Segura#1234\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private record RegisteredUser(Long id, String email) {
    }
}
