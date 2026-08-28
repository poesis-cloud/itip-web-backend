package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegeRepository extends JpaRepository<Privilege, UUID> {}
