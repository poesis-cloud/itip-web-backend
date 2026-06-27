package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public final class AuthTokenService {

  private final SecretKey signingKey;

  @Getter private final long expirationMs;

  public AuthTokenService(
      @Value("${itip.security.jwt.secret}") String jwtSecret,
      @Value("${itip.security.jwt.expiration-ms}") long expirationMs) {
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generateToken(Account account) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusMillis(expirationMs);

    return Jwts.builder()
        .subject(account.getEmail())
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  public String extractEmail(String token) {
    return extractAllClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
      Claims claims = extractAllClaims(token);
      String email = claims.getSubject();
      Date expiration = claims.getExpiration();

      return email != null && email.equals(userDetails.getUsername()) && expiration != null;
    } catch (JwtException | IllegalArgumentException exception) {
      return false;
    }
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
