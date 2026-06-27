package cloud.poesis.itip.web.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class AuthTokenServiceTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  private SecretKey signingKey() {
    return io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  }

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

  @Test
  void extractEmailShouldThrowForMalformedToken() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);

    assertThatThrownBy(() -> service.extractEmail("not-a-jwt"))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void shouldExposeConfiguredExpirationMs() {
    AuthTokenService service = new AuthTokenService(SECRET, 12345L);

    assertThat(service.getExpirationMs()).isEqualTo(12345L);
  }

  @Test
  void isTokenValidShouldReturnFalseWhenUserDetailsUsernameIsNull() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);
    Account account = Account.builder().id(UUID.randomUUID()).email("user@itip.local").build();
    String token = service.generateToken(account);
    UserDetails userDetails =
        User.withUsername("placeholder").password("ignored").authorities("READ_USER").build();

    UserDetails userDetailsWithNullUsername =
        new UserDetails() {
          @Override
          public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
              getAuthorities() {
            return userDetails.getAuthorities();
          }

          @Override
          public String getPassword() {
            return userDetails.getPassword();
          }

          @Override
          public String getUsername() {
            return null;
          }
        };

    assertThat(service.isTokenValid(token, userDetailsWithNullUsername)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseWhenTokenIsNull() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(null, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseWhenEmailMatchesButTokenIsExpired() {
    AuthTokenService service = new AuthTokenService(SECRET, -1000);
    Account account = Account.builder().id(UUID.randomUUID()).email("user@itip.local").build();
    String token = service.generateToken(account);
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(token, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseWhenSubjectClaimIsMissing() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);
    String tokenWithoutSubject =
        Jwts.builder()
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(signingKey(), Jwts.SIG.HS256)
            .compact();
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(tokenWithoutSubject, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseWhenExpirationClaimIsMissing() {
    AuthTokenService service = new AuthTokenService(SECRET, 3600000);
    String tokenWithoutExpiration =
        Jwts.builder()
            .subject("user@itip.local")
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(Instant.now()))
            .signWith(signingKey(), Jwts.SIG.HS256)
            .compact();
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(tokenWithoutExpiration, userDetails)).isFalse();
  }
}
