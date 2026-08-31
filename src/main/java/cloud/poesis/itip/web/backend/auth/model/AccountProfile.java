package cloud.poesis.itip.web.backend.auth.model;

import java.util.List;
import java.util.UUID;

public record AccountProfile(
    UUID id, String email, String fullName, List<String> roles, List<String> privileges) {

  public AccountProfile {
    roles = List.copyOf(roles);
    privileges = List.copyOf(privileges);
  }
}
