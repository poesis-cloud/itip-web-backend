package cloud.poesis.itip.web.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class SecurityConfigTest {

  @Autowired private SecurityFilterChain securityFilterChain;

  @Test
  void securityFilterChainIsConfigured() {
    assertThat(securityFilterChain).isNotNull();
  }
}
