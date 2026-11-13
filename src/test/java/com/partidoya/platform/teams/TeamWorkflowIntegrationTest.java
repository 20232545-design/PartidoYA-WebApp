package com.partidoya.platform.teams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.partidoya.platform.teams.repositories.TeamMembershipRepository;
import com.partidoya.platform.teams.repositories.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("migration-test")
class TeamWorkflowIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private TeamRepository teams;
    @Autowired private TeamMembershipRepository memberships;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticatedPlayerCreatesTeamUploadsLogoAndFindsItInMine() throws Exception {
        var player = registerPlayer("captain-" + UUID.randomUUID() + "@test.com");
        var token = login(player.email());

        mockMvc.perform(get("/api/v1/teams/mine").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.administered.length()").value(0))
                .andExpect(jsonPath("$.memberships.length()").value(0))
                .andExpect(jsonPath("$.applications.length()").value(0))
                .andExpect(jsonPath("$.pendingCount").value(0));

        var name = "Leones " + UUID.randomUUID().toString().substring(0, 8);
        var createResult = mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.captainId").value(player.id()))
                .andExpect(jsonPath("$.captain").value(true))
                .andExpect(jsonPath("$.viewerRelationship").value("CAPTAIN"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.maxMembers").value(30))
                .andReturn();
        var teamId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        var logo = new MockMultipartFile("file", "shield.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0});
        mockMvc.perform(multipart("/api/v1/teams/{id}/logo", teamId).file(logo)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("https://example.test/team-" + teamId + "-shield.png"));

        mockMvc.perform(get("/api/v1/teams/{id}", teamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.logoUrl").value("https://example.test/team-" + teamId + "-shield.png"));

        var replacement = new MockMultipartFile("file", "replacement.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 0, 0});
        mockMvc.perform(multipart("/api/v1/teams/{id}/logo", teamId).file(replacement)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("https://example.test/team-" + teamId + "-replacement.jpg"));

        var webp = new MockMultipartFile("file", "replacement.webp", "image/webp",
                new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0});
        mockMvc.perform(multipart("/api/v1/teams/{id}/logo", teamId).file(webp)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("https://example.test/team-" + teamId + "-replacement.webp"));

        var invalid = new MockMultipartFile("file", "notes.txt", "text/plain", "not-an-image".getBytes());
        mockMvc.perform(multipart("/api/v1/teams/{id}/logo", teamId).file(invalid)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        var oversized = new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[5_000_001]);
        mockMvc.perform(multipart("/api/v1/teams/{id}/logo", teamId).file(oversized)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/teams/mine").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.administered.length()").value(1))
                .andExpect(jsonPath("$.administered[0].id").value(teamId));

        assertThat(teams.findById(teamId)).isPresent();
        var captainMembership = memberships.findByTeamIdAndUserId(teamId, player.id()).orElseThrow();
        assertThat(captainMembership.getRole()).isEqualTo("CAPTAIN");
    }

    @Test
    void mineRequiresAuthenticationAndTeamAdministrationRequiresCaptain() throws Exception {
        mockMvc.perform(get("/api/v1/teams/mine"))
                .andExpect(status().isUnauthorized());

        var courtAdminEmail = "court-" + UUID.randomUUID() + "@test.com";
        registerCourtAdmin(courtAdminEmail);
        mockMvc.perform(get("/api/v1/teams/mine").header("Authorization", bearer(login(courtAdminEmail))))
                .andExpect(status().isForbidden());

        var captain = registerPlayer("owner-" + UUID.randomUUID() + "@test.com");
        var outsider = registerPlayer("outsider-" + UUID.randomUUID() + "@test.com");
        var captainToken = login(captain.email());
        var outsiderToken = login(outsider.email());
        var createResult = mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", bearer(captainToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody("Tigres " + UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated()).andReturn();
        var teamId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(put("/api/v1/teams/{id}", teamId)
                        .header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teamBody("Cambio no autorizado")))
                .andExpect(status().isForbidden());

        var logo = new MockMultipartFile("file", "shield.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0});
        mockMvc.perform(multipart("/api/v1/teams/{id}/logo", teamId).file(logo)
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/teams/{id}/requests", teamId)
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        mockMvc.perform(post("/api/v1/teams/{id}/requests", teamId)
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/teams/mine").header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications.length()").value(1))
                .andExpect(jsonPath("$.applications[0].status").value("PENDING"));
        mockMvc.perform(get("/api/v1/teams/{id}", teamId).header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewerRelationship").value("PENDING"))
                .andExpect(jsonPath("$.captain").value(false))
                .andExpect(jsonPath("$.member").value(false));
    }

    private RegisteredUser registerPlayer(String email) throws Exception {
        var body = """
                {"email":"%s","firstName":"Jugador","lastName":"Equipos","password":"Segura#1234",
                "role":"JUGADOR","planCode":"JUGADOR_BASICO","phone":"+51987654321","district":"Miraflores",
                "primarySport":"Fútbol","skillLevel":"INTERMEDIATE","availability":["EVENING"],
                "termsAccepted":true,"privacyAccepted":true,"marketingConsent":false}
                """.formatted(email);
        var result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new RegisteredUser(json.path("id").asLong(), email);
    }

    private void registerCourtAdmin(String email) throws Exception {
        var body = """
                {"email":"%s","firstName":"Admin","lastName":"Cancha","password":"Segura#1234",
                "role":"ADMIN_CANCHA","planCode":"CANCHA_EMPRENDEDOR","phone":"+51987654321",
                "district":"Miraflores","businessName":"Cancha Prueba","businessPhone":"+51987654321",
                "operationDistrict":"Miraflores","termsAccepted":true,"privacyAccepted":true,"marketingConsent":false}
                """.formatted(email);
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Segura#1234\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private static String teamBody(String name) {
        return """
                {"name":"%s","sport":"Fútbol","description":"Equipo permanente para jugar cada semana",
                "district":"Miraflores","competitiveLevel":"INTERMEDIATE","openToRequests":true}
                """.formatted(name);
    }

    private static String bearer(String token) { return "Bearer " + token; }
    private record RegisteredUser(Long id, String email) {}
}
