package cloud.poesis.itip.web.backend.auth.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AuthorizationCheckManyRequest(
    @NotEmpty @Size(max = 100) @Valid List<AuthorizationCheck> checks) {

  public AuthorizationCheckManyRequest {
    checks = List.copyOf(checks);
  }
}
