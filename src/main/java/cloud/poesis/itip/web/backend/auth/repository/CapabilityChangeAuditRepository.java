package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.CapabilityChangeAudit;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface CapabilityChangeAuditRepository extends Repository<CapabilityChangeAudit, UUID> {

  CapabilityChangeAudit save(CapabilityChangeAudit audit);
}
