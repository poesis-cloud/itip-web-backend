package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.repository.CapabilityRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CapabilityServiceTest {

  @Mock private CapabilityRepository capabilityRepository;

  @InjectMocks private CapabilityService capabilityService;

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
}
