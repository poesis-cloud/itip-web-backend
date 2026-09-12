package cloud.poesis.itip.web.backend.auth.model;

import cloud.poesis.itip.web.backend.auth.entity.CapabilityOperation;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import java.util.UUID;

public record AuthorizationDecision(
    CapabilityResourceOrigin origin,
    String resource,
    CapabilityOperation operation,
    UUID resourceId,
    boolean allowed) {}
