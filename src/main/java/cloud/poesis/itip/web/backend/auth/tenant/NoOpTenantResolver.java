package cloud.poesis.itip.web.backend.auth.tenant;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class NoOpTenantResolver implements TenantResolver {

  @Override
  public Optional<UUID> resolve(HttpServletRequest request) {
    return Optional.empty();
  }
}
