package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleCapabilityGrantRepository extends JpaRepository<RoleCapabilityGrant, UUID> {}
