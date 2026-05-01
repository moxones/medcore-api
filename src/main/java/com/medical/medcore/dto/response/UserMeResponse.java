package com.medical.medcore.dto.response;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMeResponse {

    private Long userId;
    private String email;
    private Set<String> roles;

    private String firstName;
    private String lastName;

    private Long tenantId;
}