package cloud.poesis.itip.web.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "account_role_assignment")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRoleAssignment {

  @Id
  @EqualsAndHashCode.Include
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false, insertable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "account_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_account_role_assignment_account"))
  private Account account;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "role_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_account_role_assignment_role"))
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(
      name = "assigned_by",
      nullable = true,
      foreignKey = @ForeignKey(name = "fk_account_role_assignment_assigned_by"))
  private Account assignedBy;

  @CreationTimestamp
  @Column(name = "assigned_at", nullable = false, updatable = false)
  private Instant assignedAt;

  @Column(name = "expires_at", nullable = true)
  private Instant expiresAt;

  @Column(name = "revoked_at", nullable = true)
  private Instant unassignedAt;
}
