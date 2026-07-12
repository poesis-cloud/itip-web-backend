package cloud.poesis.itip.web.backend.auth.strategy;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.service.AccessTokenService;
import cloud.poesis.itip.web.backend.auth.service.IdTokenService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailPasswordAuthenticationStrategy {

  private final AccessTokenService accessTokenService;
  private final IdTokenService idTokenService;

  public AuthenticationResult authenticate(AuthenticationRequest request) {
    try {
      Account account = idTokenService.identify(request);
      String token = accessTokenService.generateToken(account);
      Instant expiresAt = Instant.now().plusMillis(accessTokenService.getExpirationMs());

      return AuthenticationResult.builder().token(token).expiresAt(expiresAt).build();
    } catch (AuthenticationException exception) {
      log.warn(
          "Email/password authentication failed for email={} with type={} and reason={}",
          request.getEmail(),
          exception.getClass().getSimpleName(),
          exception.getMessage());
      throw exception;
    } catch (RuntimeException exception) {
      log.error(
          "Unexpected email/password authentication error for email={}",
          request.getEmail(),
          exception);
      throw new AuthenticationServiceException("Unexpected authentication error", exception);
    }
  }
}
