package cloud.poesis.itip.web.backend.auth.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class NoOpTenantResolverTest {

  @Test
  void resolveShouldAlwaysReturnEmpty() {
    NoOpTenantResolver resolver = new NoOpTenantResolver();

    assertThat(resolver.resolve(new MockHttpServletRequest())).isEmpty();
  }
}
