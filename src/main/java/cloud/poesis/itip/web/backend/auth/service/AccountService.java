package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.repository.AccountRepository;
import java.time.Instant;
import java.util.HashSet;
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

@Service
@RequiredArgsConstructor
public class AccountService implements UserDetailsService {

  private final AccountRepository accountRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Account account =
        accountRepository
            .findByEmailWithRolesAndPrivileges(email)
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
            .flatMap(role -> snapshot(role.getRolePrivilegeAssignments()).stream())
            .filter(rolePrivilegeAssignment -> rolePrivilegeAssignment.getUnassignedAt() == null)
            .map(rolePrivilegeAssignment -> rolePrivilegeAssignment.getPrivilege())
            .filter(Objects::nonNull)
            .map(privilege -> privilege.getCode())
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

  private static <T> Set<T> snapshot(Set<T> source) {
    return source == null ? Set.of() : new HashSet<>(source);
  }
}
