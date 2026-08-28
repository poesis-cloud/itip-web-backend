package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeEffect;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceType;

public record CreatePrivilegeCommand(
    String code,
    PrivilegeEffect effect,
    PrivilegeResourceType resourceType,
    String resourceTypeKey,
    PrivilegeAction action,
    String conditionExpression) {}
