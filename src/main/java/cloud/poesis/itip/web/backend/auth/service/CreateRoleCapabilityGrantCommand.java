package cloud.poesis.itip.web.backend.auth.service;

import java.util.Set;
import java.util.UUID;

public record CreateRoleCapabilityGrantCommand(UUID capabilityId, Set<UUID> policyIds) {

  public CreateRoleCapabilityGrantCommand {
    policyIds = policyIds == null ? null : Set.copyOf(policyIds);
  }
}
