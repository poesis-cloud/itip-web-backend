package cloud.poesis.itip.web.backend.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cloud.poesis.itip.web.backend.auth.model.AuthMethod;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.strategy.AuthenticationStrategy;
import cloud.poesis.itip.web.backend.auth.strategy.AuthenticationStrategyResolver;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthenticationStrategyResolver authenticationStrategyResolver;

  @MockitoBean private AuthenticationStrategy authenticationStrategy;

  @Test
  void loginEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
    when(authenticationStrategyResolver.resolve(AuthMethod.LOCAL)).thenReturn(authenticationStrategy);
    when(authenticationStrategy.authenticate(any()))
        .thenReturn(
            AuthenticationResult.builder()
                .token("jwt-token")
                .email("security@itip.local")
                .expiresAt(Instant.parse("2030-01-01T00:00:00Z"))
                .build());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("""
                    {
                      "email": "security@itip.local",
                      "password": "password"
                    }
                    """))
        .andExpect(status().isOk());
  }

  @Test
  void nonAuthEndpointsShouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/private")).andExpect(status().is4xxClientError());
  }

  @Test
  void healthEndpointShouldRemainPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
