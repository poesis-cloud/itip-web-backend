package cloud.poesis.itip.web.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityChangeAudit;
import cloud.poesis.itip.web.backend.auth.repository.CapabilityChangeAuditRepository;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CapabilityChangeAuditServiceTest {

  @Mock private CapabilityChangeAuditRepository capabilityChangeAuditRepository;

  @InjectMocks private CapabilityChangeAuditService capabilityChangeAuditService;

  private Capability capability;

  @BeforeEach
  void setUp() {
    capability = Capability.builder().version(5L).build();
  }

  @Test
  @SuppressWarnings("null")
  void recordChangeShouldPersistCapabilityAuditEntry() {
    CapabilityChangeAudit persisted = CapabilityChangeAudit.builder().actionBy("alice").build();
    when(capabilityChangeAuditRepository.save(argThat(Objects::nonNull))).thenReturn(persisted);

    CapabilityChangeAudit result =
        capabilityChangeAuditService.recordChange(
            capability,
            CapabilityAuditActionType.DISABLE,
            "{\"status\":\"ACTIVE\"}",
            "{\"status\":\"DISABLED\"}",
            "alice");

    assertSame(persisted, result);
    verify(capabilityChangeAuditRepository).save(argThat(Objects::nonNull));
  }

  @Test
  @SuppressWarnings("null")
  void recordChangeShouldCopyCapabilityVersion() {
    when(capabilityChangeAuditRepository.save(argThat(Objects::nonNull)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CapabilityChangeAudit result =
        capabilityChangeAuditService.recordChange(
            capability, CapabilityAuditActionType.UPDATE, "{\"x\":1}", "{\"x\":2}", "bob");

    assertEquals(5L, result.getCapabilityVersion());
  }
}
