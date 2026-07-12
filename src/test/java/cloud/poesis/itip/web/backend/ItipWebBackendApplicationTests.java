package cloud.poesis.itip.web.backend;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
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
      return token ->
          Jwt.withTokenValue(token)
              .header("alg", "none")
              .claim("sub", "test-user")
              .issuedAt(Instant.now())
              .expiresAt(Instant.now().plusSeconds(3600))
              .claims(claims -> claims.putAll(Map.of()))
              .build();
    }
  }
}
