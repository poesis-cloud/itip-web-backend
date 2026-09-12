package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.repository.PolicyRepository;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyService {

  private final PolicyRepository policyRepository;

  // Spring Data's generic nullness contract is not inferred by the Eclipse compiler.
  @SuppressWarnings("null")
  public Set<Policy> requireAll(Set<UUID> ids) {
    Set<UUID> requestedIds = ids == null ? Set.of() : new LinkedHashSet<>(ids);
    Set<Policy> policies = new LinkedHashSet<>(policyRepository.findAllById(requestedIds));
    if (policies.size() != requestedIds.size()) {
      throw new IllegalArgumentException("One or more policies do not exist");
    }
    if (policies.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Policies must not be null");
    }
    return policies;
  }
}
