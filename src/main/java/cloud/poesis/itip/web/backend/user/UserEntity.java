package cloud.poesis.itip.web.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class UserEntity {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(name = "username", nullable = false, unique = true, length = 100)
  @NonNull
  private String username;

  @Column(name = "first_name", nullable = false, length = 100)
  @NonNull
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  @NonNull
  private String lastName;
}
