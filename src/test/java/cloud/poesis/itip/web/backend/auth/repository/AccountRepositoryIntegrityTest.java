package cloud.poesis.itip.web.backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityOperation;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityStatus;
import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantAssignment;
import cloud.poesis.itip.web.backend.auth.service.AccountRoleAssignmentService;
import cloud.poesis.itip.web.backend.auth.service.AccountService;
import cloud.poesis.itip.web.backend.auth.service.RoleService;
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
@Import({AccountService.class, AccountRoleAssignmentService.class, RoleService.class})
class AccountRepositoryIntegrityTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private AccountRepository accountRepository;

  @Autowired private AccountRoleAssignmentRepository accountRoleAssignmentRepository;

  @Autowired
  private RoleCapabilityGrantAssignmentRepository roleCapabilityGrantAssignmentRepository;

  @Autowired private AccountService accountService;

  @BeforeEach
  void ensureActiveAssignmentUniquenessForTestDatabase() {
    jdbcTemplate.execute(
        """
        ALTER TABLE role_capability_grant_assignment
        ADD COLUMN IF NOT EXISTS role_id_active UUID GENERATED ALWAYS AS (
          CASE WHEN revoked_at IS NULL THEN role_id ELSE NULL END
        )
        """);
    jdbcTemplate.execute(
        """
        ALTER TABLE role_capability_grant_assignment
        ADD COLUMN IF NOT EXISTS role_capability_grant_id_active UUID GENERATED ALWAYS AS (
          CASE WHEN revoked_at IS NULL THEN role_capability_grant_id ELSE NULL END
        )
        """);
    jdbcTemplate.execute(
        """
        CREATE UNIQUE INDEX IF NOT EXISTS uq_role_capability_grant_assignment_active_test
        ON role_capability_grant_assignment (role_id_active, role_capability_grant_id_active)
        """);
  }

  @Test
  void duplicateRoleCapabilityGrantAssignmentShouldFail() {
    Role role = persistRole("ADMIN");
    RoleCapabilityGrant grant = persistGrant();

    roleCapabilityGrantAssignmentRepository.saveAndFlush(
        Objects.requireNonNull(
            RoleCapabilityGrantAssignment.builder().role(role).roleCapabilityGrant(grant).build()));

    RoleCapabilityGrantAssignment duplicate =
        RoleCapabilityGrantAssignment.builder().role(role).roleCapabilityGrant(grant).build();

    assertThatThrownBy(
            () ->
                roleCapabilityGrantAssignmentRepository.saveAndFlush(
                    Objects.requireNonNull(duplicate)))
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
                    INSERT INTO role_capability_grant_assignment (id, role_id, role_capability_grant_id, assigned_at)
                    VALUES (RANDOM_UUID(), RANDOM_UUID(), RANDOM_UUID(), CURRENT_TIMESTAMP)
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void accountShouldBeReturnedWithoutActiveAssignmentsAndServiceAuthoritiesShouldBeEmpty() {
    Account account = persistAccount("no-active@itip.local");
    Role role = persistRole("OPS");
    RoleCapabilityGrant grant = persistGrant();

    RoleCapabilityGrantAssignment roleCapabilityGrantAssignment =
        RoleCapabilityGrantAssignment.builder().role(role).roleCapabilityGrant(grant).build();
    roleCapabilityGrantAssignmentRepository.saveAndFlush(
        Objects.requireNonNull(roleCapabilityGrantAssignment));

    accountRoleAssignmentRepository.saveAndFlush(
        Objects.requireNonNull(
            AccountRoleAssignment.builder()
                .account(account)
                .role(role)
                .unassignedAt(Instant.now())
                .build()));

    entityManager.clear();

    Account loaded =
        accountRepository.findByEmailWithRolesAndCapabilities("no-active@itip.local").orElseThrow();

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

  private RoleCapabilityGrant persistGrant() {
    Capability capability =
        entityManager.persistFlushFind(
            Capability.builder()
                .resourceOrigin(CapabilityResourceOrigin.ITIP)
                .resource("CAPABILITY")
                .operation(CapabilityOperation.READ)
                .status(CapabilityStatus.ACTIVE)
                .createdBy("test")
                .updatedBy("test")
                .build());
    RoleCapabilityGrant grant =
        RoleCapabilityGrant.builder()
            .capability(capability)
            .createdBy("test")
            .updatedBy("test")
            .build();
    return entityManager.persistFlushFind(grant);
  }
}
