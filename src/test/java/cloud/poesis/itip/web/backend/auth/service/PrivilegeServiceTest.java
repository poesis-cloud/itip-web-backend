package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeEffect;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import cloud.poesis.itip.web.backend.auth.repository.PrivilegeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivilegeServiceTest {

  @Mock private PrivilegeRepository privilegeRepository;

  @Mock private PrivilegeChangeAuditService privilegeChangeAuditService;

  @Mock private ConditionExpressionEvaluator conditionExpressionEvaluator;

  private PrivilegeService privilegeService;

  @BeforeEach
  void setUp() {
    privilegeService =
        new PrivilegeService(
            privilegeRepository,
            privilegeChangeAuditService,
            new ObjectMapper(),
            conditionExpressionEvaluator);
    lenient().when(conditionExpressionEvaluator.isValid(any())).thenReturn(true);
  }

  @Test
  @SuppressWarnings("null")
  void createShouldPersistExplicitPrivilegeAndAuditItsState() {
    CreatePrivilegeCommand command = validCommand("target.ownerId != actor.id");
    when(privilegeRepository.save(any(Privilege.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Privilege result = privilegeService.create(command, "alice");

    assertThat(result.getEffect()).isEqualTo(PrivilegeEffect.ALLOW);
    assertThat(result.getResourceOrigin()).isEqualTo(PrivilegeResourceOrigin.DEFMAN);
    assertThat(result.getResource()).isEqualTo("ASCRIPTION");
    assertThat(result.getAction()).isEqualTo(PrivilegeAction.APPROVE);
    assertThat(result.getConditionExpression()).isEqualTo("target.ownerId != actor.id");
    assertThat(result.getCreatedBy()).isEqualTo("alice");
    assertThat(result.getUpdatedBy()).isEqualTo("alice");

    verify(privilegeChangeAuditService)
        .recordChange(
            eq(result),
            eq(PrivilegeAuditActionType.CREATE),
            eq("null"),
            org.mockito.ArgumentMatchers.contains("\"resource\":\"ASCRIPTION\""),
            eq("alice"));
  }

  @Test
  @SuppressWarnings("null")
  void createShouldNormalizeBlankConditionToNull() {
    when(privilegeRepository.save(any(Privilege.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Privilege result = privilegeService.create(validCommand("  "), "alice");

    assertThat(result.getConditionExpression()).isNull();
  }

  @Test
  void createShouldRejectInvalidCelCondition() {
    when(conditionExpressionEvaluator.isValid("target.ownerId ==")).thenReturn(false);

    assertThatThrownBy(() -> privilegeService.create(validCommand("target.ownerId =="), "alice"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("valid CEL boolean expression");

    verifyNoInteractions(privilegeRepository, privilegeChangeAuditService);
  }

  @Test
  void createShouldRejectResourceCarryingOriginPrefix() {
    CreatePrivilegeCommand command =
        new CreatePrivilegeCommand(
            PrivilegeEffect.ALLOW,
            PrivilegeResourceOrigin.DEFMAN,
            "defman:ASCRIPTION",
            PrivilegeAction.APPROVE,
            null);

    assertThatThrownBy(() -> privilegeService.create(command, "alice"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not carry an origin prefix");

    verifyNoInteractions(privilegeRepository, privilegeChangeAuditService);
  }

  @Test
  void createShouldRejectBlankResource() {
    CreatePrivilegeCommand command =
        new CreatePrivilegeCommand(
            PrivilegeEffect.ALLOW,
            PrivilegeResourceOrigin.DEFMAN,
            "  ",
            PrivilegeAction.APPROVE,
            null);

    assertThatThrownBy(() -> privilegeService.create(command, "alice"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("resource is required");

    verify(privilegeRepository, never()).save(any());
  }

  @Test
  void createShouldRejectBlankActor() {
    assertThatThrownBy(() -> privilegeService.create(validCommand(null), " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("actor is required");

    verifyNoInteractions(privilegeRepository, privilegeChangeAuditService);
  }

  @Test
  void createShouldRejectMissingEffect() {
    CreatePrivilegeCommand command =
        new CreatePrivilegeCommand(
            null, PrivilegeResourceOrigin.DEFMAN, "ASCRIPTION", PrivilegeAction.APPROVE, null);

    assertThatThrownBy(() -> privilegeService.create(command, "alice"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("effect is required");
  }

  private static CreatePrivilegeCommand validCommand(String conditionExpression) {
    return new CreatePrivilegeCommand(
        PrivilegeEffect.ALLOW,
        PrivilegeResourceOrigin.DEFMAN,
        "ASCRIPTION",
        PrivilegeAction.APPROVE,
        conditionExpression);
  }
}
