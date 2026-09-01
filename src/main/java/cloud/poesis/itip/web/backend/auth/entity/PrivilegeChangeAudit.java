package cloud.poesis.itip.web.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "privilege_change_audit")
@Getter
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PrivilegeChangeAudit {

  @Id
  @GenerateUuidV7
  @EqualsAndHashCode.Include
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "privilege_id",
      nullable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_privilege_change_audit_privilege"))
  private Privilege privilege;

  @Column(name = "privilege_version", nullable = false, updatable = false)
  private long privilegeVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, updatable = false)
  private PrivilegeAuditActionType actionType;

  @Column(name = "previous_state", nullable = false, updatable = false, columnDefinition = "TEXT")
  private String previousState;

  @Column(name = "new_state", nullable = false, updatable = false, columnDefinition = "TEXT")
  private String newState;

  @Column(name = "action_by", nullable = false, updatable = false)
  private String actionBy;

  @CreationTimestamp
  @Column(name = "action_at", nullable = false, updatable = false)
  private Instant actionAt;
}
