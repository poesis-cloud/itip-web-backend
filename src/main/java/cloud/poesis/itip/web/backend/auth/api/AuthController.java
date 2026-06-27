package cloud.poesis.itip.web.backend.auth.api;

import cloud.poesis.itip.web.backend.auth.model.AuthenticationRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.strategy.AuthenticationStrategy;
import cloud.poesis.itip.web.backend.auth.strategy.AuthenticationStrategyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationStrategyResolver authenticationStrategyResolver;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    AuthenticationStrategy strategy = authenticationStrategyResolver.resolve(request.getAuthMethod());
    AuthenticationResult result =
        strategy.authenticate(
            AuthenticationRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .authMethod(request.getAuthMethod())
                .build());

    return ResponseEntity.ok(
        LoginResponse.builder()
            .token(result.getToken())
            .email(result.getEmail())
            .expiresAt(result.getExpiresAt())
            .build());
  }
}
