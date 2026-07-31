package it.alessiogori.battledebrief.unit.controller;

import it.alessiogori.battledebrief.auth.security.AuthenticatedUser;
import it.alessiogori.battledebrief.auth.security.JwtService;
import it.alessiogori.battledebrief.unit.entity.Specialization;
import it.alessiogori.battledebrief.unit.entity.Unit;
import it.alessiogori.battledebrief.unit.repository.SpecializationRepository;
import it.alessiogori.battledebrief.unit.repository.UnitRepository;
import it.alessiogori.battledebrief.user.entity.Role;
import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UnitCatalogApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Specialization airborne;
    private Specialization armored;
    private Unit abrams;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void createCatalogAndUsers() {
        airborne = specializationRepository.save(
                new Specialization("Airborne", "USA", "Air assault forces")
        );
        armored = specializationRepository.save(
                new Specialization("Armored", "USA", "Heavy formations")
        );

        abrams = unit(
                "usa_m1a1",
                "M1A1 Abrams",
                "USA",
                "TANK",
                240,
                armored
        );
        Unit riflemen = unit(
                "usa_riflemen",
                "Riflemen",
                "USA",
                "INFANTRY",
                45,
                airborne
        );
        Unit t80 = unit(
                "rus_t80u",
                "T-80U",
                "RUS",
                "TANK",
                210,
                armored
        );
        unitRepository.saveAllAndFlush(List.of(abrams, riflemen, t80));

        String passwordHash = passwordEncoder.encode("Demo123!");
        User admin = new User("catalog-admin", "admin@catalog.test", passwordHash);
        admin.changeRole(Role.ADMIN);
        User user = new User("catalog-user", "user@catalog.test", passwordHash);
        userRepository.saveAllAndFlush(List.of(admin, user));
        adminToken = issueToken(admin);
        userToken = issueToken(user);
    }

    @Test
    void publicCatalogSupportsCombinedFiltersAndPagination() throws Exception {
        mockMvc.perform(get("/api/units")
                        .queryParam("faction", "usa")
                        .queryParam("category", "tank")
                        .queryParam("specializationId", armored.getId().toString())
                        .queryParam("minCost", "200")
                        .queryParam("maxCost", "250")
                        .queryParam("page", "0")
                        .queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("M1A1 Abrams"))
                .andExpect(jsonPath("$.content[0].specializations[0].name")
                        .value("Armored"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void publicCanReadDetailsAndInvalidCostRangeReturnsBadRequest()
            throws Exception {
        mockMvc.perform(get("/api/units/{id}", abrams.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalUnitId").value("usa_m1a1"))
                .andExpect(jsonPath("$.faction").value("USA"));

        mockMvc.perform(get("/api/units")
                        .queryParam("minCost", "300")
                        .queryParam("maxCost", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void unitAdministrationRequiresAdminRole() throws Exception {
        String request = createUnitJson("usa_apache", airborne.getId());

        mockMvc.perform(post("/api/admin/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/units")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUpdateAndDeleteUnit() throws Exception {
        String response = mockMvc.perform(post("/api/admin/units")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUnitJson("usa_apache", airborne.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        "/api/units/\\d+"
                )))
                .andExpect(jsonPath("$.name").value("AH-64 Apache"))
                .andExpect(jsonPath("$.faction").value("USA"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long createdId = extractId(response);
        mockMvc.perform(put("/api/admin/units/{id}", createdId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateUnitJson(armored.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("AH-64D Apache"))
                .andExpect(jsonPath("$.baseCost").value(190))
                .andExpect(jsonPath("$.specializations[0].name")
                        .value("Armored"));

        mockMvc.perform(delete("/api/admin/units/{id}", createdId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/units/{id}", createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsDuplicateIdAndUnknownSpecialization() throws Exception {
        mockMvc.perform(post("/api/admin/units")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUnitJson("usa_m1a1", airborne.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));

        mockMvc.perform(post("/api/admin/units")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUnitJson("missing-spec", 999999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void specializationsArePublicAndAdminCanCreateThem() throws Exception {
        mockMvc.perform(get("/api/specializations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(post("/api/admin/specializations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Marines",
                                  "faction": "usa",
                                  "description": "Naval infantry"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Marines"))
                .andExpect(jsonPath("$.faction").value("USA"));

        assertThat(specializationRepository
                .existsByNameIgnoreCaseAndFactionIgnoreCase("Marines", "USA"))
                .isTrue();
    }

    private Unit unit(
            String externalId,
            String name,
            String faction,
            String category,
            int cost,
            Specialization specialization
    ) {
        Unit unit = new Unit(
                externalId,
                name,
                faction,
                category,
                cost,
                "2026.1"
        );
        unit.addSpecialization(specialization);
        return unit;
    }

    private String createUnitJson(String externalId, Long specializationId) {
        return """
                {
                  "externalUnitId": "%s",
                  "name": "AH-64 Apache",
                  "faction": "usa",
                  "category": "helicopter",
                  "baseCost": 180,
                  "description": "Attack helicopter",
                  "hitPoints": 100,
                  "speed": 72.5,
                  "armor": "LIGHT",
                  "mainWeapon": "Hellfire",
                  "imageUrl": null,
                  "datasetVersion": "2026.1",
                  "specializationIds": [%d]
                }
                """.formatted(externalId, specializationId);
    }

    private String updateUnitJson(Long specializationId) {
        return """
                {
                  "name": "AH-64D Apache",
                  "faction": "usa",
                  "category": "helicopter",
                  "baseCost": 190,
                  "description": "Updated attack helicopter",
                  "hitPoints": 110,
                  "speed": 74.0,
                  "armor": "LIGHT",
                  "mainWeapon": "Longbow Hellfire",
                  "imageUrl": null,
                  "datasetVersion": "2026.2",
                  "specializationIds": [%d]
                }
                """.formatted(specializationId);
    }

    private Long extractId(String response) {
        String marker = "\"id\":";
        int start = response.indexOf(marker) + marker.length();
        int end = response.indexOf(',', start);
        return Long.valueOf(response.substring(start, end));
    }

    private String issueToken(User user) {
        return jwtService.issueToken(AuthenticatedUser.from(user)).value();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
