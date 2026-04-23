package com.medical.medcore.mapper;

import com.medical.medcore.entity.User;
import com.medical.medcore.entity.Role;
import com.medical.medcore.dto.response.UserResponse;
import com.medical.medcore.dto.response.PersonResponse;

import java.util.List;

public class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {

        PersonResponse person = new PersonResponse(
                user.getPerson().getFirstName(),
                user.getPerson().getLastName(),
                user.getPerson().getPhone()
        );

        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getCode)
                .toList();

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getIsActive(),
                person,
                roles
        );
    }
}