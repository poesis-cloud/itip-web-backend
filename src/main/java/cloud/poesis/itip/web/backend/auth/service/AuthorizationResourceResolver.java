package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationResourceResolver {

  boolean supports(PrivilegeResourceOrigin origin, String resource);

  Optional<Map<String, Object>> resolve(
      PrivilegeResourceOrigin origin, String resource, UUID resourceId);
}
