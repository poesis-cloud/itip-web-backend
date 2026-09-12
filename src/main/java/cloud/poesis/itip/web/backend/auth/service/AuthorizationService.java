package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityStatus;
import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheck;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationDecision;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service
public class AuthorizationService {

  private final AccountService accountService;
  private final List<AuthorizationResourceResolver> resourceResolvers;
  private final ConditionExpressionEvaluator conditionExpressionEvaluator;
  private final Duration batchDeadline;
  private final ExecutorService resolutionExecutor;

  public AuthorizationService(
      AccountService accountService,
      List<AuthorizationResourceResolver> resourceResolvers,
      ConditionExpressionEvaluator conditionExpressionEvaluator,
      @Value("${itip.authorization.batch-timeout-ms:8000}") long batchTimeoutMs) {
    this.accountService = accountService;
    this.resourceResolvers = List.copyOf(resourceResolvers);
    this.conditionExpressionEvaluator = conditionExpressionEvaluator;
    this.batchDeadline = Duration.ofMillis(batchTimeoutMs);
    this.resolutionExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  @PreDestroy
  void shutdown() {
    resolutionExecutor.shutdownNow();
  }

  public List<AuthorizationDecision> checkMany(String email, List<AuthorizationCheck> checks) {
    Account account = accountService.loadAccountWithRolesAndCapabilities(email);
    List<RoleCapabilityGrant> roleCapabilityGrants =
        accountService.activeRoleCapabilityGrants(account);
    Map<ResourceKey, Optional<Map<String, Object>>> resolutions =
        resolveDistinctTargets(account, roleCapabilityGrants, checks);
    return checks.stream()
        .map(check -> decide(account, roleCapabilityGrants, check, resolutions))
        .toList();
  }

  /**
   * Resolves every distinct resource that at least one check could possibly need, concurrently and
   * within a single overall deadline, so a batch of up to 100 checks has a bounded total latency
   * regardless of how many distinct downstream lookups it triggers. A resolution that does not
   * complete before the deadline is treated as a failure (fail-closed), matching the synchronous
   * per-call failure handling in {@link #resolve(AuthorizationCheck)}.
   */
  private Map<ResourceKey, Optional<Map<String, Object>>> resolveDistinctTargets(
      Account account,
      List<RoleCapabilityGrant> roleCapabilityGrants,
      List<AuthorizationCheck> checks) {
    Map<ResourceKey, AuthorizationCheck> distinctTargets = new LinkedHashMap<>();
    for (AuthorizationCheck check : checks) {
      if (check == null
          || !hasValidShape(check)
          || !account.isEnabled()
          || check.resourceId() == null) {
        continue;
      }
      boolean hasCandidate =
          roleCapabilityGrants.stream()
              .filter(matches(check))
              .anyMatch(grant -> grant.getCapability().getStatus() == CapabilityStatus.ACTIVE);
      if (!hasCandidate) {
        continue;
      }
      distinctTargets.putIfAbsent(
          new ResourceKey(check.origin(), check.resource(), check.resourceId()), check);
    }
    if (distinctTargets.isEmpty()) {
      return Map.of();
    }

    // Capture the current servlet request attributes (which RestDefmanClient reads to forward
    // the caller's Authorization header) before scheduling work onto the executor: virtual
    // threads spawned by supplyAsync/submit do not inherit the request-thread ThreadLocal, so
    // downstream calls made without this would silently omit the bearer token.
    RequestAttributes callerRequestAttributes = RequestContextHolder.getRequestAttributes();

    Map<ResourceKey, Future<Optional<Map<String, Object>>>> futures = new LinkedHashMap<>();
    distinctTargets.forEach(
        (key, check) ->
            futures.put(
                key,
                resolutionExecutor.submit(
                    () -> resolveWithRequestContext(check, callerRequestAttributes))));

    Instant deadline = Instant.now().plus(batchDeadline);
    Map<ResourceKey, Optional<Map<String, Object>>> resolutions = new HashMap<>();
    futures.forEach(
        (key, future) -> {
          Duration remaining = Duration.between(Instant.now(), deadline);
          try {
            resolutions.put(
                key, future.get(Math.max(remaining.toMillis(), 0), TimeUnit.MILLISECONDS));
          } catch (TimeoutException | ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
              Thread.currentThread().interrupt();
            }
            // cancel(true) interrupts the virtual thread running the task (unlike
            // CompletableFuture, a submitted Future's underlying FutureTask propagates the
            // interrupt to a still-running task), which aborts the blocking RestClient call
            // instead of leaving it running against Defman past the batch deadline.
            future.cancel(true);
            resolutions.put(key, Optional.empty());
          }
        });
    return resolutions;
  }

