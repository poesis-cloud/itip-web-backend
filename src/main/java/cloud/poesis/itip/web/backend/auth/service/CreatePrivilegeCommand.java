package cloud.poesis.itip.web.backend.auth.service;

import cloud.poesis.itip.web.backend.auth.entity.PrivilegeAction;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeEffect;
import cloud.poesis.itip.web.backend.auth.entity.PrivilegeResourceOrigin;

public record CreatePrivilegeCommand(
    PrivilegeEffect effect,
    PrivilegeResourceOrigin resourceOrigin,
    String resource,
    PrivilegeAction action,
    String conditionExpression) {}
