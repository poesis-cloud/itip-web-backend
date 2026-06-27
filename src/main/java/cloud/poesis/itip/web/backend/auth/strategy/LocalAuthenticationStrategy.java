package cloud.poesis.itip.web.backend.auth.strategy;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.model.AuthMethod;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.service.AccountService;
import cloud.poesis.itip.web.backend.auth.service.AuthTokenService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalAuthenticationStrategy implements AuthenticationStrategy {

  private final AuthenticationManager authenticationManager;
  private final AccountService accountService;
  private final AuthTokenService authTokenService;

  @Override
  public AuthenticationResult authenticate(AuthenticationRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    Account account = accountService.loadAccountByEmail(request.getEmail());
    String token = authTokenService.generateToken(account);
    Instant expiresAt = Instant.now().plusMillis(authTokenService.getExpirationMs());

    return AuthenticationResult.builder()
        .token(token)
        .email(account.getEmail())
        .expiresAt(expiresAt)
        .build();
  }

  @Override
  public boolean supports(AuthMethod authMethod) {
    return AuthMethod.LOCAL == authMethod;
  }
}
