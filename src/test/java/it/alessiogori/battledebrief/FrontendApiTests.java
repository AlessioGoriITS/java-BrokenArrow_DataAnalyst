package it.alessiogori.battledebrief;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrontendApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void frontendAndAssetsArePublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("BATTLE")))
                .andExpect(content().string(containsString(
                        "id=\"hangar-title\""
                )))
                .andExpect(content().string(containsString(
                        "id=\"login-form\""
                )))
                .andExpect(content().string(containsString(
                        "id=\"register-form\""
                )))
                .andExpect(content().string(containsString(
                        "id=\"steam-link-form\""
                )));

        mockMvc.perform(get("/assets/css/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));

        mockMvc.perform(get("/assets/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "text/javascript"
                ))
                .andExpect(content().string(containsString(
                        "api.linkSteam"
                )));
    }

    @Test
    void publishingFrontendDoesNotExposeProtectedApis() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
