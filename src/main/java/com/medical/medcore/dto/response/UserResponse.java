package com.medical.medcore.dto.response;

import java.util.List;

public class UserResponse {

    private Long id;
    private String email;
    private Boolean isActive;
    private PersonResponse person;
    private List<String> roles;
    private List<Long> roleIds;
    private Long tenantId;

    public UserResponse(Long id,
                        String email,
                        Boolean isActive,
                        PersonResponse person,
                        List<String> roles,
                        List<Long> roleIds,
                        Long tenantId) {
        this.id = id;
        this.email = email;
        this.isActive = isActive;
        this.person = person;
        this.roles = roles;
        this.roleIds = roleIds;
        this.tenantId = tenantId;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Boolean getIsActive() { return isActive; }
    public PersonResponse getPerson() { return person; }
    public List<String> getRoles() { return roles; }
    public List<Long> getRoleIds() { return roleIds; }
    public Long getTenantId() { return tenantId; }
}