package cloud.poesis.itip.web.backend.auth.api;

import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheckManyRequest;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheckManyResponse;
import cloud.poesis.itip.web.backend.auth.service.AuthorizationService;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authorization")
@RequiredArgsConstructor
public class AuthorizationController {

  private final AuthorizationService authorizationService;

  @PostMapping("/check-many")
  public ResponseEntity<AuthorizationCheckManyResponse> checkMany(
      Authentication authentication, @Valid @RequestBody AuthorizationCheckManyRequest request) {
    Objects.requireNonNull(authentication, "authentication is required");
    return ResponseEntity.ok(
        new AuthorizationCheckManyResponse(
            authorizationService.checkMany(authentication.getName(), request.checks())));
  }
}
