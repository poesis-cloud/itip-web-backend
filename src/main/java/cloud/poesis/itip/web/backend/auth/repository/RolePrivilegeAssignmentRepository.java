package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.RolePrivilegeAssignment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePrivilegeAssignmentRepository
    extends JpaRepository<RolePrivilegeAssignment, UUID> {}
