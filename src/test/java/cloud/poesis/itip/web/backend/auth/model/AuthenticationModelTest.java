package cloud.poesis.itip.web.backend.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import cloud.poesis.itip.web.backend.auth.api.LoginResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthenticationModelTest {

  @Test
  void authenticationRequestShouldSupportBuilderAndAccessors() {
    AuthenticationRequest request =
        AuthenticationRequest.builder().email("alice@itip.local").password("secret").build();

    assertThat(request.getEmail()).isEqualTo("alice@itip.local");
    assertThat(request.getPassword()).isEqualTo("secret");
    request.setPassword("changed");
    assertThat(request.getPassword()).isEqualTo("changed");
  }

  @Test
  void authenticationResultShouldSupportBuilderAndAccessors() {
    Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");
    AuthenticationResult result =
        AuthenticationResult.builder().token("token").expiresAt(expiresAt).build();

    assertThat(result.getToken()).isEqualTo("token");
    assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
    result.setToken("updated-token");
    assertThat(result.getToken()).isEqualTo("updated-token");
  }

  @Test
  void loginResponseShouldSupportBuilderAndAccessors() {
    Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");
    LoginResponse response = LoginResponse.builder().token("token").expiresAt(expiresAt).build();

    assertThat(response.getToken()).isEqualTo("token");
    assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
    response.setToken("updated-token");
    assertThat(response.getToken()).isEqualTo("updated-token");
  }
}
