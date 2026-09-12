package cloud.poesis.itip.web.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrantChangeAudit;
import cloud.poesis.itip.web.backend.auth.repository.RoleCapabilityGrantChangeAuditRepository;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleCapabilityGrantChangeAuditServiceTest {

  @Mock private RoleCapabilityGrantChangeAuditRepository roleCapabilityGrantChangeAuditRepository;

  @InjectMocks private RoleCapabilityGrantChangeAuditService roleCapabilityGrantChangeAuditService;

  private RoleCapabilityGrant roleCapabilityGrant;

  @BeforeEach
  void setUp() {
    roleCapabilityGrant = RoleCapabilityGrant.builder().version(3L).build();
  }

  @Test
  @SuppressWarnings("null")
  void recordChangeShouldPersistRoleCapabilityGrantAuditEntry() {
    RoleCapabilityGrantChangeAudit persisted =
        RoleCapabilityGrantChangeAudit.builder().actionBy("alice").build();
    when(roleCapabilityGrantChangeAuditRepository.save(argThat(Objects::nonNull)))
        .thenReturn(persisted);

    RoleCapabilityGrantChangeAudit result =
        roleCapabilityGrantChangeAuditService.recordChange(
            roleCapabilityGrant,
            RoleCapabilityGrantAuditActionType.UPDATE,
            "{\"effect\":\"ALLOW\"}",
            "{\"effect\":\"DENY\"}",
            "alice");

    assertSame(persisted, result);
    verify(roleCapabilityGrantChangeAuditRepository).save(argThat(Objects::nonNull));
  }

  @Test
  @SuppressWarnings("null")
  void recordChangeShouldCopyRoleCapabilityGrantVersion() {
    when(roleCapabilityGrantChangeAuditRepository.save(argThat(Objects::nonNull)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RoleCapabilityGrantChangeAudit result =
        roleCapabilityGrantChangeAuditService.recordChange(
            roleCapabilityGrant,
            RoleCapabilityGrantAuditActionType.UPDATE,
            "{\"x\":1}",
            "{\"x\":2}",
            "bob");

    assertEquals(3L, result.getRoleCapabilityGrantVersion());
  }
}
