package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheck;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  void doesNotResolveTargetWhenNoEnabledPrivilegeMatches() {
    when(accountService.activePrivileges(account)).thenReturn(List.of());

    var decision =
        authorizationService
            .checkMany(account.getEmail(), List.of(check(UUID.randomUUID())))
            .getFirst();

    assertThat(decision.allowed()).isFalse();
    verifyNoInteractions(resourceResolver);
  }

  @Test
  void allowsMatchingEnabledCapabilityWithoutPolicies() {
    when(accountService.activePrivileges(account)).thenReturn(List.of(privilege(capability(true))));

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isTrue();
  }

  @Test
  void deniesWhenAccountIsDisabled() {
    account.setEnabled(false);
    when(accountService.activePrivileges(account))
        .thenReturn(List.of(privilege(capability(true), policy("true"))));

    var decision =
        authorizationService
            .checkMany(account.getEmail(), List.of(check(UUID.randomUUID())))
            .getFirst();

    assertThat(decision.allowed()).isFalse();
    verifyNoInteractions(resourceResolver);
  }

  @Test
  void deniesWhenMatchingCapabilityIsDisabled() {
    when(accountService.activePrivileges(account))
        .thenReturn(List.of(privilege(capability(false))));

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void deniesWhenAnyCapabilityPolicyDoesNotApply() {
    UUID resourceId = UUID.randomUUID();
    when(accountService.activePrivileges(account))
        .thenReturn(
            List.of(
                privilege(
                    capability(true, policy("actor.id == target.ownerId"), policy("false")))));
    when(resourceResolver.supports(PrivilegeResourceOrigin.ITIP, "PRIVILEGE")).thenReturn(true);
    when(resourceResolver.resolve(PrivilegeResourceOrigin.ITIP, "PRIVILEGE", resourceId))
        .thenReturn(Optional.of(Map.of("ownerId", account.getId().toString())));
    when(conditionExpressionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("actor.id == target.ownerId"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    when(conditionExpressionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("false"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(resourceId))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void allowsWhenOneOfMultiplePrivilegesApplies() {
    UUID resourceId = UUID.randomUUID();
    when(accountService.activePrivileges(account))
        .thenReturn(
            List.of(
                privilege(capability(true), policy("false")),
                privilege(capability(true), policy("actor.id == target.ownerId"))));
    when(resourceResolver.supports(PrivilegeResourceOrigin.ITIP, "PRIVILEGE")).thenReturn(true);
    when(resourceResolver.resolve(PrivilegeResourceOrigin.ITIP, "PRIVILEGE", resourceId))
        .thenReturn(Optional.of(Map.of("ownerId", account.getId().toString())));
    when(conditionExpressionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("false"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);
    when(conditionExpressionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("actor.id == target.ownerId"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(resourceId))).getFirst();

    assertThat(decision.allowed()).isTrue();
  }

  @Test
  void resolvesTargetOnceForMultipleMatchingPrivileges() {
    UUID resourceId = UUID.randomUUID();
    when(accountService.activePrivileges(account))
        .thenReturn(
            List.of(
                privilege(capability(true), policy("true")),
                privilege(capability(true), policy("true"))));
    when(resourceResolver.supports(PrivilegeResourceOrigin.ITIP, "PRIVILEGE")).thenReturn(true);
    when(resourceResolver.resolve(PrivilegeResourceOrigin.ITIP, "PRIVILEGE", resourceId))
        .thenReturn(Optional.of(Map.of()));

    authorizationService.checkMany(account.getEmail(), List.of(check(resourceId)));

    verify(resourceResolver).resolve(PrivilegeResourceOrigin.ITIP, "PRIVILEGE", resourceId);
    verify(resourceResolver, never()).resolve(PrivilegeResourceOrigin.ITIP, "OTHER", resourceId);
  }

  private AuthorizationCheck check(UUID resourceId) {
    return new AuthorizationCheck(
        PrivilegeResourceOrigin.ITIP, "PRIVILEGE", PrivilegeAction.CREATE, resourceId);
  }

  private static Privilege privilege(Capability capability) {
    return Privilege.builder().id(UUID.randomUUID()).capability(capability).build();
  }

  private static Privilege privilege(Capability capability, Policy... policies) {
    return Privilege.builder()
        .id(UUID.randomUUID())
        .capability(capability)
        .policies(Set.of(policies))
        .build();
  }

  private static Capability capability(boolean enabled, Policy... policies) {
    return Capability.builder()
        .id(UUID.randomUUID())
        .resourceOrigin(PrivilegeResourceOrigin.ITIP)
        .resource("PRIVILEGE")
        .operation(PrivilegeAction.CREATE)
        .enabled(enabled)
        .policies(new LinkedHashSet<>(List.of(policies)))
        .build();
  }

  private static Policy policy(String conditionExpression) {
    return Policy.builder().id(UUID.randomUUID()).conditionExpression(conditionExpression).build();
  }
}
