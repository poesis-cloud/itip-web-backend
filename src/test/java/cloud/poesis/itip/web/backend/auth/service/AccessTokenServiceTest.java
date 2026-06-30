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

class AccessTokenServiceTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  private SecretKey signingKey() {
    return io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  }

  private String tokenForEmail(String email, Instant expiration) {
    return Jwts.builder()
        .subject(email)
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(expiration))
        .signWith(signingKey(), Jwts.SIG.HS256)
        .compact();
  }

  @Test
  void shouldGenerateTokenAndExposeExpiration() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
    Account account = Account.builder().id(UUID.randomUUID()).email("user@itip.local").build();

    String token = service.generateToken(account);

    assertThat(service.getExpirationMs()).isEqualTo(3600000L);
    assertThat(
            Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject())
        .isEqualTo("user@itip.local");
  }

  @Test
  void shouldRespectConfiguredExpirationWindow() {
    AccessTokenService service = new AccessTokenService(SECRET, 12345L);
    Account account = Account.builder().id(UUID.randomUUID()).email("user@itip.local").build();

    String token = service.generateToken(account);
    Date expiration =
        Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration();

    assertThat(service.getExpirationMs()).isEqualTo(12345L);
    assertThat(expiration).isNotNull();
  }

  @Test
  void shouldExtractEmailFromValidToken() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
    String token = tokenForEmail("user@itip.local", Instant.now().plusSeconds(60));

    assertThat(service.extractEmail(token)).isEqualTo("user@itip.local");
  }

  @Test
  void isTokenValidShouldReturnTrueForMatchingUser() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
    String token = tokenForEmail("user@itip.local", Instant.now().plusSeconds(60));
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(token, userDetails)).isTrue();
  }

  @Test
  void isTokenValidShouldReturnFalseForDifferentUser() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
    String token = tokenForEmail("user@itip.local", Instant.now().plusSeconds(60));
    UserDetails userDetails =
        User.withUsername("other@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(token, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseForExpiredToken() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
    String token = tokenForEmail("user@itip.local", Instant.now().minusSeconds(60));
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(token, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseForMalformedToken() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid("not-a-jwt", userDetails)).isFalse();
  }

  @Test
  void extractEmailShouldThrowForMalformedToken() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);

    assertThatThrownBy(() -> service.extractEmail("not-a-jwt"))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void isTokenValidShouldReturnFalseWhenUserDetailsUsernameIsNull() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
    String token = tokenForEmail("user@itip.local", Instant.now().plusSeconds(60));
    UserDetails userDetails =
        new UserDetails() {
          @Override
          public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
              getAuthorities() {
            return java.util.List.of();
          }

          @Override
          public String getPassword() {
            return "ignored";
          }

          @Override
          public String getUsername() {
            return null;
          }
        };

    assertThat(service.isTokenValid(token, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseWhenTokenIsNull() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
    UserDetails userDetails =
        User.withUsername("user@itip.local").password("ignored").authorities("READ_USER").build();

    assertThat(service.isTokenValid(null, userDetails)).isFalse();
  }

  @Test
  void isTokenValidShouldReturnFalseWhenSubjectClaimIsMissing() {
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
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
    AccessTokenService service = new AccessTokenService(SECRET, 3600000L);
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
