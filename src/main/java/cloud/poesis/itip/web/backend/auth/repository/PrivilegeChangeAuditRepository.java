package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.PrivilegeChangeAudit;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface PrivilegeChangeAuditRepository extends Repository<PrivilegeChangeAudit, UUID> {

  PrivilegeChangeAudit save(PrivilegeChangeAudit audit);
}
