package cloud.poesis.itip.web.backend.auth.repository;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  Optional<Account> findByEmail(String email);

  @Query(
      """
      SELECT DISTINCT a
      FROM Account a
      LEFT JOIN FETCH a.accountRoleAssignments ara
      LEFT JOIN FETCH ara.role r
      LEFT JOIN FETCH r.rolePrivilegeAssignments rpa
      LEFT JOIN FETCH rpa.privilege p
      WHERE a.email = :email
        AND (ara IS NULL OR (ara.revokedAt IS NULL AND (ara.expiresAt IS NULL OR ara.expiresAt > CURRENT_TIMESTAMP)))
        AND (rpa IS NULL OR rpa.revokedAt IS NULL)
      """)
  Optional<Account> findByEmailWithRolesAndPrivileges(@Param("email") String email);
}
