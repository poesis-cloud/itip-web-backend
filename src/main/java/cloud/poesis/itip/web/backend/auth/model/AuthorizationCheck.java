package cloud.poesis.itip.web.backend.auth.model;

import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AuthorizationCheck(
    @NotNull PrivilegeResourceOrigin origin,
    @NotBlank String resource,
    @NotNull PrivilegeAction action,
    UUID resourceId) {}
