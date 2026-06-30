package cloud.poesis.itip.web.backend.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.service.AccessTokenService;
import cloud.poesis.itip.web.backend.auth.service.IdTokenService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class EmailPasswordAuthenticationStrategyTest {

  @Mock private AccessTokenService accessTokenService;

  @Mock private IdTokenService idTokenService;

  @InjectMocks private EmailPasswordAuthenticationStrategy emailPasswordAuthenticationStrategy;

  @Test
  void authenticateShouldReturnJwtAuthenticationResult() {
    AuthenticationRequest request =
        AuthenticationRequest.builder().email("local@itip.local").password("secret").build();
    Account account = Account.builder().id(UUID.randomUUID()).email("local@itip.local").build();

    when(idTokenService.identify(request)).thenReturn(account);
    when(accessTokenService.generateToken(account)).thenReturn("jwt-token");
    when(accessTokenService.getExpirationMs()).thenReturn(3600000L);

    Instant before = Instant.now();
    AuthenticationResult result = emailPasswordAuthenticationStrategy.authenticate(request);
    Instant after = Instant.now();

    verify(idTokenService).identify(request);
    assertThat(result.getToken()).isEqualTo("jwt-token");
    assertThat(result.getExpiresAt())
        .isAfterOrEqualTo(before.plusMillis(3600000L))
        .isBeforeOrEqualTo(after.plusMillis(3600000L).plusSeconds(1));
  }

  @Test
  void authenticateShouldRethrowAuthenticationException() {
    AuthenticationRequest request =
        AuthenticationRequest.builder().email("local@itip.local").password("wrong").build();

    when(idTokenService.identify(request))
        .thenThrow(new BadCredentialsException("bad credentials"));

    assertThatThrownBy(() -> emailPasswordAuthenticationStrategy.authenticate(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("bad credentials");

    verify(accessTokenService, never()).generateToken(any());
  }

  @Test
  void authenticateShouldWrapUnexpectedRuntimeExceptionInAuthenticationServiceException() {
    AuthenticationRequest request =
        AuthenticationRequest.builder().email("local@itip.local").password("secret").build();

    RuntimeException unexpected = new RuntimeException("ldap down");
    when(idTokenService.identify(request)).thenThrow(unexpected);

    assertThatThrownBy(() -> emailPasswordAuthenticationStrategy.authenticate(request))
        .isInstanceOf(AuthenticationServiceException.class)
        .hasMessage("Unexpected authentication error")
        .hasCause(unexpected);

    verify(accessTokenService, never()).generateToken(any());
  }
}
