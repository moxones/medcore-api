package com.medical.medcore.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserRequest {

    private String email;
    private PersonRequest person;
    private List<Long> roleIds;
}