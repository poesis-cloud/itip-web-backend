package cloud.poesis.itip.web.backend.auth.model;

import java.util.List;

public record AuthorizationCheckManyResponse(List<AuthorizationDecision> decisions) {

  public AuthorizationCheckManyResponse {
    decisions = List.copyOf(decisions);
  }
}
