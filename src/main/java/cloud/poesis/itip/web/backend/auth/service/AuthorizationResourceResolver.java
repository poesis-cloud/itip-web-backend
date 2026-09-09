package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationResourceResolver {

  boolean supports(CapabilityResourceOrigin origin, String resource);

  Optional<Map<String, Object>> resolve(
      CapabilityResourceOrigin origin, String resource, UUID resourceId);
}
