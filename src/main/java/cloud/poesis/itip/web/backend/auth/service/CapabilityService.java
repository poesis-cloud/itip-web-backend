package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityStatus;
import cloud.poesis.itip.web.backend.auth.repository.CapabilityRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CapabilityService {

  private final CapabilityRepository capabilityRepository;
  private final CapabilityChangeAuditService capabilityChangeAuditService;
  private final ObjectMapper objectMapper;

  // Spring Data's generic nullness contract is not inferred by the Eclipse compiler.
  @SuppressWarnings("null")
  public Capability require(UUID id) {
    return capabilityRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Capability not found: " + id));
  }

  @Transactional
  @SuppressWarnings("null")
  public Capability changeStatus(UUID id, CapabilityStatus newStatus, String actor) {
    if (newStatus == null) {
      throw new NullPointerException("newStatus is required");
    }
    requireText(actor, "actor");

    Capability capability = require(id);
    if (capability.getStatus() == newStatus) {
      return capability;
    }

    String previousState = serializeState(capability);
    capability.setStatus(newStatus);
    capability.setUpdatedBy(actor);

    Capability saved = capabilityRepository.save(capability);
    capabilityChangeAuditService.recordChange(
        saved, actionTypeFor(newStatus), previousState, serializeState(saved), actor);
    return saved;
  }

  private static CapabilityAuditActionType actionTypeFor(CapabilityStatus status) {
    return switch (status) {
      case ACTIVE -> CapabilityAuditActionType.ACTIVATE;
      case DISABLED -> CapabilityAuditActionType.DISABLE;
      case DEPRECATED -> CapabilityAuditActionType.DEPRECATE;
    };
  }

  @SuppressWarnings("null")
  private String serializeState(Capability capability) {
    CapabilityState state =
        new CapabilityState(
            capability.getResourceOrigin() == null ? null : capability.getResourceOrigin().name(),
            capability.getResource(),
            capability.getOperation() == null ? null : capability.getOperation().name(),
            capability.getStatus() == null ? null : capability.getStatus().name(),
            capability.getVersion(),
            capability.getCreatedBy(),
            capability.getUpdatedBy());
    try {
      return objectMapper.writeValueAsString(state);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize capability audit state", exception);
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
  }

  private record CapabilityState(
      String resourceOrigin,
      String resource,
      String operation,
      String status,
      long version,
      String createdBy,
      String updatedBy) {}
}
