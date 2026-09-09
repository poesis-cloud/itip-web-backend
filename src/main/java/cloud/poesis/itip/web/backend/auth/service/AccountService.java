package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityStatus;
import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.model.AccountProfile;
import cloud.poesis.itip.web.backend.auth.repository.AccountRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService implements UserDetailsService {

  private final AccountRepository accountRepository;
  private final RoleService roleService;
  private final AccountRoleAssignmentService accountRoleAssignmentService;

  @Transactional
  @SuppressWarnings("null")
  public Account create(Account account) {
    Account saved = accountRepository.save(account);
    accountRoleAssignmentService.assign(saved, roleService.requireDefaultRole());
    return saved;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Account account =
        accountRepository
            .findByEmailWithRolesAndCapabilities(email)
            .orElseThrow(() -> new UsernameNotFoundException("Account not found: " + email));

    Instant now = Instant.now();
    Set<AccountRoleAssignment> roleAssignments = snapshot(account.getAccountRoleAssignments());
    Set<GrantedAuthority> authorities =
        roleAssignments.stream()
            .filter(
                assignment ->
                    assignment.getUnassignedAt() == null
                        && (assignment.getExpiresAt() == null
                            || assignment.getExpiresAt().isAfter(now)))
            .map(assignment -> assignment.getRole())
            .filter(Objects::nonNull)
            .flatMap(role -> snapshot(role.getRoleCapabilityGrantAssignments()).stream())
            .filter(assignment -> assignment.getUnassignedAt() == null)
            .map(assignment -> assignment.getRoleCapabilityGrant())
            .filter(Objects::nonNull)
            .map(AccountService::authorityOf)
            .filter(Objects::nonNull)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());

    return User.withUsername(account.getEmail())
        .password(account.getPasswordHash())
        .disabled(!account.isEnabled())
        .authorities(authorities)
        .build();
  }

  public Account loadAccountByEmail(String email) {
    return accountRepository
        .findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Account not found: " + email));
  }

  public Account loadAccountWithRolesAndCapabilities(String email) {
    return accountRepository
        .findByEmailWithRolesAndCapabilities(email)
        .orElseThrow(() -> new UsernameNotFoundException("Account not found: " + email));
  }

  public List<RoleCapabilityGrant> activeRoleCapabilityGrants(Account account) {
    Instant now = Instant.now();
    return snapshot(account.getAccountRoleAssignments()).stream()
        .filter(
            assignment ->
                assignment.getUnassignedAt() == null
                    && (assignment.getExpiresAt() == null
                        || assignment.getExpiresAt().isAfter(now)))
        .map(assignment -> assignment.getRole())
        .filter(Objects::nonNull)
        .flatMap(role -> snapshot(role.getRoleCapabilityGrantAssignments()).stream())
        .filter(assignment -> assignment.getUnassignedAt() == null)
        .map(assignment -> assignment.getRoleCapabilityGrant())
        .filter(Objects::nonNull)
        .toList();
  }

  public AccountProfile describeAccount(String email) {
    Account account = loadAccountWithRolesAndCapabilities(email);
    List<String> roles =
        activeRoles(account).stream()
            .map(role -> role.getName())
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
    List<String> capabilities =
        activeRoleCapabilityGrants(account).stream()
            .map(AccountService::authorityOf)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
    return new AccountProfile(
        account.getId(), account.getEmail(), account.getFullName(), roles, capabilities);
  }

  private static List<Role> activeRoles(Account account) {
    Instant now = Instant.now();
    return snapshot(account.getAccountRoleAssignments()).stream()
        .filter(
            assignment ->
                assignment.getUnassignedAt() == null
                    && (assignment.getExpiresAt() == null
                        || assignment.getExpiresAt().isAfter(now)))
        .map(assignment -> assignment.getRole())
        .filter(Objects::nonNull)
        .toList();
  }

  private static <T> Set<T> snapshot(Set<T> source) {
    return source == null ? Set.of() : new HashSet<>(source);
  }

  private static String authorityOf(RoleCapabilityGrant roleCapabilityGrant) {
    Capability capability = roleCapabilityGrant.getCapability();
    if (capability == null
        || capability.getStatus() != CapabilityStatus.ACTIVE
        || capability.getResourceOrigin() == null
        || capability.getResource() == null
        || capability.getOperation() == null) {
      return null;
    }
    return capability.getResourceOrigin()
        + ":"
        + capability.getResource()
        + ":"
        + capability.getOperation();
  }
}
