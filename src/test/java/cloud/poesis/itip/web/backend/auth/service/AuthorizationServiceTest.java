package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeEffect;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheck;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

  @Mock private AccountService accountService;

  @Mock private AuthorizationResourceResolver resourceResolver;

  @Mock private ConditionExpressionEvaluator conditionExpressionEvaluator;

  private AuthorizationService authorizationService;
  private Account account;

  @BeforeEach
  void setUp() {
    authorizationService =
        new AuthorizationService(
            accountService, List.of(resourceResolver), conditionExpressionEvaluator);
    account = Account.builder().id(UUID.randomUUID()).email("alice@itip.local").build();
    when(accountService.loadAccountWithRolesAndPrivileges(account.getEmail())).thenReturn(account);
  }

  @Test
  void deniesByDefaultWhenNoPrivilegeMatches() {
    when(accountService.activePrivileges(account)).thenReturn(List.of());

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void allowsAnUnconditionalAllow() {
    when(accountService.activePrivileges(account))
        .thenReturn(List.of(privilege(PrivilegeEffect.ALLOW, null)));

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isTrue();
  }

  @Test
  void denyOverridesAnAllow() {
    when(accountService.activePrivileges(account))
        .thenReturn(
            List.of(privilege(PrivilegeEffect.ALLOW, null), privilege(PrivilegeEffect.DENY, null)));

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void conditionWithoutTargetFailsClosed() {
    when(accountService.activePrivileges(account))
        .thenReturn(List.of(privilege(PrivilegeEffect.ALLOW, "true")));

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void unresolvedTargetFailsClosed() {
    UUID resourceId = UUID.randomUUID();
    when(accountService.activePrivileges(account))
        .thenReturn(List.of(privilege(PrivilegeEffect.ALLOW, "target.ownerId == actor.id")));
    when(resourceResolver.supports(PrivilegeResourceOrigin.ITIP, "PRIVILEGE")).thenReturn(true);
    when(resourceResolver.resolve(PrivilegeResourceOrigin.ITIP, "PRIVILEGE", resourceId))
        .thenReturn(Optional.empty());

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(resourceId))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void evaluatesCelConditionAgainstResolvedTarget() {
    UUID resourceId = UUID.randomUUID();
    when(accountService.activePrivileges(account))
        .thenReturn(List.of(privilege(PrivilegeEffect.ALLOW, "target.ownerId == actor.id")));
    when(resourceResolver.supports(PrivilegeResourceOrigin.ITIP, "PRIVILEGE")).thenReturn(true);
    when(resourceResolver.resolve(PrivilegeResourceOrigin.ITIP, "PRIVILEGE", resourceId))
        .thenReturn(Optional.of(Map.of("ownerId", account.getId().toString())));
    authorizationService =
        new AuthorizationService(
            accountService, List.of(resourceResolver), new CelConditionExpressionEvaluator());

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(resourceId))).getFirst();

    assertThat(decision.allowed()).isTrue();
  }

  private AuthorizationCheck check(UUID resourceId) {
    return new AuthorizationCheck(
        PrivilegeResourceOrigin.ITIP, "PRIVILEGE", PrivilegeAction.CREATE, resourceId);
  }

  private Privilege privilege(PrivilegeEffect effect, String condition) {
    return Privilege.builder()
        .id(UUID.randomUUID())
        .effect(effect)
        .resourceOrigin(PrivilegeResourceOrigin.ITIP)
        .resource("PRIVILEGE")
        .action(PrivilegeAction.CREATE)
        .conditionExpression(condition)
        .build();
  }
}
