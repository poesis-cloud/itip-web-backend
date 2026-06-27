package cloud.poesis.itip.web.backend.auth.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cloud.poesis.itip.web.backend.auth.model.AuthMethod;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuthenticationStrategyResolverTest {

  @Test
  void resolveShouldReturnMatchingStrategy() {
    AuthenticationStrategy localStrategy = Mockito.mock(AuthenticationStrategy.class);
    AuthenticationStrategy googleStrategy = Mockito.mock(AuthenticationStrategy.class);

    Mockito.when(localStrategy.supports(AuthMethod.LOCAL)).thenReturn(true);
    Mockito.when(googleStrategy.supports(AuthMethod.LOCAL)).thenReturn(false);

    AuthenticationStrategyResolver resolver =
        new AuthenticationStrategyResolver(List.of(googleStrategy, localStrategy));

    assertThat(resolver.resolve(AuthMethod.LOCAL)).isSameAs(localStrategy);
  }

  @Test
  void resolveShouldThrowWhenNoStrategyMatches() {
    AuthenticationStrategy strategy = Mockito.mock(AuthenticationStrategy.class);
    Mockito.when(strategy.supports(AuthMethod.SAML)).thenReturn(false);

    AuthenticationStrategyResolver resolver = new AuthenticationStrategyResolver(List.of(strategy));

    assertThatThrownBy(() -> resolver.resolve(AuthMethod.SAML))
        .isInstanceOf(UnsupportedAuthMethodException.class)
        .hasMessageContaining("SAML");
  }
}