  /**
   * Runs {@link #resolve(AuthorizationCheck)} on the executor's virtual thread with the caller's
   * servlet request attributes installed, so {@code RequestContextHolder}-based downstream clients
   * (e.g. {@code RestDefmanClient}) can still read the original Authorization header.
   */
  private Optional<Map<String, Object>> resolveWithRequestContext(
      AuthorizationCheck check, RequestAttributes callerRequestAttributes) {
    RequestContextHolder.setRequestAttributes(callerRequestAttributes);
    try {
      return resolve(check);
    } finally {
      RequestContextHolder.resetRequestAttributes();
    }
  }

  private AuthorizationDecision decide(
      Account account,
      List<RoleCapabilityGrant> roleCapabilityGrants,
      AuthorizationCheck check,
      Map<ResourceKey, Optional<Map<String, Object>>> resolutions) {
    Objects.requireNonNull(check, "check is required");
    if (!hasValidShape(check) || !account.isEnabled()) {
      return denied(check);
    }

    List<RoleCapabilityGrant> candidates =
        roleCapabilityGrants.stream()
            .filter(matches(check))
            .filter(grant -> grant.getCapability().getStatus() == CapabilityStatus.ACTIVE)
            .toList();
    if (candidates.isEmpty()) {
      return denied(check);
    }

    Optional<Map<String, Object>> target =
        check.resourceId() != null
            ? resolutions.getOrDefault(
                new ResourceKey(check.origin(), check.resource(), check.resourceId()),
                Optional.empty())
            : Optional.empty();
    boolean allowed =
        (check.resourceId() == null || target.isPresent())
            && candidates.stream().anyMatch(grant -> applies(grant, account, target));
    return decision(check, allowed);
  }

  private static AuthorizationDecision denied(AuthorizationCheck check) {
    return decision(check, false);
  }

  private static AuthorizationDecision decision(AuthorizationCheck check, boolean allowed) {
    return new AuthorizationDecision(
        check.origin(), check.resource(), check.operation(), check.resourceId(), allowed);
  }

  private Predicate<RoleCapabilityGrant> matches(AuthorizationCheck check) {
    return grant ->
        grant.getCapability().getResourceOrigin() == check.origin()
            && Objects.equals(grant.getCapability().getResource(), check.resource())
            && grant.getCapability().getOperation() == check.operation();
  }

  private boolean applies(
      RoleCapabilityGrant grant, Account account, Optional<Map<String, Object>> target) {
    return applies(grant.getCapability().getPolicies(), account, target)
        && applies(grant.getPolicies(), account, target);
  }

  private boolean applies(
      Set<Policy> policies, Account account, Optional<Map<String, Object>> target) {
    if (policies.isEmpty()) {
      return true;
    }
    if (target.isEmpty()) {
      return false;
    }
    Map<String, Object> context =
        Map.of("actor", Map.of("id", account.getId().toString()), "target", target.get());
    return policies.stream()
        .allMatch(
            policy ->
                conditionExpressionEvaluator.evaluate(policy.getConditionExpression(), context));
  }

  private Optional<Map<String, Object>> resolve(AuthorizationCheck check) {
    try {
      return resourceResolvers.stream()
          .filter(resolver -> resolver.supports(check.origin(), check.resource()))
          .findFirst()
          .flatMap(
              resolver -> resolver.resolve(check.origin(), check.resource(), check.resourceId()));
    } catch (RuntimeException exception) {
      return Optional.empty();
    }
  }

  private static boolean hasValidShape(AuthorizationCheck check) {
    return check.origin() != null
        && check.resource() != null
        && !check.resource().isBlank()
        && check.operation() != null;
  }

  private record ResourceKey(
      CapabilityResourceOrigin origin, String resource, java.util.UUID resourceId) {}
}
