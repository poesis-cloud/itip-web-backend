package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import cloud.poesis.itip.web.backend.auth.entity.Privilege;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeEffect;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationCheck;
import cloud.poesis.itip.web.backend.auth.model.AuthorizationDecision;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    boolean allowed = false;
    if (hasValidShape(check)) {
      List<Privilege> matching = privileges.stream().filter(matches(check)).toList();
      boolean denied =
          matching.stream()
              .filter(privilege -> privilege.getEffect() == PrivilegeEffect.DENY)
              .anyMatch(privilege -> applies(privilege, account, check));
      boolean allowedRule =
          matching.stream()
              .filter(privilege -> privilege.getEffect() == PrivilegeEffect.ALLOW)
              .anyMatch(privilege -> applies(privilege, account, check));
      allowed = !denied && allowedRule;
    }
    return new AuthorizationDecision(
        check.origin(), check.resource(), check.action(), check.resourceId(), allowed);
  }

  private Predicate<Privilege> matches(AuthorizationCheck check) {
    return privilege ->
        privilege.getResourceOrigin() == check.origin()
            && Objects.equals(privilege.getResource(), check.resource())
            && privilege.getAction() == check.action();
  }

  private boolean applies(Privilege privilege, Account account, AuthorizationCheck check) {
    Optional<Map<String, Object>> target =
        check.resourceId() == null ? Optional.empty() : resolve(check);
    if (check.resourceId() != null && target.isEmpty()) {
      return false;
    }

    String condition = privilege.getConditionExpression();
    if (condition == null || condition.isBlank()) {
      return true;
    }
    if (check.resourceId() == null) {
      return false;
    }
    return conditionExpressionEvaluator.evaluate(
        condition,
        Map.of("actor", Map.of("id", account.getId().toString()), "target", target.get()));
  }

  private Optional<Map<String, Object>> resolve(AuthorizationCheck check) {
    return resourceResolvers.stream()
        .filter(resolver -> resolver.supports(check.origin(), check.resource()))
        .findFirst()
        .flatMap(
            resolver -> resolver.resolve(check.origin(), check.resource(), check.resourceId()));
  }

  private static boolean hasValidShape(AuthorizationCheck check) {
    return check != null
        && check.origin() != null
        && check.resource() != null
        && !check.resource().isBlank()
        && check.action() != null;
  }
}
