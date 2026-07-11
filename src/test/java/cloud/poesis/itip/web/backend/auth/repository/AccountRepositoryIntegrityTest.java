package cloud.poesis.itip.web.backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.entity.RolePrivilegeAssignment;
import cloud.poesis.itip.web.backend.auth.service.AccountService;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;

@DataJpaTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false",
      "spring.jpa.properties.hibernate.format_sql=true"
    })
@Import(AccountService.class)
class AccountRepositoryIntegrityTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private AccountRepository accountRepository;

  @Autowired private AccountRoleAssignmentRepository accountRoleAssignmentRepository;

  @Autowired private RolePrivilegeAssignmentRepository rolePrivilegeAssignmentRepository;

  @Autowired private AccountService accountService;

  @BeforeEach
  void ensureActiveAssignmentUniquenessForTestDatabase() {
    jdbcTemplate.execute(
        """
        ALTER TABLE role_privilege_assignment
        ADD COLUMN IF NOT EXISTS role_id_active UUID GENERATED ALWAYS AS (
          CASE WHEN revoked_at IS NULL THEN role_id ELSE NULL END
        )
        """);
    jdbcTemplate.execute(
        """
        ALTER TABLE role_privilege_assignment
        ADD COLUMN IF NOT EXISTS privilege_id_active UUID GENERATED ALWAYS AS (
          CASE WHEN revoked_at IS NULL THEN privilege_id ELSE NULL END
        )
        """);
    jdbcTemplate.execute(
        """
        CREATE UNIQUE INDEX IF NOT EXISTS uq_role_privilege_assignment_active_test
        ON role_privilege_assignment (role_id_active, privilege_id_active)
        """);
  }

  @Test
  void duplicateRolePrivilegeAssignmentShouldFail() {
    Role role = persistRole("ADMIN");
    Privilege privilege = persistPrivilege("READ_USER");

    rolePrivilegeAssignmentRepository.saveAndFlush(
        Objects.requireNonNull(
            RolePrivilegeAssignment.builder().role(role).privilege(privilege).build()));

    RolePrivilegeAssignment duplicate =
        RolePrivilegeAssignment.builder().role(role).privilege(privilege).build();

    assertThatThrownBy(
            () -> rolePrivilegeAssignmentRepository.saveAndFlush(Objects.requireNonNull(duplicate)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void expiredAccountRoleAssignmentShouldAllowRegrant() {
    Account account = persistAccount("expired@itip.local");
    Role role = persistRole("TEMP_REVIEWER");

    accountRoleAssignmentRepository.saveAndFlush(
        Objects.requireNonNull(
            AccountRoleAssignment.builder()
                .account(account)
                .role(role)
                .expiresAt(Instant.now().minusSeconds(60))
                .build()));

    AccountRoleAssignment regrant =
        AccountRoleAssignment.builder().account(account).role(role).build();

    accountRoleAssignmentRepository.saveAndFlush(Objects.requireNonNull(regrant));

    assertThat(accountRoleAssignmentRepository.findAll())
        .filteredOn(assignment -> assignment.getAccount().getId().equals(account.getId()))
        .hasSize(2);
  }

  @Test
  void invalidForeignKeysShouldFail() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO role_privilege_assignment (id, role_id, privilege_id, assigned_at)
                    VALUES (RANDOM_UUID(), RANDOM_UUID(), RANDOM_UUID(), CURRENT_TIMESTAMP)
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void accountShouldBeReturnedWithoutActiveAssignmentsAndServiceAuthoritiesShouldBeEmpty() {
    Account account = persistAccount("no-active@itip.local");
    Role role = persistRole("OPS");
    Privilege privilege = persistPrivilege("READ_ROLE");

    RolePrivilegeAssignment rolePrivilegeAssignment =
        RolePrivilegeAssignment.builder().role(role).privilege(privilege).build();
    rolePrivilegeAssignmentRepository.saveAndFlush(Objects.requireNonNull(rolePrivilegeAssignment));

    accountRoleAssignmentRepository.saveAndFlush(
        Objects.requireNonNull(
            AccountRoleAssignment.builder()
                .account(account)
                .role(role)
                .unassignedAt(Instant.now())
                .build()));

    entityManager.clear();

    Account loaded =
        accountRepository.findByEmailWithRolesAndPrivileges("no-active@itip.local").orElseThrow();

    assertThat(loaded.getEmail()).isEqualTo("no-active@itip.local");
    assertThat(loaded.getAccountRoleAssignments())
        .allMatch(
            assignment ->
                assignment.getUnassignedAt() != null
                    || (assignment.getExpiresAt() != null
                        && !assignment.getExpiresAt().isAfter(Instant.now())));

    UserDetails userDetails = accountService.loadUserByUsername("no-active@itip.local");
    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  private Account persistAccount(String email) {
    Account account =
        Account.builder()
            .email(email)
            .passwordHash("hashed-password")
            .fullName("Test User")
            .enabled(true)
            .build();
    return entityManager.persistFlushFind(account);
  }

  private Role persistRole(String name) {
    Role role = Role.builder().name(name).build();
    return entityManager.persistFlushFind(role);
  }

  private Privilege persistPrivilege(String code) {
    Privilege privilege = Privilege.builder().code(code).build();
    return entityManager.persistFlushFind(privilege);
  }
}
