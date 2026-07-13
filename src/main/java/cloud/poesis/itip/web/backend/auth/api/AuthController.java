package cloud.poesis.itip.web.backend.auth.api;

import cloud.poesis.itip.web.backend.auth.model.AccountProfile;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthenticationResult;
import cloud.poesis.itip.web.backend.auth.service.AccountService;
import cloud.poesis.itip.web.backend.auth.strategy.EmailPasswordAuthenticationStrategy;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final EmailPasswordAuthenticationStrategy authenticationStrategy;
  private final AccountService accountService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    if (request == null
        || !StringUtils.hasText(request.getEmail())
        || !StringUtils.hasText(request.getPassword())) {
      return ResponseEntity.badRequest().build();
    }

    String email = request.getEmail();
    try {
      AuthenticationResult result =
          authenticationStrategy.authenticate(
              AuthenticationRequest.builder().email(email).password(request.getPassword()).build());

      return ResponseEntity.ok(
          LoginResponse.builder()
              .token(result.getToken())
              .expiresAt(result.getExpiresAt())
              .build());
    } catch (AuthenticationException exception) {
      log.warn(
          "Login failed for email={} with type={} and reason={}",
          email,
          exception.getClass().getSimpleName(),
          exception.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    } catch (RuntimeException exception) {
      log.error("Unexpected login error for email={}", email, exception);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/me")
  public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    AccountProfile profile;
    try {
      profile = accountService.describeAccount(userDetails.getUsername());
    } catch (UsernameNotFoundException exception) {
      log.warn(
          "Authenticated principal no longer resolves to an account: username={}",
          userDetails.getUsername());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(
        MeResponse.builder()
            .id(Objects.toString(profile.getId(), null))
            .email(profile.getEmail())
            .fullName(profile.getFullName())
            .roles(profile.getRoles())
            .privileges(profile.getPrivileges())
            .build());
  }
}
