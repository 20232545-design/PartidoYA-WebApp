package com.partidoya.platform.matches;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujo de pago por Yape: el organizador ve quién envió el comprobante y puede descargarlo. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("migration-test")
class JoinRequestWorkflowIntegrationTest {
    /** Cabecera mínima válida de un PNG. */
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0};

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void organizerSeesThePlayerNameAndCanDownloadTheProofOnlyWithTheirToken() throws Exception {
        var organizer = registerPlayer("Carla", "Organizadora");
        var player = registerPlayer("Ana", "Torres");
        var matchId = createPaidMatch(organizer.token());

        mockMvc.perform(multipart("/api/v1/matches/{id}/join-requests", matchId)
                        .file(new MockMultipartFile("proof", "yape.png", "image/png", PNG))
                        .param("userId", player.id().toString())
                        .header("Authorization", bearer(player.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerName").value("Ana Torres"));

        var listing = mockMvc.perform(get("/api/v1/matches/{id}/join-requests", matchId)
                        .param("requesterId", organizer.id().toString())
                        .header("Authorization", bearer(organizer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerId").value(player.id()))
                .andExpect(jsonPath("$[0].playerName").value("Ana Torres"))
                .andReturn();
        var proofUrl = objectMapper.readTree(listing.getResponse().getContentAsString()).path(0).path("proofUrl").asText();

        mockMvc.perform(get(proofUrl).header("Authorization", bearer(organizer.token())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));
        // Un <img src> no envía el token: por eso el frontend descarga el comprobante con HttpClient.
        mockMvc.perform(get(proofUrl)).andExpect(status().isUnauthorized());
    }

    private long createPaidMatch(String organizerToken) throws Exception {
        var body = """
                {"sport":"Fútbol","title":"Pichanga con Yape","description":"Pago por jugador",
                "address":"Av. Javier Prado 123, Lima","matchDate":"2030-01-15T19:00:00","totalSlots":10,
                "price":160.0,"latitude":null,"longitude":null,"requiresPlayerPayment":true,"yapePhone":"987654321"}
                """;
        var result = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private RegisteredUser registerPlayer(String firstName, String lastName) throws Exception {
        var email = "yape-" + UUID.randomUUID() + "@test.com";
        var body = """
                {"email":"%s","firstName":"%s","lastName":"%s","password":"Segura#1234",
                "role":"JUGADOR","planCode":"JUGADOR_BASICO","phone":"+51987654321","district":"Miraflores",
                "primarySport":"Fútbol","skillLevel":"INTERMEDIATE","availability":["EVENING"],
                "termsAccepted":true,"privacyAccepted":true,"marketingConsent":false}
                """.formatted(email, firstName, lastName);
        var registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        var id = objectMapper.readTree(registration.getResponse().getContentAsString()).path("id").asLong();
        var login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Segura#1234\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn();
        return new RegisteredUser(id, objectMapper.readTree(login.getResponse().getContentAsString()).path("token").asText());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record RegisteredUser(Long id, String token) {
    }
}
