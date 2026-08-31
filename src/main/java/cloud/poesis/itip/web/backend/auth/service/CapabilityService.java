package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.repository.CapabilityRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CapabilityService {

  private final CapabilityRepository capabilityRepository;

  // Spring Data's generic nullness contract is not inferred by the Eclipse compiler.
  @SuppressWarnings("null")
  public Capability require(UUID id) {
    return capabilityRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Capability not found: " + id));
  }
}
