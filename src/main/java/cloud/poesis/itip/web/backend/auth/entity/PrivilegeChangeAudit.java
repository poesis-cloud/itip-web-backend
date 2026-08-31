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
import jakarta.persistence.PrePersist;
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
@Table(name = "privilege_change_audit")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivilegeChangeAudit {

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "privilege_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_privilege_change_audit_privilege"))
  private Privilege privilege;

  @Column(name = "privilege_version", nullable = false)
  private long privilegeVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false)
  private PrivilegeAuditActionType actionType;

  @Column(name = "previous_state", nullable = false, columnDefinition = "TEXT")
  private String previousState;

  @Column(name = "new_state", nullable = false, columnDefinition = "TEXT")
  private String newState;

  @Column(name = "action_by", nullable = false)
  private String actionBy;

  @CreationTimestamp
  @Column(name = "action_at", nullable = false, updatable = false)
  private Instant actionAt;

  @PrePersist
  void assignUuidV7IfMissing() {
    if (id == null) {
      id = UuidV7Generator.generate();
    }
  }
}
