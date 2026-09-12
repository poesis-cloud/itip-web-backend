package cloud.poesis.itip.web.backend.auth.entity;

/** Lifecycle status of a {@link Capability}. Only {@link #ACTIVE} authorizes. */
public enum CapabilityStatus {
  ACTIVE,
  DISABLED,
  DEPRECATED
}
