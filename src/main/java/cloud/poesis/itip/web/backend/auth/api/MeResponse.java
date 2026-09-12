package cloud.poesis.itip.web.backend.auth.api;

import java.util.List;

public record MeResponse(
    String id, String email, String fullName, List<String> roles, List<String> privileges) {

  public MeResponse {
    roles = List.copyOf(roles);
    privileges = List.copyOf(privileges);
  }
}
