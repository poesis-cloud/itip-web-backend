package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.entity.RolePrivilegeAssignment;
import cloud.poesis.itip.web.backend.auth.repository.AccountRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private AccountRepository accountRepository;

  @Mock private RoleService roleService;

  @Mock private AccountRoleAssignmentService accountRoleAssignmentService;

  @InjectMocks private AccountService accountService;

  @Test
  @SuppressWarnings("null")
  void createShouldSaveAccountThenAssignResolvedDefaultRole() {
    Account account = Account.builder().email("new@itip.local").build();
    Account savedAccount = Account.builder().id(UUID.randomUUID()).email("new@itip.local").build();
    Role defaultRole =
        Role.builder().id(UUID.randomUUID()).name(RoleService.DEFAULT_ROLE_NAME).build();
    when(accountRepository.save(account)).thenReturn(savedAccount);
    when(roleService.requireDefaultRole()).thenReturn(defaultRole);

    Account created = accountService.create(account);

    assertThat(created).isSameAs(savedAccount);
    InOrder inOrder = inOrder(accountRepository, roleService, accountRoleAssignmentService);
    inOrder.verify(accountRepository).save(account);
    inOrder.verify(roleService).requireDefaultRole();
    inOrder.verify(accountRoleAssignmentService).assign(savedAccount, defaultRole);
  }

  @Test
  @SuppressWarnings("null")
  void createShouldPropagateFailureWhenDefaultRoleIsMissing() {
    Account account = Account.builder().email("new@itip.local").build();
    Account savedAccount = Account.builder().id(UUID.randomUUID()).email("new@itip.local").build();
    when(accountRepository.save(account)).thenReturn(savedAccount);
    when(roleService.requireDefaultRole())
        .thenThrow(new IllegalStateException("Default role ITIP_USER is not configured"));

    assertThatThrownBy(() -> accountService.create(account))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Default role ITIP_USER is not configured");

    verifyNoInteractions(accountRoleAssignmentService);
  }

  @Test
  void loadUserByUsernameShouldReturnUserDetailsWithOnlyActiveAuthorities() {
    Privilege activePrivilege = allowPrivilege("ACCOUNT", PrivilegeAction.READ);
    RolePrivilegeAssignment activeRolePrivilegeAssignment =
        RolePrivilegeAssignment.builder().id(UUID.randomUUID()).privilege(activePrivilege).build();

    Privilege disabledCapabilityPrivilege = allowPrivilege("ACCOUNT", PrivilegeAction.DISABLE);
    disabledCapabilityPrivilege.getCapability().setEnabled(false);
    RolePrivilegeAssignment disabledCapabilityRolePrivilegeAssignment =
        RolePrivilegeAssignment.builder()
            .id(UUID.randomUUID())
            .privilege(disabledCapabilityPrivilege)
            .build();

    Privilege revokedPrivilege = allowPrivilege("PRIVILEGE", PrivilegeAction.CREATE);
    RolePrivilegeAssignment revokedRolePrivilegeAssignment =
        RolePrivilegeAssignment.builder()
            .id(UUID.randomUUID())
            .privilege(revokedPrivilege)
            .unassignedAt(Instant.now())
            .build();

    Role activeRole =
        Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .rolePrivilegeAssignments(
                Set.of(
                    activeRolePrivilegeAssignment,
                    disabledCapabilityRolePrivilegeAssignment,
                    revokedRolePrivilegeAssignment))
            .build();

    AccountRoleAssignment activeRoleAssignment =
        AccountRoleAssignment.builder().id(UUID.randomUUID()).role(activeRole).build();

    AccountRoleAssignment expiredRoleAssignment =
        AccountRoleAssignment.builder()
            .id(UUID.randomUUID())
            .role(activeRole)
            .expiresAt(Instant.now().minusSeconds(300))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("john.doe@itip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(Set.of(activeRoleAssignment, expiredRoleAssignment))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("john.doe@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("john.doe@itip.local");

    assertThat(userDetails.getUsername()).isEqualTo("john.doe@itip.local");
    assertThat(userDetails.getPassword()).isEqualTo("hashed-password");
    assertThat(userDetails.isEnabled()).isTrue();
    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("ITIP:ACCOUNT:READ");
  }

  @Test
  void loadUserByUsernameShouldThrowWhenAccountNotFound() {
    when(accountRepository.findByEmailWithRolesAndPrivileges("missing@itip.local"))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> accountService.loadUserByUsername("missing@itip.local"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("missing@itip.local");
  }

  @Test
  void loadAccountByEmailShouldReturnAccountWhenExists() {
    Account account = Account.builder().email("admin@itip.local").build();
    when(accountRepository.findByEmail("admin@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    Account loaded = accountService.loadAccountByEmail("admin@itip.local");
    assertThat(loaded.getEmail()).isEqualTo("admin@itip.local");
  }

  @Test
  void loadAccountByEmailShouldThrowWhenMissing() {
    when(accountRepository.findByEmail("missing@itip.local"))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> accountService.loadAccountByEmail("missing@itip.local"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("missing@itip.local");
  }

  @Test
  void loadUserByUsernameShouldHandleNullAssignmentsAndDisabledAccount() {
    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("disabled@itip.local")
            .passwordHash("hashed-password")
            .enabled(false)
            .accountRoleAssignments(null)
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("disabled@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("disabled@itip.local");

    assertThat(userDetails.getUsername()).isEqualTo("disabled@itip.local");
    assertThat(userDetails.isEnabled()).isFalse();
    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldIgnoreIncompletePrivilegesAndKeepValidAuthorities() {
    Privilege validPrivilege = allowPrivilege("ACCOUNT", PrivilegeAction.READ);
    Privilege incompletePrivilege = allowPrivilege(null, PrivilegeAction.READ);

    Role roleWithNullPrivilegeAssignments =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_PRIV_ASSIGNMENTS")
            .rolePrivilegeAssignments(null)
            .build();

    Role roleWithNullPrivilege =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_PRIVILEGE")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(null)
                        .build()))
            .build();

    Role roleWithIncompletePrivilege =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_PRIVILEGE_CODE")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(incompletePrivilege)
                        .build()))
            .build();

    Role roleWithValidPrivilege =
        Role.builder()
            .id(UUID.randomUUID())
            .name("VALID")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(validPrivilege)
                        .build()))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("mixed@itip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder().id(UUID.randomUUID()).role(null).build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithNullPrivilegeAssignments)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithNullPrivilege)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithIncompletePrivilege)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithValidPrivilege)
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("mixed@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("mixed@itip.local");

    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("ITIP:ACCOUNT:READ");
  }

  @Test
  void loadUserByUsernameShouldIgnoreRevokedRoleAssignments() {
    Privilege privilege = allowPrivilege("ACCOUNT", PrivilegeAction.READ);
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(privilege)
                        .build()))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("revoked@itip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(role)
                        .unassignedAt(Instant.now())
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("revoked@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("revoked@itip.local");

    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldIgnoreExpiredRoleAssignmentsEvenWhenNotRevoked() {
    Privilege privilege = allowPrivilege("ACCOUNT", PrivilegeAction.READ);
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("EXPIRED_ROLE")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(privilege)
                        .build()))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("expired@itip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(role)
                        .expiresAt(Instant.now().minusSeconds(10))
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("expired@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("expired@itip.local");

    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldAcceptFutureExpiringRoleAssignments() {
    Privilege privilege = allowPrivilege("ACCOUNT", PrivilegeAction.READ);
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("FUTURE_ROLE")
            .rolePrivilegeAssignments(
                Set.of(
                    RolePrivilegeAssignment.builder()
                        .id(UUID.randomUUID())
                        .privilege(privilege)
                        .build()))
            .build();

    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("future@itip.local")
            .passwordHash("hashed-password")
            .enabled(true)
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(role)
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndPrivileges("future@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("future@itip.local");

    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("ITIP:ACCOUNT:READ");
  }

  private static Privilege allowPrivilege(String resource, PrivilegeAction operation) {
    Capability capability =
        Capability.builder()
            .id(UUID.randomUUID())
            .resourceOrigin(PrivilegeResourceOrigin.ITIP)
            .resource(resource)
            .operation(operation)
            .enabled(true)
            .build();
    return Privilege.builder().id(UUID.randomUUID()).capability(capability).build();
  }
}
