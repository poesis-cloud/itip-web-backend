package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityStatus;
import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.entity.RoleCapabilityGrant;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheck;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationDecision;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

  private final AccountService accountService;
  private final List<AuthorizationResourceResolver> resourceResolvers;
  private final ConditionExpressionEvaluator conditionExpressionEvaluator;

  public List<AuthorizationDecision> checkMany(String email, List<AuthorizationCheck> checks) {
    Account account = accountService.loadAccountWithRolesAndCapabilities(email);
    List<RoleCapabilityGrant> roleCapabilityGrants =
        accountService.activeRoleCapabilityGrants(account);
    Map<ResourceKey, Optional<Map<String, Object>>> resolutionCache = new HashMap<>();
    return checks.stream()
        .map(check -> decide(account, roleCapabilityGrants, check, resolutionCache))
        .toList();
  }

  private AuthorizationDecision decide(
      Account account,
      List<RoleCapabilityGrant> roleCapabilityGrants,
      AuthorizationCheck check,
      Map<ResourceKey, Optional<Map<String, Object>>> resolutionCache) {
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
        check.resourceId() != null ? resolveCached(check, resolutionCache) : Optional.empty();
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

  private Optional<Map<String, Object>> resolveCached(
      AuthorizationCheck check, Map<ResourceKey, Optional<Map<String, Object>>> resolutionCache) {
    ResourceKey key = new ResourceKey(check.origin(), check.resource(), check.resourceId());
    return resolutionCache.computeIfAbsent(key, ignored -> resolve(check));
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
