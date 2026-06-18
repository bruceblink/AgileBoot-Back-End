package app.keystone.admin.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.keystone.admin.KeystoneAdminApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = KeystoneAdminApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
@Tag("db-integration")
class SwaggerDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsShouldRequireAuthenticationWhenDisabled() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(106));
    }

    @Test
    void swaggerUiShouldRequireAuthenticationWhenDisabled() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(106));
    }
}
