package cloud.poesis.itip.web.backend.auth.strategy;

import cloud.poesis.itip.web.backend.auth.model.AuthMethod;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationStrategyResolver {

  private final List<AuthenticationStrategy> strategies;

  public AuthenticationStrategy resolve(AuthMethod method) {
    return strategies.stream()
        .filter(strategy -> strategy.supports(method))
        .findFirst()
        .orElseThrow(
            () ->
                new UnsupportedAuthMethodException(
                    "No authentication strategy found for method: " + method));
  }
}
