package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAuditActionType;
import cloud.poesis.itip.web.backend.auth.repository.PrivilegeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivilegeService {

  private final PrivilegeRepository privilegeRepository;
  private final PrivilegeChangeAuditService privilegeChangeAuditService;
  private final ObjectMapper objectMapper;
  private final CapabilityService capabilityService;
  private final PolicyService policyService;

  @Transactional
  @SuppressWarnings("null")
  public Privilege create(CreatePrivilegeCommand command, String actor) {
    if (command == null) {
      throw new NullPointerException("command is required");
    }
    if (command.capabilityId() == null) {
      throw new NullPointerException("capabilityId is required");
    }
    requireText(actor, "actor");

    Capability capability = capabilityService.require(command.capabilityId());
    Set<Policy> policies = policyService.requireAll(command.policyIds());

    Privilege privilege =
        Privilege.builder()
            .capability(capability)
            .policies(policies)
            .createdBy(actor)
            .updatedBy(actor)
            .build();

    Privilege saved = privilegeRepository.save(privilege);
    privilegeChangeAuditService.recordChange(
        saved, PrivilegeAuditActionType.CREATE, "null", serializeState(saved), actor);
    return saved;
  }

  @SuppressWarnings("null")
  private String serializeState(Privilege privilege) {
    PrivilegeState state =
        new PrivilegeState(
            privilege.getCapability().getId(),
            privilege.getPolicies().stream().map(Policy::getId).toList(),
            privilege.getVersion(),
            privilege.getCreatedBy(),
            privilege.getUpdatedBy());
    try {
      return objectMapper.writeValueAsString(state);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize privilege audit state", exception);
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
  }

  private record PrivilegeState(
      java.util.UUID capabilityId,
      java.util.List<java.util.UUID> policyIds,
      long version,
      String createdBy,
      String updatedBy) {}
}
