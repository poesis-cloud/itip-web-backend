package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeEffect;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceType;
import cloud.poesis.itip.web.backend.auth.repository.PrivilegeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivilegeService {

  private final PrivilegeRepository privilegeRepository;
  private final PrivilegeChangeAuditService privilegeChangeAuditService;
  private final ObjectMapper objectMapper;

  @Transactional
  @SuppressWarnings("null")
  public Privilege create(CreatePrivilegeCommand command, String actor) {
    Objects.requireNonNull(command, "command is required");
    requireText(command.code(), "code");
    Objects.requireNonNull(command.effect(), "effect is required");
    Objects.requireNonNull(command.resourceType(), "resourceType is required");
    requireResourceTypeKey(command.resourceType(), command.resourceTypeKey());
    Objects.requireNonNull(command.action(), "action is required");
    requireText(actor, "actor");

    Privilege privilege =
        Privilege.builder()
            .code(command.code())
            .effect(command.effect())
            .resourceType(command.resourceType())
            .resourceTypeKey(command.resourceTypeKey())
            .action(command.action())
            .conditionExpression(normalizeCondition(command.conditionExpression()))
            .createdBy(actor)
            .updatedBy(actor)
            .build();

    Privilege saved = privilegeRepository.save(privilege);
    privilegeChangeAuditService.recordChange(
        saved, PrivilegeAuditActionType.CREATE, "null", serializeState(saved), actor);
    return saved;
  }

  private String serializeState(Privilege privilege) {
    PrivilegeState state =
        new PrivilegeState(
            privilege.getCode(),
            privilege.getEffect(),
            privilege.getResourceType(),
            privilege.getResourceTypeKey(),
            privilege.getAction(),
            privilege.getConditionExpression(),
            privilege.getVersion(),
            privilege.getCreatedBy(),
            privilege.getUpdatedBy());
    try {
      return objectMapper.writeValueAsString(state);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize privilege audit state", exception);
    }
  }

  private static void requireResourceTypeKey(
      PrivilegeResourceType resourceType, String resourceTypeKey) {
    requireText(resourceTypeKey, "resourceTypeKey");
    String expectedPrefix = resourceType.name().toLowerCase(Locale.ROOT) + ":";
    if (!resourceTypeKey.startsWith(expectedPrefix)
        || resourceTypeKey.length() == expectedPrefix.length()) {
      throw new IllegalArgumentException(
          "resourceTypeKey must start with " + expectedPrefix + " and include a type");
    }
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
  }

  private static String normalizeCondition(String conditionExpression) {
    return conditionExpression == null || conditionExpression.isBlank()
        ? null
        : conditionExpression;
  }

  private record PrivilegeState(
      String code,
      PrivilegeEffect effect,
      PrivilegeResourceType resourceType,
      String resourceTypeKey,
      PrivilegeAction action,
      String conditionExpression,
      long version,
      String createdBy,
      String updatedBy) {}
}
