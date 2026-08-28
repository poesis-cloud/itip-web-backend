package cloud.poesis.itip.web.backend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAuditActionType;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeChangeAudit;
import cloud.poesis.itip.web.backend.auth.repository.PrivilegeChangeAuditRepository;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivilegeChangeAuditServiceTest {

  @Mock private PrivilegeChangeAuditRepository privilegeChangeAuditRepository;

  @InjectMocks private PrivilegeChangeAuditService privilegeChangeAuditService;

  private Privilege privilege;

  @BeforeEach
  void setUp() {
    privilege = Privilege.builder().code("approve-proposed").version(3L).build();
  }

  @Test
  @SuppressWarnings("null")
  void recordChangeShouldPersistPrivilegeAuditEntry() {
    PrivilegeChangeAudit persisted = PrivilegeChangeAudit.builder().actionBy("alice").build();
    when(privilegeChangeAuditRepository.save(argThat(Objects::nonNull))).thenReturn(persisted);

    PrivilegeChangeAudit result =
        privilegeChangeAuditService.recordChange(
            privilege,
            PrivilegeAuditActionType.UPDATE,
            "{\"effect\":\"ALLOW\"}",
            "{\"effect\":\"DENY\"}",
            "alice");

    assertSame(persisted, result);
    verify(privilegeChangeAuditRepository).save(argThat(Objects::nonNull));
  }

  @Test
  @SuppressWarnings("null")
  void recordChangeShouldCopyPrivilegeVersion() {
    when(privilegeChangeAuditRepository.save(argThat(Objects::nonNull)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PrivilegeChangeAudit result =
        privilegeChangeAuditService.recordChange(
            privilege, PrivilegeAuditActionType.UPDATE, "{\"x\":1}", "{\"x\":2}", "bob");

    assertEquals(3L, result.getPrivilegeVersion());
  }
}
