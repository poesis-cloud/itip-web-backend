package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantAssignment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleCapabilityGrantAssignmentRepository
    extends JpaRepository<RoleCapabilityGrantAssignment, UUID> {}
