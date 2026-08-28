package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.PrivilegeChangeAudit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeChangeAuditRepository extends JpaRepository<PrivilegeChangeAudit, UUID> {}
