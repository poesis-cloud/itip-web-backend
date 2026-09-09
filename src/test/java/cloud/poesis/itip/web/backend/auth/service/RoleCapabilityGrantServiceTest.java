package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantAuditActionType;
import cloud.poesis.itip.web.backend.auth.repository.RoleCapabilityGrantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleCapabilityGrantServiceTest {

  @Mock private RoleCapabilityGrantRepository roleCapabilityGrantRepository;

  @Mock private RoleCapabilityGrantChangeAuditService roleCapabilityGrantChangeAuditService;

  @Mock private CapabilityService capabilityService;

  @Mock private PolicyService policyService;

  private RoleCapabilityGrantService roleCapabilityGrantService;

  @BeforeEach
  void setUp() {
    roleCapabilityGrantService =
        new RoleCapabilityGrantService(
            roleCapabilityGrantRepository,
            roleCapabilityGrantChangeAuditService,
            new ObjectMapper(),
            capabilityService,
            policyService);
  }

  @Test
  @SuppressWarnings("null")
  void createShouldPersistGrantWithCapabilityPoliciesAndAuditItsState() {
    UUID capabilityId = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    Capability capability = Capability.builder().id(capabilityId).build();
    Policy policy = Policy.builder().id(policyId).build();
    CreateRoleCapabilityGrantCommand command =
        new CreateRoleCapabilityGrantCommand(capabilityId, Set.of(policyId));
    when(capabilityService.require(capabilityId)).thenReturn(capability);
    when(policyService.requireAll(command.policyIds())).thenReturn(Set.of(policy));
    when(roleCapabilityGrantRepository.save(any(RoleCapabilityGrant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RoleCapabilityGrant result = roleCapabilityGrantService.create(command, "alice");

    assertThat(result.getCapability()).isSameAs(capability);
    assertThat(result.getPolicies()).containsExactly(policy);
    assertThat(result.getCreatedBy()).isEqualTo("alice");
    assertThat(result.getUpdatedBy()).isEqualTo("alice");

    verify(capabilityService).require(capabilityId);
    verify(policyService).requireAll(command.policyIds());

    verify(roleCapabilityGrantChangeAuditService)
        .recordChange(
            eq(result),
            eq(RoleCapabilityGrantAuditActionType.CREATE),
            eq("null"),
            org.mockito.ArgumentMatchers.contains(capabilityId.toString()),
            eq("alice"));
  }

  @Test
  void createShouldRejectBlankActor() {
    assertThatThrownBy(() -> roleCapabilityGrantService.create(validCommand(), " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("actor is required");

    verifyNoInteractions(roleCapabilityGrantRepository, roleCapabilityGrantChangeAuditService);
  }

  @Test
  void createShouldRejectNullCommand() {
    assertThatThrownBy(() -> roleCapabilityGrantService.create(null, "alice"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command is required");

    verifyNoInteractions(roleCapabilityGrantRepository, roleCapabilityGrantChangeAuditService);
  }

  @Test
  void createShouldRejectMissingCapabilityId() {
    CreateRoleCapabilityGrantCommand command = new CreateRoleCapabilityGrantCommand(null, Set.of());

    assertThatThrownBy(() -> roleCapabilityGrantService.create(command, "alice"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("capabilityId is required");

    verifyNoInteractions(roleCapabilityGrantRepository, roleCapabilityGrantChangeAuditService);
  }

  private static CreateRoleCapabilityGrantCommand validCommand() {
    return new CreateRoleCapabilityGrantCommand(UUID.randomUUID(), Set.of());
  }
}
