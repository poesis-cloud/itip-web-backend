package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantAuditActionType;
import cloud.poesis.itip.web.backend.auth.repository.RoleCapabilityGrantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleCapabilityGrantService {

  private final RoleCapabilityGrantRepository roleCapabilityGrantRepository;
  private final RoleCapabilityGrantChangeAuditService roleCapabilityGrantChangeAuditService;
  private final ObjectMapper objectMapper;
  private final CapabilityService capabilityService;
  private final PolicyService policyService;

  @Transactional
  @SuppressWarnings("null")
  public RoleCapabilityGrant create(CreateRoleCapabilityGrantCommand command, String actor) {
    if (command == null) {
      throw new NullPointerException("command is required");
    }
    if (command.capabilityId() == null) {
      throw new NullPointerException("capabilityId is required");
    }
    requireText(actor, "actor");

    Capability capability = capabilityService.require(command.capabilityId());
    Set<Policy> policies = policyService.requireAll(command.policyIds());

    RoleCapabilityGrant roleCapabilityGrant =
        RoleCapabilityGrant.builder()
            .capability(capability)
            .policies(policies)
            .createdBy(actor)
            .updatedBy(actor)
            .build();

    RoleCapabilityGrant saved = roleCapabilityGrantRepository.save(roleCapabilityGrant);
    roleCapabilityGrantChangeAuditService.recordChange(
        saved, RoleCapabilityGrantAuditActionType.CREATE, "null", serializeState(saved), actor);
    return saved;
  }

  @SuppressWarnings("null")
  private String serializeState(RoleCapabilityGrant roleCapabilityGrant) {
    RoleCapabilityGrantState state =
        new RoleCapabilityGrantState(
            roleCapabilityGrant.getCapability().getId(),
            roleCapabilityGrant.getPolicies().stream().map(Policy::getId).toList(),
            roleCapabilityGrant.getVersion(),
            roleCapabilityGrant.getCreatedBy(),
            roleCapabilityGrant.getUpdatedBy());
    try {
      return objectMapper.writeValueAsString(state);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Could not serialize role capability grant audit state", exception);
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
  }

  private record RoleCapabilityGrantState(
      java.util.UUID capabilityId,
      java.util.List<java.util.UUID> policyIds,
      long version,
      String createdBy,
      String updatedBy) {}
}
