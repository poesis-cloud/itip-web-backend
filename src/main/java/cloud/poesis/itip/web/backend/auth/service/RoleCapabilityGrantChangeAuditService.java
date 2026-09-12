package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantChangeAudit;
import cloud.poesis.itip.web.backend.auth.repository.RoleCapabilityGrantChangeAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleCapabilityGrantChangeAuditService {

  private final RoleCapabilityGrantChangeAuditRepository roleCapabilityGrantChangeAuditRepository;

  // Spring Data nullness contracts can conflict with JDT inference on generic save().
  @SuppressWarnings("null")
  public RoleCapabilityGrantChangeAudit recordChange(
      RoleCapabilityGrant roleCapabilityGrant,
      RoleCapabilityGrantAuditActionType actionType,
      String previousState,
      String newState,
      String actionBy) {
    RoleCapabilityGrantChangeAudit audit =
        RoleCapabilityGrantChangeAudit.builder()
            .roleCapabilityGrant(roleCapabilityGrant)
            .roleCapabilityGrantVersion(roleCapabilityGrant.getVersion())
            .actionType(actionType)
            .previousState(previousState)
            .newState(newState)
            .actionBy(actionBy)
            .build();
    return roleCapabilityGrantChangeAuditRepository.save(audit);
  }
}
