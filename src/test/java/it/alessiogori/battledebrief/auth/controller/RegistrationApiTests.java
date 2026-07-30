package it.alessiogori.battledebrief.auth.controller;

import it.alessiogori.battledebrief.user.entity.User;
import it.alessiogori.battledebrief.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersLocalUserWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson("demo", "demo@example.com")))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        matchesPattern("/api/users/\\d+")
                ))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.email").value("demo@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User savedUser = userRepository
                .findByUsernameIgnoreCase("demo")
                .orElseThrow();

        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Demo123!");
        assertThat(passwordEncoder.matches(
                "Demo123!",
                savedUser.getPasswordHash()
        )).isTrue();
    }

    @Test
    void rejectsCaseInsensitiveDuplicateUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson(
                                "DemoUser",
                                "first@example.com"
                        )))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson(
                                "demouser",
                                "second@example.com"
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value(
                        "Username already exists"
                ));
    }

    @Test
    void reportsAllInvalidRegistrationFields() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "invalid user",
                                  "email": "not-an-email",
                                  "password": "weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void keepsOtherEndpointsProtected() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private String validRegistrationJson(String username, String email) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "Demo123!"
                }
                """.formatted(username, email);
    }
}
