package cloud.poesis.itip.web.backend.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cloud.poesis.itip.web.backend.auth.model.AuthMethod;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.strategy.AuthenticationStrategy;
import cloud.poesis.itip.web.backend.auth.strategy.AuthenticationStrategyResolver;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private AuthenticationStrategyResolver authenticationStrategyResolver;

  @Mock private AuthenticationStrategy authenticationStrategy;

  @InjectMocks private AuthController authController;

  @Test
  void loginShouldReturnJwtResponse() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

    when(authenticationStrategyResolver.resolve(AuthMethod.LOCAL))
        .thenReturn(authenticationStrategy);
    when(authenticationStrategy.authenticate(any()))
        .thenReturn(
            AuthenticationResult.builder()
                .token("jwt-value")
                .email("john.doe@itip.local")
                .expiresAt(Instant.parse("2030-01-01T00:00:00Z"))
                .build());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    """
                    {
                      "email": "john.doe@itip.local",
                      "password": "password"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-value"))
        .andExpect(jsonPath("$.email").value("john.doe@itip.local"))
        .andExpect(jsonPath("$.expiresAt").isNumber());
  }
}
