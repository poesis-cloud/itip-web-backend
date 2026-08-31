package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.repository.AccountRoleAssignmentRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountRoleAssignmentServiceTest {

  @Mock private AccountRoleAssignmentRepository accountRoleAssignmentRepository;

  @InjectMocks private AccountRoleAssignmentService accountRoleAssignmentService;

  @Test
  @SuppressWarnings("null")
  void assignShouldSaveAssignmentForAccountAndRole() {
    Account account = Account.builder().id(UUID.randomUUID()).build();
    Role role = Role.builder().id(UUID.randomUUID()).name("ITIP_USER").build();
    AccountRoleAssignment savedAssignment =
        AccountRoleAssignment.builder().id(UUID.randomUUID()).build();
    when(accountRoleAssignmentRepository.save(any(AccountRoleAssignment.class)))
        .thenReturn(savedAssignment);

    AccountRoleAssignment assignment = accountRoleAssignmentService.assign(account, role);

    assertThat(assignment).isSameAs(savedAssignment);
    ArgumentCaptor<AccountRoleAssignment> assignmentCaptor =
        ArgumentCaptor.forClass(AccountRoleAssignment.class);
    verify(accountRoleAssignmentRepository).save(assignmentCaptor.capture());
    assertThat(assignmentCaptor.getValue().getAccount()).isSameAs(account);
    assertThat(assignmentCaptor.getValue().getRole()).isSameAs(role);
  }
}
