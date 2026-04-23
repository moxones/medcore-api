package com.medical.medcore.security.authorization.service;

import com.medical.medcore.security.authorization.constants.PermissionConstants;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RolePermissionService {

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            RoleConstants.SUPER_ADMIN, List.of(
                    PermissionConstants.USER_READ,
                    PermissionConstants.USER_CREATE,
                    PermissionConstants.USER_UPDATE,
                    PermissionConstants.APPOINTMENT_CREATE,
                    PermissionConstants.APPOINTMENT_READ
            ),
            RoleConstants.DOCTOR, List.of(
                    PermissionConstants.APPOINTMENT_READ,
                    PermissionConstants.APPOINTMENT_CREATE
            )
    );

    public List<String> getPermissionsByRoles(List<String> roles) {
        return roles.stream()
                .filter(ROLE_PERMISSIONS::containsKey)
                .flatMap(r -> ROLE_PERMISSIONS.get(r).stream())
                .distinct()
                .toList();
    }
}