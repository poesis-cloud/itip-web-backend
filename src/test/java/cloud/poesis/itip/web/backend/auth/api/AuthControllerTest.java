package cloud.poesis.itip.web.backend.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
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
  void meShouldReturnCurrentUserProfileForAuthenticatedPrincipal() throws Exception {
    UUID accountId = UUID.randomUUID();
    UserDetails principal =
        User.withUsername("john.doe@itip.local")
            .password("hashed-password")
            .authorities("READ_USER")
            .build();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      MockMvc mockMvc =
          MockMvcBuilders.standaloneSetup(authController)
              .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
              .build();

      when(accountService.describeAccount("john.doe@itip.local"))
          .thenReturn(
              AccountProfile.builder()
                  .id(accountId)
                  .email("john.doe@itip.local")
                  .fullName("John Doe")
                  .roles(List.of("ADMIN", "VIEWER"))
                  .privileges(List.of("READ_USER", "WRITE_USER"))
                  .build());

      mockMvc
          .perform(get("/api/auth/me"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(accountId.toString()))
          .andExpect(jsonPath("$.email").value("john.doe@itip.local"))
          .andExpect(jsonPath("$.fullName").value("John Doe"))
          .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
          .andExpect(jsonPath("$.roles[1]").value("VIEWER"))
          .andExpect(jsonPath("$.privileges[0]").value("READ_USER"))
          .andExpect(jsonPath("$.privileges[1]").value("WRITE_USER"));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void meShouldReturnUnauthorizedWhenAccountNoLongerResolves() throws Exception {
    UserDetails principal =
        User.withUsername("deleted.user@itip.local")
            .password("hashed-password")
            .authorities("READ_USER")
            .build();
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      MockMvc mockMvc =
          MockMvcBuilders.standaloneSetup(authController)
              .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
              .build();

      when(accountService.describeAccount("deleted.user@itip.local"))
          .thenThrow(new UsernameNotFoundException("Account not found: deleted.user@itip.local"));

      mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
