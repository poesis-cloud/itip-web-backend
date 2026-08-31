package cloud.poesis.itip.web.backend.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UuidV7EntityLifecycleTest {

  @Test
  void shouldAssignUuidV7ToIdRootsWithoutAnId() {
    lifecycleSubjects()
        .forEach(
            subject -> {
              subject.assignUuidV7IfMissing().run();

              UUID id = subject.id().get();
              assertThat(id).isNotNull();
              assertThat(id.version()).isEqualTo(7);
              assertThat(id.variant()).isEqualTo(2);
            });
  }

  @Test
  void shouldPreserveExplicitIds() {
    UUID explicitId = UUID.randomUUID();

    lifecycleSubjects()
        .forEach(
            subject -> {
              subject.setId().accept(explicitId);
              subject.assignUuidV7IfMissing().run();

              assertThat(subject.id().get()).isEqualTo(explicitId);
            });
  }

  private static Stream<LifecycleSubject> lifecycleSubjects() {
    Account account = new Account();
    AccountRoleAssignment accountRoleAssignment = new AccountRoleAssignment();
    Privilege privilege = new Privilege();
    PrivilegeChangeAudit privilegeChangeAudit = new PrivilegeChangeAudit();
    Capability capability = new Capability();
    Policy policy = new Policy();
    Role role = new Role();
    RolePrivilegeAssignment rolePrivilegeAssignment = new RolePrivilegeAssignment();

    return Stream.of(
        subject(account::assignUuidV7IfMissing, account::getId, account::setId),
        subject(
            accountRoleAssignment::assignUuidV7IfMissing,
            accountRoleAssignment::getId,
            accountRoleAssignment::setId),
        subject(privilege::assignUuidV7IfMissing, privilege::getId, privilege::setId),
        subject(
            privilegeChangeAudit::assignUuidV7IfMissing,
            privilegeChangeAudit::getId,
            privilegeChangeAudit::setId),
        subject(capability::assignUuidV7IfMissing, capability::getId, capability::setId),
        subject(policy::assignUuidV7IfMissing, policy::getId, policy::setId),
        subject(role::assignUuidV7IfMissing, role::getId, role::setId),
        subject(
            rolePrivilegeAssignment::assignUuidV7IfMissing,
            rolePrivilegeAssignment::getId,
            rolePrivilegeAssignment::setId));
  }

  private static LifecycleSubject subject(
      Runnable assignUuidV7IfMissing, Supplier<UUID> id, Consumer<UUID> setId) {
    return new LifecycleSubject(assignUuidV7IfMissing, id, setId);
  }

  private record LifecycleSubject(
      Runnable assignUuidV7IfMissing, Supplier<UUID> id, Consumer<UUID> setId) {}
}
