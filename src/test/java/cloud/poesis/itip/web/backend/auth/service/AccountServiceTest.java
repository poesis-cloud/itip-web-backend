package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.entity.RolePrivilegeAssignment;
import cloud.poesis.itip.web.backend.auth.repository.AccountRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private AccountRepository accountRepository;

  @InjectMocks private AccountService accountService;

  @Test
  void loadUserByUsernameShouldReturnUserDetailsWithOnlyActiveAuthorities() {
    Privilege activePrivilege = Privilege.builder().id(UUID.randomUUID()).code("READ_USER").build();
    RolePrivilegeAssignment activeRolePrivilegeAssignment =
        RolePrivilegeAssignment.builder()
            .id(UUID.randomUUID())
            .privilege(activePrivilege)
            .build();

    Privilege revokedPrivilege =
        Privilege.builder().id(UUID.randomUUID()).code("DELETE_USER").build();
    RolePrivilegeAssignment revokedRolePrivilegeAssignment =
        RolePrivilegeAssignment.builder()
            .id(UUID.randomUUID())
            .privilege(revokedPrivilege)
            .revokedAt(Instant.now())
            .build();

    Role activeRole =
        Role.builder()
            .id(UUID.randomUUID())
            .name("ADMIN")
            .rolePrivilegeAssignments(
                Set.of(activeRolePrivilegeAssignment, revokedRolePrivilegeAssignment))
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
        .containsExactlyInAnyOrder("READ_USER");
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
}
