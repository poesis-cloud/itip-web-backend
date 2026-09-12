package cloud.poesis.itip.web.backend.auth.model;

import cloud.poesis.itip.web.backend.auth.entity.CapabilityOperation;
import cloud.poesis.itip.web.backend.auth.entity.CapabilityResourceOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AuthorizationCheck(
    @NotNull CapabilityResourceOrigin origin,
    @NotBlank String resource,
    @NotNull CapabilityOperation operation,
    UUID resourceId) {}
