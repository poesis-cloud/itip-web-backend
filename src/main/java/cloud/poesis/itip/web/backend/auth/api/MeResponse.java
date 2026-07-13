package cloud.poesis.itip.web.backend.auth.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
  private String id;
  private String email;
  private String fullName;
  private List<String> roles;
  private List<String> privileges;
}
