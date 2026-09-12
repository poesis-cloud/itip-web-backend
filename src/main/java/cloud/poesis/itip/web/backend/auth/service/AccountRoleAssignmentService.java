package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.AccountRoleAssignment;
import cloud.poesis.itip.web.backend.auth.entity.Role;
import cloud.poesis.itip.web.backend.auth.repository.AccountRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountRoleAssignmentService {

  private final AccountRoleAssignmentRepository accountRoleAssignmentRepository;

  @SuppressWarnings("null")
  public AccountRoleAssignment assign(Account account, Role role) {
    return accountRoleAssignmentRepository.save(
        AccountRoleAssignment.builder().account(account).role(role).build());
  }
}
