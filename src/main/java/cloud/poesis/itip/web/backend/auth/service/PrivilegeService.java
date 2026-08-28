package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeEffect;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import cloud.poesis.itip.web.backend.auth.repository.PrivilegeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ConditionExpressionEvaluator conditionExpressionEvaluator;

  @Transactional
  @SuppressWarnings("null")
  public Privilege create(CreatePrivilegeCommand command, String actor) {
    Objects.requireNonNull(command, "command is required");
    Objects.requireNonNull(command.effect(), "effect is required");
    Objects.requireNonNull(command.resourceOrigin(), "resourceOrigin is required");
    requireResource(command.resource());
    Objects.requireNonNull(command.action(), "action is required");
    requireText(actor, "actor");

    String conditionExpression = normalizeCondition(command.conditionExpression());
    if (!conditionExpressionEvaluator.isValid(conditionExpression)) {
      throw new IllegalArgumentException(
          "conditionExpression must be a valid CEL boolean expression");
    }

    Privilege privilege =
        Privilege.builder()
            .effect(command.effect())
            .resourceOrigin(command.resourceOrigin())
            .resource(command.resource())
            .action(command.action())
            .conditionExpression(conditionExpression)
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
            privilege.getEffect(),
            privilege.getResourceOrigin(),
            privilege.getResource(),
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

  // The origin is carried by resourceOrigin, so the resource name must stay unprefixed.
  private static void requireResource(String resource) {
    requireText(resource, "resource");
    if (resource.indexOf(':') >= 0) {
      throw new IllegalArgumentException("resource must not carry an origin prefix");
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
      PrivilegeEffect effect,
      PrivilegeResourceOrigin resourceOrigin,
      String resource,
      PrivilegeAction action,
      String conditionExpression,
      long version,
      String createdBy,
      String updatedBy) {}
}
