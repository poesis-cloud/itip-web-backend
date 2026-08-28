package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeChangeAudit;
import cloud.poesis.itip.web.backend.auth.repository.PrivilegeChangeAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrivilegeChangeAuditService {

  private final PrivilegeChangeAuditRepository privilegeChangeAuditRepository;

  // Spring Data nullness contracts can conflict with JDT inference on generic save().
  @SuppressWarnings("null")
  public PrivilegeChangeAudit recordChange(
      Privilege privilege,
      PrivilegeAuditActionType actionType,
      String previousState,
      String newState,
      String actionBy) {
    PrivilegeChangeAudit audit =
        PrivilegeChangeAudit.builder()
            .privilege(privilege)
            .privilegeVersion(privilege.getVersion())
            .actionType(actionType)
            .previousState(previousState)
            .newState(newState)
            .actionBy(actionBy)
            .build();
    return privilegeChangeAuditRepository.save(audit);
  }
}
