package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private AuthTokenService authTokenService;

  @Mock private AccountService accountService;

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSkipWhenAuthorizationHeaderMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    jwtAuthenticationFilter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(authTokenService, accountService);
  }

  @Test
  void shouldSkipWhenTokenCannotBeParsed() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    when(authTokenService.extractEmail("invalid-token"))
        .thenThrow(new RuntimeException("bad token"));

    jwtAuthenticationFilter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldAuthenticateWhenTokenIsValid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    UserDetails userDetails =
        User.withUsername("auth@itip.local").password("ignored").authorities("READ_USER").build();

    when(authTokenService.extractEmail("valid-token")).thenReturn("auth@itip.local");
    when(accountService.loadUserByUsername("auth@itip.local")).thenReturn(userDetails);
    when(authTokenService.isTokenValid("valid-token", userDetails)).thenReturn(true);

    jwtAuthenticationFilter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo("auth@itip.local");
  }

  @Test
  void shouldSkipWhenAuthorizationHeaderDoesNotStartWithBearerPrefix() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Basic abc123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    jwtAuthenticationFilter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(authTokenService, accountService);
  }

  @Test
  void shouldNotOverrideExistingAuthenticationInSecurityContext() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    UserDetails existingUser =
        User.withUsername("existing@itip.local")
            .password("ignored")
            .authorities("READ_USER")
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                existingUser, null, existingUser.getAuthorities()));

    when(authTokenService.extractEmail("valid-token")).thenReturn("new@itip.local");

    jwtAuthenticationFilter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo("existing@itip.local");
    verifyNoInteractions(accountService);
  }

  @Test
  void shouldSkipWhenExtractedEmailIsBlank() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    when(authTokenService.extractEmail("valid-token")).thenReturn(" ");

    jwtAuthenticationFilter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verifyNoInteractions(accountService);
  }

  @Test
  void shouldNotAuthenticateWhenTokenValidationFails() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    UserDetails userDetails =
        User.withUsername("auth@itip.local").password("ignored").authorities("READ_USER").build();

    when(authTokenService.extractEmail("invalid-token")).thenReturn("auth@itip.local");
    when(accountService.loadUserByUsername("auth@itip.local")).thenReturn(userDetails);
    when(authTokenService.isTokenValid("invalid-token", userDetails)).thenReturn(false);

    jwtAuthenticationFilter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
