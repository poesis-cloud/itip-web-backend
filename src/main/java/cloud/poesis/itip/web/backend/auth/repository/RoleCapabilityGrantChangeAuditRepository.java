package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantChangeAudit;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface RoleCapabilityGrantChangeAuditRepository
    extends Repository<RoleCapabilityGrantChangeAudit, UUID> {

  RoleCapabilityGrantChangeAudit save(RoleCapabilityGrantChangeAudit audit);
}
