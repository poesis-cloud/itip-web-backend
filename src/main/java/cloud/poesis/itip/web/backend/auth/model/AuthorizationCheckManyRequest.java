package cloud.poesis.itip.web.backend.auth.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AuthorizationCheckManyRequest(@NotEmpty @Valid List<AuthorizationCheck> checks) {

  public AuthorizationCheckManyRequest {
    checks = List.copyOf(checks);
  }
}
