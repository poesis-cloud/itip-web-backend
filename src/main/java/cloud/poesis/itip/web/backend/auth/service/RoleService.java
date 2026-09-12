package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {

  public static final String DEFAULT_ROLE_NAME = "ITIP_USER";

  private final RoleRepository roleRepository;

  public Role requireDefaultRole() {
    return roleRepository
        .findByName(DEFAULT_ROLE_NAME)
        .orElseThrow(() -> new IllegalStateException("Default role ITIP_USER is not configured"));
  }
}
