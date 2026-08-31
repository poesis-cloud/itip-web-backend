package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.repository.RoleRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private RoleRepository roleRepository;

  @InjectMocks private RoleService roleService;

  @Test
  void requireDefaultRoleShouldReturnConfiguredDefaultRole() {
    Role defaultRole =
        Role.builder().id(UUID.randomUUID()).name(RoleService.DEFAULT_ROLE_NAME).build();
    when(roleRepository.findByName(RoleService.DEFAULT_ROLE_NAME))
        .thenReturn(Optional.of(defaultRole));

    Role resolvedRole = roleService.requireDefaultRole();

    assertThat(resolvedRole).isSameAs(defaultRole);
    verify(roleRepository).findByName(RoleService.DEFAULT_ROLE_NAME);
  }

  @Test
  void requireDefaultRoleShouldThrowWhenDefaultRoleIsNotConfigured() {
    when(roleRepository.findByName(RoleService.DEFAULT_ROLE_NAME)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleService.requireDefaultRole())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Default role ITIP_USER is not configured");
  }
}
