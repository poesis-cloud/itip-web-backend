package cloud.poesis.itip.web.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "privilege")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Privilege {

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "capability_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_privilege_capability"))
  private Capability capability;

  @Default
  @ManyToMany
  @JoinTable(
      name = "privilege_policy",
      joinColumns = @JoinColumn(name = "privilege_id"),
      inverseJoinColumns = @JoinColumn(name = "policy_id"))
  private Set<Policy> policies = new HashSet<>();

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_by", nullable = false)
  private String createdBy;

  @Column(name = "updated_by", nullable = false)
  private String updatedBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Default
  @OneToMany(mappedBy = "privilege", fetch = FetchType.LAZY)
  private Set<RolePrivilegeAssignment> rolePrivilegeAssignments = new HashSet<>();

  @PrePersist
  void assignUuidV7IfMissing() {
    if (id == null) {
      id = UuidV7Generator.generate();
    }
  }
}
