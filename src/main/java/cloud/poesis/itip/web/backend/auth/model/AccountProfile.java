package cloud.poesis.itip.web.backend.auth.model;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountProfile {
  private UUID id;
  private String email;
  private String fullName;
  private List<String> roles;
  private List<String> privileges;
}
