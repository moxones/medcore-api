package com.medical.medcore.identity.service;

import com.medical.medcore.branch.repository.StaffBranchRepository;
import com.medical.medcore.doctor.repository.DoctorBranchRepository;
import com.medical.medcore.identity.domain.Role;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.shared.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final StaffBranchRepository staffBranchRepository;
    private final DoctorBranchRepository doctorBranchRepository;

    @Transactional
    public AuthResponse issue(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getCode).sorted().toList();
        String accessToken = jwtProvider.generateToken(user.getId(), user.getTenantId(), roles, branchIdsOf(user));
        String refreshToken = refreshTokenService.create(user.getId(), user.getTenantId());
        return new AuthResponse(accessToken, refreshToken, Boolean.TRUE.equals(user.getMustChangePassword()));
    }

    @Transactional(readOnly = true)
    public List<Long> branchIdsOf(User user) {
        Set<Long> branchIds = new LinkedHashSet<>(staffBranchRepository.findActiveBranchIdsByUserId(user.getId()));
        branchIds.addAll(doctorBranchRepository.findActiveBranchIdsByPersonId(user.getPerson().getId()));
        return new ArrayList<>(branchIds);
    }
}
