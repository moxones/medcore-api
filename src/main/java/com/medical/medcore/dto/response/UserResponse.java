package com.medical.medcore.dto.response;

import java.util.List;

public class UserResponse {

    private Long id;
    private String email;
    private Boolean isActive;
    private PersonResponse person;
    private List<String> roles;

    public UserResponse(Long id,
                        String email,
                        Boolean isActive,
                        PersonResponse person,
                        List<String> roles) {
        this.id = id;
        this.email = email;
        this.isActive = isActive;
        this.person = person;
        this.roles = roles;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Boolean getIsActive() { return isActive; }
    public PersonResponse getPerson() { return person; }
    public List<String> getRoles() { return roles; }
}