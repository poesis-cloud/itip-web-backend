package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.Policy;
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheck;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationDecision;
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
    Account account = accountService.loadAccountWithRolesAndPrivileges(email);
    List<Privilege> privileges = accountService.activePrivileges(account);
    return checks.stream().map(check -> decide(account, privileges, check)).toList();
  }

  private AuthorizationDecision decide(
      Account account, List<Privilege> privileges, AuthorizationCheck check) {
    Objects.requireNonNull(check, "check is required");
    if (!hasValidShape(check) || !account.isEnabled()) {
      return denied(check);
    }

    List<Privilege> candidates =
        privileges.stream()
            .filter(matches(check))
            .filter(privilege -> privilege.getCapability().isEnabled())
            .toList();
    if (candidates.isEmpty()) {
      return denied(check);
    }

    Optional<Map<String, Object>> target =
        check.resourceId() != null ? resolve(check) : Optional.empty();
    boolean allowed =
        (check.resourceId() == null || target.isPresent())
            && candidates.stream().anyMatch(privilege -> applies(privilege, account, target));
    return decision(check, allowed);
  }

  private static AuthorizationDecision denied(AuthorizationCheck check) {
    return decision(check, false);
  }

  private static AuthorizationDecision decision(AuthorizationCheck check, boolean allowed) {
    return new AuthorizationDecision(
        check.origin(), check.resource(), check.operation(), check.resourceId(), allowed);
  }

  private Predicate<Privilege> matches(AuthorizationCheck check) {
    return privilege ->
        privilege.getCapability().getResourceOrigin() == check.origin()
            && Objects.equals(privilege.getCapability().getResource(), check.resource())
            && privilege.getCapability().getOperation() == check.operation();
  }

  private boolean applies(
      Privilege privilege, Account account, Optional<Map<String, Object>> target) {
    return applies(privilege.getCapability().getPolicies(), account, target)
        && applies(privilege.getPolicies(), account, target);
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
    return resourceResolvers.stream()
        .filter(resolver -> resolver.supports(check.origin(), check.resource()))
        .findFirst()
        .flatMap(
            resolver -> resolver.resolve(check.origin(), check.resource(), check.resourceId()));
  }

  private static boolean hasValidShape(AuthorizationCheck check) {
    return check.origin() != null
        && check.resource() != null
        && !check.resource().isBlank()
        && check.operation() != null;
  }
}
