package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class AuthTokenServiceTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  @Test
  void shouldGenerateTokenAndExtractEmail() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);
    Account account = Account.builder().id(UUID.randomUUID()).email("user@itip.local").build();

    String token = service.generateToken(account);
    String extractedEmail = service.extractEmail(token);

    assertThat(token).isNotBlank();
    assertThat(extractedEmail).isEqualTo("user@itip.local");
  }

  @Test
  void isTokenValidShouldReturnTrueForMatchingUser() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);
    Account account = Account.builder().id(UUID.randomUUID()).email("user@itip.local").build();
    String token = service.generateToken(account);
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(token, userDetails)).isTrue();
  }

  @Test
  void isTokenValidShouldReturnFalseForDifferentUser() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);
    Account account = Account.builder().id(UUID.randomUUID()).email("user@itip.local").build();
    String token = service.generateToken(account);
    UserDetails userDetails =
        User.withUsername("other@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(token, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseForExpiredToken() {
    AuthTokenService service = new AuthTokenService(SECRET, -1000);
    Account account = Account.builder().id(UUID.randomUUID()).email("user@itip.local").build();
    String token = service.generateToken(account);
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(token, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseForMalformedToken() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid("not-a-jwt", userDetails)).isFalse();
  }
}
