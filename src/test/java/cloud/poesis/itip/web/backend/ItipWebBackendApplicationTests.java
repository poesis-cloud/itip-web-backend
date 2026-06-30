package cloud.poesis.itip.web.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ItipWebBackendApplicationTests {

  @Test
  void contextLoads() {}

  @TestConfiguration
  static class TestSecurityBeans {

    @Bean
    JwtDecoder jwtDecoder() {
      return token -> null;
    }
  }
}
