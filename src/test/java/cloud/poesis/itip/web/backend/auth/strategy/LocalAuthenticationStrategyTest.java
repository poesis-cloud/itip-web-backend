package cloud.poesis.itip.web.backend.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.model.AuthMethod;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.service.AccountService;
import cloud.poesis.itip.web.backend.auth.service.AuthTokenService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

@ExtendWith(MockitoExtension.class)
class LocalAuthenticationStrategyTest {

  @Mock private AuthenticationManager authenticationManager;

  @Mock private AccountService accountService;

  @Mock private AuthTokenService authTokenService;

  @InjectMocks private LocalAuthenticationStrategy localAuthenticationStrategy;

  @Test
  void authenticateShouldReturnJwtAuthenticationResult() {
    AuthenticationRequest request =
        AuthenticationRequest.builder()
            .email("local@itip.local")
            .password("secret")
            .authMethod(AuthMethod.LOCAL)
            .build();
    Account account = Account.builder().id(UUID.randomUUID()).email("local@itip.local").build();

    when(accountService.loadAccountByEmail("local@itip.local")).thenReturn(account);
    when(authTokenService.generateToken(account)).thenReturn("jwt-token");
    when(authTokenService.getExpirationMs()).thenReturn(3600000L);

    Instant before = Instant.now();
    AuthenticationResult result = localAuthenticationStrategy.authenticate(request);
    Instant after = Instant.now();

    verify(authenticationManager).authenticate(any());
    assertThat(result.getToken()).isEqualTo("jwt-token");
    assertThat(result.getEmail()).isEqualTo("local@itip.local");
    assertThat(result.getExpiresAt())
        .isAfterOrEqualTo(before.plusMillis(3600000L))
        .isBeforeOrEqualTo(after.plusMillis(3600000L).plusSeconds(1));
  }

  @Test
  void supportsShouldOnlyReturnTrueForLocalMethod() {
    assertThat(localAuthenticationStrategy.supports(AuthMethod.LOCAL)).isTrue();
    assertThat(localAuthenticationStrategy.supports(AuthMethod.GOOGLE)).isFalse();
  }
}
