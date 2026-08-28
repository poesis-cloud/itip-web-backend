package cloud.poesis.itip.web.backend.auth.model;

import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import java.util.UUID;

public record AuthorizationDecision(
    PrivilegeResourceOrigin origin,
    String resource,
    PrivilegeAction action,
    UUID resourceId,
    boolean allowed) {}
