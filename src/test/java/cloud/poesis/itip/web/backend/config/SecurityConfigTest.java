package cloud.poesis.itip.web.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.strategy.EmailPasswordAuthenticationStrategy;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigTest.ProtectedTestProbeController.class)
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EmailPasswordAuthenticationStrategy emailPasswordAuthenticationStrategy;

  @Test
  void loginEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
    when(emailPasswordAuthenticationStrategy.authenticate(any()))
        .thenReturn(
            AuthenticationResult.builder()
                .token("jwt-token")
                .expiresAt(Instant.parse("2030-01-01T00:00:00Z"))
                .build());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    """
                    {
                      "email": "security@itip.local",
                      "password": "password"
                    }
                    """))
        .andExpect(status().isOk());
  }

  @Test
  void nonAuthEndpointsShouldRequireAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/test-protected-probe"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
  }

  @Test
  void healthEndpointShouldRemainPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void openApiDocsShouldBeAccessibleWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  @Test
  void healthEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @TestConfiguration
  static class ProtectedTestProbeController {
    @RestController
    static class ProbeController {
      @GetMapping("/api/test-protected-probe")
      String protectedProbe() {
        return "protected";
      }
    }
  }
}
