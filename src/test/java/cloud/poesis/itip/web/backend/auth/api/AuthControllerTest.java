package cloud.poesis.itip.web.backend.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cloud.poesis.itip.web.backend.auth.model.AccountProfile;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.service.AccountService;
import cloud.poesis.itip.web.backend.auth.strategy.EmailPasswordAuthenticationStrategy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private EmailPasswordAuthenticationStrategy authenticationStrategy;

  @Mock private AccountService accountService;

  @InjectMocks private AuthController authController;

  private MockMvc buildMockMvc() {
    return MockMvcBuilders.standaloneSetup(authController).build();
  }

  @Test
  void loginShouldReturnJwtResponse() throws Exception {
    MockMvc mockMvc = buildMockMvc();

    when(authenticationStrategy.authenticate(any()))
        .thenReturn(
            AuthenticationResult.builder()
                .token("jwt-value")
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
        .andExpect(jsonPath("$.expiresAt").isNotEmpty());
  }

  @Test
  void loginShouldReturnUnauthorizedWhenAuthenticationFails() throws Exception {
    MockMvc mockMvc = buildMockMvc();

    when(authenticationStrategy.authenticate(any()))
        .thenThrow(new BadCredentialsException("invalid credentials"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    """
                    {
                      "email": "john.doe@itip.local",
                      "password": "wrong"
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginShouldReturnInternalServerErrorOnUnexpectedRuntimeException() throws Exception {
    MockMvc mockMvc = buildMockMvc();

    when(authenticationStrategy.authenticate(any()))
        .thenThrow(new RuntimeException("unexpected error"));

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
        .andExpect(status().isInternalServerError());
  }

  @Test
  void loginShouldReturnBadRequestWhenEmailIsBlank() throws Exception {
    MockMvc mockMvc = buildMockMvc();

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    """
                    {
                      "email": " ",
                      "password": "password"
                    }
                    """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(authenticationStrategy);
  }

  @Test
  void loginShouldReturnBadRequestWhenPasswordIsMissing() throws Exception {
    MockMvc mockMvc = buildMockMvc();

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content(
                    """
                    {
                      "email": "john.doe@itip.local"
                    }
                    """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(authenticationStrategy);
  }

  @Test
  void meShouldReturnAuthenticatedAccountProfile() {
    UUID accountId = UUID.randomUUID();
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            "john.doe@itip.local", "ignored", List.of());
    when(accountService.describeAccount("john.doe@itip.local"))
        .thenReturn(
            new AccountProfile(
                accountId,
                "john.doe@itip.local",
                "John Doe",
                List.of("ADMIN"),
                List.of("ITIP:ACCOUNT:READ")));

    ResponseEntity<MeResponse> response = authController.me(authentication);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .isEqualTo(
            new MeResponse(
                accountId.toString(),
                "john.doe@itip.local",
                "John Doe",
                List.of("ADMIN"),
                List.of("ITIP:ACCOUNT:READ")));
  }

  @Test
  void meShouldReturnUnauthorizedWhenProfileIsMissing() {
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            "missing@itip.local", "ignored", List.of());
    when(accountService.describeAccount("missing@itip.local"))
        .thenThrow(new UsernameNotFoundException("Account not found: missing@itip.local"));

    assertThat(authController.me(authentication).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void meShouldReturnUnauthorizedWhenProfileWasDeletedAfterAuthentication() {
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            "deleted@itip.local", "ignored", List.of());
    when(accountService.describeAccount("deleted@itip.local"))
        .thenThrow(new UsernameNotFoundException("Account not found: deleted@itip.local"));

    assertThat(authController.me(authentication).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void meShouldReturnUnauthorizedWhenUnauthenticated() {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken("john.doe@itip.local", "ignored");

    assertThat(authController.me(authentication).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    verifyNoInteractions(accountService);
  }
}
