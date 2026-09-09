package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityOperation;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityStatus;
import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantAssignment;
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
    RoleCapabilityGrant activeGrant = allowGrant("ACCOUNT", CapabilityOperation.READ);
    RoleCapabilityGrantAssignment activeGrantAssignment =
        RoleCapabilityGrantAssignment.builder()
            .id(UUID.randomUUID())
            .roleCapabilityGrant(activeGrant)
            .build();

    RoleCapabilityGrant disabledCapabilityGrant =
        allowGrant("ACCOUNT", CapabilityOperation.DISABLE);
    disabledCapabilityGrant.getCapability().setStatus(CapabilityStatus.DISABLED);
    RoleCapabilityGrantAssignment disabledCapabilityGrantAssignment =
        RoleCapabilityGrantAssignment.builder()
            .id(UUID.randomUUID())
            .roleCapabilityGrant(disabledCapabilityGrant)
            .build();

    RoleCapabilityGrant revokedGrant = allowGrant("CAPABILITY", CapabilityOperation.CREATE);
    RoleCapabilityGrantAssignment revokedGrantAssignment =
        RoleCapabilityGrantAssignment.builder()
            .id(UUID.randomUUID())
            .roleCapabilityGrant(revokedGrant)
            .unassignedAt(Instant.now())
            .build();

    Role activeRole =
        Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .roleCapabilityGrantAssignments(
                Set.of(
                    activeGrantAssignment,
                    disabledCapabilityGrantAssignment,
                    revokedGrantAssignment))
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

    when(accountRepository.findByEmailWithRolesAndCapabilities("john.doe@itip.local"))
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
    when(accountRepository.findByEmailWithRolesAndCapabilities("missing@itip.local"))
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

    when(accountRepository.findByEmailWithRolesAndCapabilities("disabled@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("disabled@itip.local");

    assertThat(userDetails.getUsername()).isEqualTo("disabled@itip.local");
    assertThat(userDetails.isEnabled()).isFalse();
    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldIgnoreIncompleteGrantsAndKeepValidAuthorities() {
    RoleCapabilityGrant validGrant = allowGrant("ACCOUNT", CapabilityOperation.READ);
    RoleCapabilityGrant incompleteGrant = allowGrant(null, CapabilityOperation.READ);

    Role roleWithNullGrantAssignments =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_GRANT_ASSIGNMENTS")
            .roleCapabilityGrantAssignments(null)
            .build();

    Role roleWithNullGrant =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_GRANT")
            .roleCapabilityGrantAssignments(
                Set.of(
                    RoleCapabilityGrantAssignment.builder()
                        .id(UUID.randomUUID())
                        .roleCapabilityGrant(null)
                        .build()))
            .build();

    Role roleWithIncompleteGrant =
        Role.builder()
            .id(UUID.randomUUID())
            .name("NULL_GRANT_CODE")
            .roleCapabilityGrantAssignments(
                Set.of(
                    RoleCapabilityGrantAssignment.builder()
                        .id(UUID.randomUUID())
                        .roleCapabilityGrant(incompleteGrant)
                        .build()))
            .build();

    Role roleWithValidGrant =
        Role.builder()
            .id(UUID.randomUUID())
            .name("VALID")
            .roleCapabilityGrantAssignments(
                Set.of(
                    RoleCapabilityGrantAssignment.builder()
                        .id(UUID.randomUUID())
                        .roleCapabilityGrant(validGrant)
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
                        .role(roleWithNullGrantAssignments)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithNullGrant)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithIncompleteGrant)
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(roleWithValidGrant)
                        .build()))
            .build();

    when(accountRepository.findByEmailWithRolesAndCapabilities("mixed@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("mixed@itip.local");

    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("ITIP:ACCOUNT:READ");
  }

  @Test
  void loadUserByUsernameShouldIgnoreRevokedRoleAssignments() {
    RoleCapabilityGrant grant = allowGrant("ACCOUNT", CapabilityOperation.READ);
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .roleCapabilityGrantAssignments(
                Set.of(
                    RoleCapabilityGrantAssignment.builder()
                        .id(UUID.randomUUID())
                        .roleCapabilityGrant(grant)
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

    when(accountRepository.findByEmailWithRolesAndCapabilities("revoked@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("revoked@itip.local");

    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldIgnoreExpiredRoleAssignmentsEvenWhenNotRevoked() {
    RoleCapabilityGrant grant = allowGrant("ACCOUNT", CapabilityOperation.READ);
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("EXPIRED_ROLE")
            .roleCapabilityGrantAssignments(
                Set.of(
                    RoleCapabilityGrantAssignment.builder()
                        .id(UUID.randomUUID())
                        .roleCapabilityGrant(grant)
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

    when(accountRepository.findByEmailWithRolesAndCapabilities("expired@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("expired@itip.local");

    assertThat(userDetails.getAuthorities()).isEmpty();
  }

  @Test
  void loadUserByUsernameShouldAcceptFutureExpiringRoleAssignments() {
    RoleCapabilityGrant grant = allowGrant("ACCOUNT", CapabilityOperation.READ);
    Role role =
        Role.builder()
            .id(UUID.randomUUID())
            .name("FUTURE_ROLE")
            .roleCapabilityGrantAssignments(
                Set.of(
                    RoleCapabilityGrantAssignment.builder()
                        .id(UUID.randomUUID())
                        .roleCapabilityGrant(grant)
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

    when(accountRepository.findByEmailWithRolesAndCapabilities("future@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    UserDetails userDetails = accountService.loadUserByUsername("future@itip.local");

    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactlyInAnyOrder("ITIP:ACCOUNT:READ");
  }

  @Test
  void describeAccountShouldIncludeOnlyActiveRolesAndEnabledCapabilityGrants() {
    RoleCapabilityGrant activeGrant = allowGrant("ACCOUNT", CapabilityOperation.READ);
    RoleCapabilityGrant disabledCapabilityGrant =
        allowGrant("ACCOUNT", CapabilityOperation.DISABLE);
    disabledCapabilityGrant.getCapability().setStatus(CapabilityStatus.DISABLED);

    Role activeRole =
        Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .roleCapabilityGrantAssignments(
                Set.of(
                    RoleCapabilityGrantAssignment.builder()
                        .id(UUID.randomUUID())
                        .roleCapabilityGrant(activeGrant)
                        .build(),
                    RoleCapabilityGrantAssignment.builder()
                        .id(UUID.randomUUID())
                        .roleCapabilityGrant(disabledCapabilityGrant)
                        .build()))
            .build();
    Role revokedRole = Role.builder().id(UUID.randomUUID()).name("REVOKED").build();
    Role expiredRole = Role.builder().id(UUID.randomUUID()).name("EXPIRED").build();
    Account account =
        Account.builder()
            .id(UUID.randomUUID())
            .email("profile@itip.local")
            .fullName("Profile Account")
            .accountRoleAssignments(
                Set.of(
                    AccountRoleAssignment.builder().id(UUID.randomUUID()).role(activeRole).build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(revokedRole)
                        .unassignedAt(Instant.now())
                        .build(),
                    AccountRoleAssignment.builder()
                        .id(UUID.randomUUID())
                        .role(expiredRole)
                        .expiresAt(Instant.now().minusSeconds(1))
                        .build()))
            .build();
    when(accountRepository.findByEmailWithRolesAndCapabilities("profile@itip.local"))
        .thenReturn(java.util.Optional.of(account));

    var profile = accountService.describeAccount("profile@itip.local");

    assertThat(profile.id()).isEqualTo(account.getId());
    assertThat(profile.email()).isEqualTo("profile@itip.local");
    assertThat(profile.fullName()).isEqualTo("Profile Account");
    assertThat(profile.roles()).containsExactly("ADMIN");
    assertThat(profile.capabilities()).containsExactly("ITIP:ACCOUNT:READ");
  }

  private static RoleCapabilityGrant allowGrant(String resource, CapabilityOperation operation) {
    Capability capability =
        Capability.builder()
            .id(UUID.randomUUID())
            .resourceOrigin(CapabilityResourceOrigin.ITIP)
            .resource(resource)
            .operation(operation)
            .status(CapabilityStatus.ACTIVE)
            .build();
    return RoleCapabilityGrant.builder().id(UUID.randomUUID()).capability(capability).build();
  }
}
