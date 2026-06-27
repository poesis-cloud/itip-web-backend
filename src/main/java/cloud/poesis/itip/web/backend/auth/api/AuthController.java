package cloud.poesis.itip.web.backend.auth.api;

import cloud.poesis.itip.web.backend.auth.model.AuthenticationRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.strategy.AuthenticationStrategy;
import cloud.poesis.itip.web.backend.auth.strategy.AuthenticationStrategyResolver;
import cloud.poesis.itip.web.backend.auth.strategy.UnsupportedAuthMethodException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthenticationStrategyResolver authenticationStrategyResolver;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    String email = request.getEmail();
    try {
      AuthenticationStrategy strategy =
          authenticationStrategyResolver.resolve(request.getAuthMethod());
      AuthenticationResult result =
          strategy.authenticate(
              AuthenticationRequest.builder()
                  .email(email)
                  .password(request.getPassword())
                  .authMethod(request.getAuthMethod())
                  .build());

      return ResponseEntity.ok(
          LoginResponse.builder()
              .token(result.getToken())
              .email(result.getEmail())
              .expiresAt(result.getExpiresAt())
              .build());
    } catch (UnsupportedAuthMethodException exception) {
      log.warn("Login rejected due to unsupported auth method for email={}", email);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    } catch (AuthenticationException exception) {
      log.warn(
          "Login failed for email={} with type={} and reason={}",
          email,
          exception.getClass().getSimpleName(),
          exception.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (RuntimeException exception) {
      log.error("Unexpected login error for email={}", email, exception);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
