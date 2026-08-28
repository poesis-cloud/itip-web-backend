package cloud.poesis.itip.web.backend.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
    name = "privilege",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_privilege_identity",
            columnNames = {"resource_origin", "resource", "action", "effect"}))
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

  @Enumerated(EnumType.STRING)
  @Column(name = "effect", nullable = false)
  private PrivilegeEffect effect;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_origin", nullable = false)
  private PrivilegeResourceOrigin resourceOrigin;

  // Bare resource name, without any origin prefix.
  @Column(name = "resource", nullable = false)
  private String resource;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false)
  private PrivilegeAction action;

  @Column(name = "condition_expression", columnDefinition = "TEXT")
  private String conditionExpression;

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
}
