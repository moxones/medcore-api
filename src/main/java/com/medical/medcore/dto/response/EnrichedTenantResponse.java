package com.medical.medcore.dto.response;

import com.medical.medcore.entity.enums.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedTenantResponse {
    private Long id;
    private String subdomain;
    private String name;
    private TenantStatus status;
    private LocalDateTime createdAt;
    
    private String planName;
    private String subscriptionStatus;
    private LocalDate subscriptionEndDate;
    
    private int currentUsers;
    private int maxUsers;
    private int currentBranches;
    private int maxBranches;
    
    private boolean limitReached;
    private boolean nearExpiration;
}
