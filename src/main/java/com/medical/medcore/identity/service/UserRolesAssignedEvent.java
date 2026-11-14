package com.medical.medcore.identity.service;

import java.util.Set;

public record UserRolesAssignedEvent(Long tenantId, Long personId, Set<String> roleCodes) {
}
