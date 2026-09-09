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
import java.time.Duration;
import java.time.Instant;
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
            accountService, List.of(resourceResolver), conditionExpressionEvaluator, 8000);
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

  @Test
  void resolvesMultipleDistinctTargetsConcurrentlyWithinBoundedLatency() {
    // Regression pin for thread 25 follow-up: N distinct slow resolutions must not add up
    // sequentially. Each simulated resolver call blocks ~200ms; with 20 distinct resources
    // resolved concurrently the whole batch must still complete well under 20 * 200ms.
    int distinctResourceCount = 20;
    List<AuthorizationCheck> checks = new java.util.ArrayList<>();
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.ACTIVE), policy("true"))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(conditionExpressionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("true"), org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);
    for (int i = 0; i < distinctResourceCount; i++) {
      UUID resourceId = UUID.randomUUID();
      checks.add(check(resourceId));
      when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
          .thenAnswer(
              invocation -> {
                Thread.sleep(200);
                return Optional.of(Map.of());
              });
    }

    Instant start = Instant.now();
    var decisions = authorizationService.checkMany(account.getEmail(), checks);
    Duration elapsed = Duration.between(start, Instant.now());

    assertThat(decisions).hasSize(distinctResourceCount).allMatch(AuthorizationDecision::allowed);
    assertThat(elapsed).isLessThan(Duration.ofMillis(200L * distinctResourceCount / 2));
  }

  @Test
  void deniesResolutionsThatDoNotCompleteWithinTheBatchDeadline() {
    authorizationService =
        new AuthorizationService(
            accountService, List.of(resourceResolver), conditionExpressionEvaluator, 100);
    UUID resourceId = UUID.randomUUID();
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.ACTIVE), policy("true"))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
        .thenAnswer(
            invocation -> {
              Thread.sleep(2000);
              return Optional.of(Map.of());
            });

    Instant start = Instant.now();
    var decision =
        authorizationService.checkMany(account.getEmail(), List.of(check(resourceId))).getFirst();
    Duration elapsed = Duration.between(start, Instant.now());

    assertThat(decision.allowed()).isFalse();
    // The batch call itself must return around the 100ms deadline, not wait out the full 2s
    // resolver delay; this is the observable proof that checkMany() does not block the caller
    // past the configured deadline (the separate concern of whether the abandoned downstream
    // call is actually interrupted is covered by resolverThreadIsInterruptedWhenDeadlineElapses).
    assertThat(elapsed).isLessThan(Duration.ofSeconds(1));
  }

  @Test
  void resolverThreadIsInterruptedWhenDeadlineElapses() throws InterruptedException {
    // Regression pin: cancel(true) on a submitted Future must actually interrupt the running
    // virtual thread so an abandoned downstream call is aborted instead of running to
    // completion after the batch deadline has already returned a denial to the caller.
    authorizationService =
        new AuthorizationService(
            accountService, List.of(resourceResolver), conditionExpressionEvaluator, 100);
    UUID resourceId = UUID.randomUUID();
    java.util.concurrent.CountDownLatch interrupted = new java.util.concurrent.CountDownLatch(1);
    when(accountService.activeRoleCapabilityGrants(account))
        .thenReturn(List.of(grant(capability(CapabilityStatus.ACTIVE), policy("true"))));
    when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
    when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
        .thenAnswer(
            invocation -> {
              try {
                Thread.sleep(5000);
              } catch (InterruptedException e) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
              }
              return Optional.of(Map.of());
            });

    authorizationService.checkMany(account.getEmail(), List.of(check(resourceId)));

    assertThat(interrupted.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void propagatesCallerRequestAttributesToResolverThread() {
    // Regression pin: RestDefmanClient reads the Authorization header from
    // RequestContextHolder, whose ThreadLocal is not inherited by the virtual threads the
    // resolution executor spawns. checkMany() must install the caller's request attributes on
    // those threads so downstream calls still carry the bearer token.
    org.springframework.mock.web.MockHttpServletRequest servletRequest =
        new org.springframework.mock.web.MockHttpServletRequest();
    servletRequest.addHeader("Authorization", "Bearer test-token");
    org.springframework.web.context.request.RequestAttributes callerAttributes =
        new org.springframework.web.context.request.ServletRequestAttributes(servletRequest);
    org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
        callerAttributes);
    try {
      UUID resourceId = UUID.randomUUID();
      java.util.concurrent.atomic.AtomicReference<String> observedAuthorizationHeader =
          new java.util.concurrent.atomic.AtomicReference<>();
      when(accountService.activeRoleCapabilityGrants(account))
          .thenReturn(List.of(grant(capability(CapabilityStatus.ACTIVE), policy("true"))));
      when(resourceResolver.supports(CapabilityResourceOrigin.ITIP, "CAPABILITY")).thenReturn(true);
      when(resourceResolver.resolve(CapabilityResourceOrigin.ITIP, "CAPABILITY", resourceId))
          .thenAnswer(
              invocation -> {
                var attributes =
                    org.springframework.web.context.request.RequestContextHolder
                        .getRequestAttributes();
                if (attributes
                    instanceof
                    org.springframework.web.context.request.ServletRequestAttributes servletAttrs) {
                  observedAuthorizationHeader.set(
                      servletAttrs.getRequest().getHeader("Authorization"));
                }
                return Optional.of(Map.of());
              });

      authorizationService.checkMany(account.getEmail(), List.of(check(resourceId)));

      assertThat(observedAuthorizationHeader.get()).isEqualTo("Bearer test-token");
    } finally {
      org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }
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
