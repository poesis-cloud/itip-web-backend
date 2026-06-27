package cloud.poesis.itip.web.backend.auth.api;

import cloud.poesis.itip.web.backend.auth.model.AuthMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
  private String email;
  private String password;

  @Default private AuthMethod authMethod = AuthMethod.LOCAL;
}
