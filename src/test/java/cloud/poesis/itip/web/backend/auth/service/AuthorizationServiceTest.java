package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.Capability;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityOperation;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityStatus;
import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheck;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationDecision;
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
    when(accountService.loadAccountWithRolesAndCapabilities(account.getEmail()))
        .thenReturn(account);
  }

  @Test
  void deniesByDefaultWhenNoGrantMatches() {
    when(accountService.activeRoleCapabilityGrants(account)).thenReturn(List.of());

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void doesNotResolveTargetWhenNoEnabledGrantMatches() {
    when(accountService.activeRoleCapabilityGrants(account)).thenReturn(List.of());

    var decision =
        authorizationService
            .checkMany(account.getEmail(), List.of(check(UUID.randomUUID())))
            .getFirst();

    assertThat(decision.allowed()).isFalse();
    verifyNoInteractions(resourceResolver);
  }

  @Test
  void allowsMatchingEnabledCapabilityWithoutPolicies() {
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.ACTIVE))));

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isTrue();
  }

  @Test
  void deniesWhenAccountIsDisabled() {
    account.setEnabled(false);
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.ACTIVE), policy("true"))));

    var decision =
        authorizationService
            .checkMany(account.getEmail(), List.of(check(UUID.randomUUID())))
            .getFirst();

    assertThat(decision.allowed()).isFalse();
    verifyNoInteractions(resourceResolver);
  }

  @Test
  void deniesWhenMatchingCapabilityIsDisabled() {
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.DISABLED))));

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void deniesWhenMatchingCapabilityIsDeprecated() {
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.DEPRECATED))));

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(null))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void capabilityPolicyVetoesEveryGrantOnTheSameCapability() {
    // Regression pin: the capability-level lock must deny even when another grant
    // on the same capability carries no policy (the bypass a global role would introduce).
    UUID resourceId = UUID.randomUUID();
    Capability lockedCapability = capability(CapabilityStatus.ACTIVE, policy("false"));
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(lockedCapability), grant(lockedCapability, policy("true"))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
        .thenReturn(Optional.of(Map.of()));
    when(conditionExpressionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("false"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);

    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(resourceId))).getFirst();

    assertThat(decision.allowed()).isFalse();
  }

  @Test
  void deniesWhenAnyCapabilityPolicyDoesNotApply() {
    UUID resourceId = UUID.randomUUID();
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(
            List.of(
                grant(
                    capability(
                        CapabilityStatus.ACTIVE,
                        policy("actor.id == target.ownerId"),
                        policy("false")))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
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
  void allowsWhenOneOfMultipleGrantsApplies() {
    UUID resourceId = UUID.randomUUID();
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(
            List.of(
                grant(capability(CapabilityStatus.ACTIVE), policy("false")),
                grant(capability(CapabilityStatus.ACTIVE), policy("actor.id == target.ownerId"))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
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
  void resolvesTargetOnceForMultipleMatchingGrants() {
    UUID resourceId = UUID.randomUUID();
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(
            List.of(
                grant(capability(CapabilityStatus.ACTIVE), policy("true")),
                grant(capability(CapabilityStatus.ACTIVE), policy("true"))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
        .thenReturn(Optional.of(Map.of()));

    authorizationService.checkMany(account.getEmail(), List.of(check(resourceId)));

    verify(resourceResolver).resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId);
    verify(resourceResolver, never()).resolve(CapabilityResourceOrigin.ITIP, "OTHER", resourceId);
  }

  @Test
  void resolvesDuplicateTargetsOncePerBatch() {
    UUID resourceId = UUID.randomUUID();
    AuthorizationCheck check = check(resourceId);
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.ACTIVE), policy("true"))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
        .thenReturn(Optional.of(Map.of()));
    when(conditionExpressionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("true"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);

    var decisions = authorizationService.checkMany(account.getEmail(), List.of(check, check));

    assertThat(decisions).allMatch(AuthorizationDecision::allowed);
    verify(resourceResolver, times(1))
        .resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId);
  }

  @Test
  void cachesResolutionFailuresWithinOneBatch() {
    UUID resourceId = UUID.randomUUID();
    AuthorizationCheck check = check(resourceId);
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.ACTIVE), policy("true"))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
        .thenThrow(new IllegalStateException("Defman unavailable"));

    var decisions = authorizationService.checkMany(account.getEmail(), List.of(check, check));

    assertThat(decisions).allMatch(decision -> !decision.allowed());
    verify(resourceResolver, times(1))
        .resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId);
    verifyNoInteractions(conditionExpressionEvaluator);
  }

  private AuthorizationCheck check(UUID resourceId) {
    return new AuthorizationCheck(
        CapabilityResourceOrigin.ITIP, "CAPABILITY", CapabilityOperation.CREATE, resourceId);
  }

  private static RoleCapabilityGrant grant(Capability capability) {
    return RoleCapabilityGrant.builder().id(UUID.randomUUID()).capability(capability).build();
  }

  private static RoleCapabilityGrant grant(Capability capability, Policy... policies) {
    return RoleCapabilityGrant.builder()
        .id(UUID.randomUUID())
        .capability(capability)
        .policies(Set.of(policies))
        .build();
  }

  private static Capability capability(CapabilityStatus status, Policy... policies) {
    return Capability.builder()
        .id(UUID.randomUUID())
        .resourceOrigin(CapabilityResourceOrigin.ITIP)
        .resource("CAPABILITY")
        .operation(CapabilityOperation.CREATE)
        .status(status)
        .policies(new LinkedHashSet<>(List.of(policies)))
        .build();
  }

  private static Policy policy(String conditionExpression) {
    return Policy.builder().id(UUID.randomUUID()).conditionExpression(conditionExpression).build();
  }
}
