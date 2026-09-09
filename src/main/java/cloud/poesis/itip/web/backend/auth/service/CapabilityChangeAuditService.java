package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityChangeAudit;
import cloud.poesis.itip.web.backend.auth.repository.CapabilityChangeAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CapabilityChangeAuditService {

  private final CapabilityChangeAuditRepository capabilityChangeAuditRepository;

  // Spring Data nullness contracts can conflict with JDT inference on generic save().
  @SuppressWarnings("null")
  public CapabilityChangeAudit recordChange(
      Capability capability,
      CapabilityAuditActionType actionType,
      String previousState,
      String newState,
      String actionBy) {
    CapabilityChangeAudit audit =
        CapabilityChangeAudit.builder()
            .capability(capability)
            .capabilityVersion(capability.getVersion())
            .actionType(actionType)
            .previousState(previousState)
            .newState(newState)
            .actionBy(actionBy)
            .build();
    return capabilityChangeAuditRepository.save(audit);
  }
}
