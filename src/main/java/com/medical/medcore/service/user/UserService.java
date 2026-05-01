package com.medical.medcore.service.user;

import com.medical.medcore.dto.request.ChangePasswordRequest;
import com.medical.medcore.dto.request.CreateUserRequest;
import com.medical.medcore.dto.request.UpdateUserRequest;
import com.medical.medcore.dto.request.UpdateUserStatusRequest;
import com.medical.medcore.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    List<UserResponse> findAll();

    UserResponse findById(Long id);

    UserResponse update(Long id, UpdateUserRequest request);

    void updateStatus(Long id, UpdateUserStatusRequest request);

    void changePassword(Long id, ChangePasswordRequest request);
}