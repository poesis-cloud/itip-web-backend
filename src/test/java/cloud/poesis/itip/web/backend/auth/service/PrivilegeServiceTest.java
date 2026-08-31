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
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAuditActionType;
import cloud.poesis.itip.web.backend.auth.repository.PrivilegeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivilegeServiceTest {

  @Mock private PrivilegeRepository privilegeRepository;

  @Mock private PrivilegeChangeAuditService privilegeChangeAuditService;

  @Mock private CapabilityService capabilityService;

  @Mock private PolicyService policyService;

  private PrivilegeService privilegeService;

  @BeforeEach
  void setUp() {
    privilegeService =
        new PrivilegeService(
            privilegeRepository,
            privilegeChangeAuditService,
            new ObjectMapper(),
            capabilityService,
            policyService);
  }

  @Test
  @SuppressWarnings("null")
  void createShouldPersistPrivilegeWithCapabilityPoliciesAndAuditItsState() {
    UUID capabilityId = UUID.randomUUID();
    UUID policyId = UUID.randomUUID();
    Capability capability = Capability.builder().id(capabilityId).build();
    Policy policy = Policy.builder().id(policyId).build();
    CreatePrivilegeCommand command = new CreatePrivilegeCommand(capabilityId, Set.of(policyId));
    when(capabilityService.require(capabilityId)).thenReturn(capability);
    when(policyService.requireAll(command.policyIds())).thenReturn(Set.of(policy));
    when(privilegeRepository.save(any(Privilege.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Privilege result = privilegeService.create(command, "alice");

    assertThat(result.getCapability()).isSameAs(capability);
    assertThat(result.getPolicies()).containsExactly(policy);
    assertThat(result.getCreatedBy()).isEqualTo("alice");
    assertThat(result.getUpdatedBy()).isEqualTo("alice");

    verify(capabilityService).require(capabilityId);
    verify(policyService).requireAll(command.policyIds());

    verify(privilegeChangeAuditService)
        .recordChange(
            eq(result),
            eq(PrivilegeAuditActionType.CREATE),
            eq("null"),
            org.mockito.ArgumentMatchers.contains(capabilityId.toString()),
            eq("alice"));
  }

  @Test
  void createShouldRejectBlankActor() {
    assertThatThrownBy(() -> privilegeService.create(validCommand(), " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("actor is required");

    verifyNoInteractions(privilegeRepository, privilegeChangeAuditService);
  }

  @Test
  void createShouldRejectNullCommand() {
    assertThatThrownBy(() -> privilegeService.create(null, "alice"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("command is required");

    verifyNoInteractions(privilegeRepository, privilegeChangeAuditService);
  }

  @Test
  void createShouldRejectMissingCapabilityId() {
    CreatePrivilegeCommand command = new CreatePrivilegeCommand(null, Set.of());

    assertThatThrownBy(() -> privilegeService.create(command, "alice"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("capabilityId is required");

    verifyNoInteractions(privilegeRepository, privilegeChangeAuditService);
  }

  private static CreatePrivilegeCommand validCommand() {
    return new CreatePrivilegeCommand(UUID.randomUUID(), Set.of());
  }
}
