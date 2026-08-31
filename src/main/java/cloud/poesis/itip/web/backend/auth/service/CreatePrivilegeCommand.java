package cloud.poesis.itip.web.backend.auth.service;

import java.util.Set;
import java.util.UUID;

public record CreatePrivilegeCommand(UUID capabilityId, Set<UUID> policyIds) {

  public CreatePrivilegeCommand {
    policyIds = policyIds == null ? null : Set.copyOf(policyIds);
  }
}
