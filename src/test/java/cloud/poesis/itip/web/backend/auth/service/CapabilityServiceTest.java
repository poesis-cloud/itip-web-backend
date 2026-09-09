package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityOperation;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityStatus;
import cloud.poesis.itip.web.backend.auth.repository.CapabilityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CapabilityServiceTest {

  @Mock private CapabilityRepository capabilityRepository;

  @Mock private CapabilityChangeAuditService capabilityChangeAuditService;

  private CapabilityService capabilityService;

  @BeforeEach
  void setUp() {
    capabilityService =
        new CapabilityService(
            capabilityRepository, capabilityChangeAuditService, new ObjectMapper());
  }

  @Test
  @SuppressWarnings("null")
  void requireShouldReturnCapabilityForResolvedId() {
    UUID capabilityId = UUID.randomUUID();
    Capability capability = Capability.builder().id(capabilityId).build();
    when(capabilityRepository.findById(capabilityId)).thenReturn(Optional.of(capability));

    Capability resolvedCapability = capabilityService.require(capabilityId);

    assertThat(resolvedCapability).isSameAs(capability);
    verify(capabilityRepository).findById(capabilityId);
  }

  @Test
  @SuppressWarnings("null")
  void requireShouldThrowWhenCapabilityIsMissing() {
    UUID capabilityId = UUID.randomUUID();
    when(capabilityRepository.findById(capabilityId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> capabilityService.require(capabilityId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Capability not found: " + capabilityId);
  }

  @Test
  @SuppressWarnings("null")
  void requireShouldThrowWhenNullIdDoesNotResolve() {
    when(capabilityRepository.findById(null)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> capabilityService.require(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Capability not found: null");
  }

  @Test
  @SuppressWarnings("null")
  void changeStatusShouldPersistTransitionAndRecordAudit() {
    Capability capability = activeCapability();
    when(capabilityRepository.findById(capability.getId())).thenReturn(Optional.of(capability));
    when(capabilityRepository.save(capability)).thenReturn(capability);

    Capability result =
        capabilityService.changeStatus(capability.getId(), CapabilityStatus.DISABLED, "alice");

    assertThat(result.getStatus()).isEqualTo(CapabilityStatus.DISABLED);
    assertThat(result.getUpdatedBy()).isEqualTo("alice");
    verify(capabilityRepository).save(capability);
    verify(capabilityChangeAuditService)
        .recordChange(
            eq(capability),
            eq(CapabilityAuditActionType.DISABLE),
            contains("\"status\":\"ACTIVE\""),
            contains("\"status\":\"DISABLED\""),
            eq("alice"));
  }

  @Test
  @SuppressWarnings("null")
  void changeStatusShouldMapEachStatusToItsAuditActionType() {
    Capability capability = activeCapability();
    capability.setStatus(CapabilityStatus.DISABLED);
    when(capabilityRepository.findById(capability.getId())).thenReturn(Optional.of(capability));
    when(capabilityRepository.save(capability)).thenReturn(capability);

    capabilityService.changeStatus(capability.getId(), CapabilityStatus.ACTIVE, "alice");
    capabilityService.changeStatus(capability.getId(), CapabilityStatus.DEPRECATED, "alice");

    verify(capabilityChangeAuditService)
        .recordChange(
            eq(capability), eq(CapabilityAuditActionType.ACTIVATE), any(), any(), eq("alice"));
    verify(capabilityChangeAuditService)
        .recordChange(
            eq(capability), eq(CapabilityAuditActionType.DEPRECATE), any(), any(), eq("alice"));
  }

  @Test
  @SuppressWarnings("null")
  void changeStatusShouldBeIdempotentForUnchangedStatus() {
    Capability capability = activeCapability();
    when(capabilityRepository.findById(capability.getId())).thenReturn(Optional.of(capability));

    Capability result =
        capabilityService.changeStatus(capability.getId(), CapabilityStatus.ACTIVE, "alice");

    assertThat(result).isSameAs(capability);
    verify(capabilityRepository, never()).save(any());
    verify(capabilityChangeAuditService, never()).recordChange(any(), any(), any(), any(), any());
  }

  @Test
  void changeStatusShouldRejectNullStatus() {
    assertThatThrownBy(() -> capabilityService.changeStatus(UUID.randomUUID(), null, "alice"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("newStatus is required");
  }

  @Test
  void changeStatusShouldRejectBlankActor() {
    assertThatThrownBy(
            () -> capabilityService.changeStatus(UUID.randomUUID(), CapabilityStatus.DISABLED, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("actor is required");
  }

  private static Capability activeCapability() {
    return Capability.builder()
        .id(UUID.randomUUID())
        .resourceOrigin(CapabilityResourceOrigin.ITIP)
        .resource("ACCOUNT")
        .operation(CapabilityOperation.READ)
        .status(CapabilityStatus.ACTIVE)
        .createdBy("seed")
        .updatedBy("seed")
        .build();
  }
}
