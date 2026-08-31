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
      LEFT JOIN FETCH p.capability c
      LEFT JOIN FETCH c.policies
      LEFT JOIN FETCH p.policies
      WHERE a.email = :email
        AND (
          ara IS NULL
          OR (ara.unassignedAt IS NULL AND (ara.expiresAt IS NULL OR ara.expiresAt > CURRENT_TIMESTAMP))
          OR NOT EXISTS (
            SELECT 1
            FROM AccountRoleAssignment araActive
            WHERE araActive.account = a
              AND araActive.unassignedAt IS NULL
              AND (araActive.expiresAt IS NULL OR araActive.expiresAt > CURRENT_TIMESTAMP)
          )
        )
        AND (
          ara IS NULL
          OR ara.unassignedAt IS NOT NULL
          OR (ara.expiresAt IS NOT NULL AND ara.expiresAt <= CURRENT_TIMESTAMP)
          OR (rpa IS NULL OR rpa.unassignedAt IS NULL)
          OR NOT EXISTS (
            SELECT 1
            FROM RolePrivilegeAssignment rpaActive
            WHERE rpaActive.role = ara.role
              AND rpaActive.unassignedAt IS NULL
          )
        )
      """)
  Optional<Account> findByEmailWithRolesAndPrivileges(@Param("email") String email);
}
