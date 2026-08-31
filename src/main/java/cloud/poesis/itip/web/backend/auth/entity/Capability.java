package cloud.poesis.itip.web.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
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
@Table(
    name = "capability",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_capability_identity",
            columnNames = {"resource_origin", "resource", "operation"}))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Capability {

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_origin", nullable = false)
  private PrivilegeResourceOrigin resourceOrigin;

  @Column(name = "resource", nullable = false)
  private String resource;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation", nullable = false)
  private PrivilegeAction operation;

  @Default
  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Default
  @Column(name = "system_managed", nullable = false)
  private boolean systemManaged = false;

  @Default
  @ManyToMany
  @JoinTable(
      name = "capability_policy",
      joinColumns = @JoinColumn(name = "capability_id"),
      inverseJoinColumns = @JoinColumn(name = "policy_id"))
  private java.util.Set<Policy> policies = new HashSet<>();

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
}
