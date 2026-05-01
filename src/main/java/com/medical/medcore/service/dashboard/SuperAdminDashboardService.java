package com.medical.medcore.service.dashboard;

import com.medical.medcore.dto.response.EnrichedTenantResponse;
import com.medical.medcore.dto.response.GlobalDashboardSummaryResponse;
import com.medical.medcore.entity.Plan;
import com.medical.medcore.entity.Subscription;
import com.medical.medcore.entity.Tenant;
import com.medical.medcore.entity.enums.TenantStatus;
import com.medical.medcore.repository.BranchRepository;
import com.medical.medcore.repository.SubscriptionRepository;
import com.medical.medcore.repository.TenantRepository;
import com.medical.medcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SuperAdminDashboardService {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    public GlobalDashboardSummaryResponse getGlobalSummary() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long inactiveTenants = tenantRepository.countByStatus(TenantStatus.INACTIVE);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

        long tenantsCreatedThisMonth = tenantRepository.countByCreatedAtBetween(startOfThisMonth, now);
        long tenantsCreatedLastMonth = tenantRepository.countByCreatedAtBetween(startOfLastMonth, startOfThisMonth);

        long activeSubscriptions = subscriptionRepository.countByStatus("ACTIVE");
        long expiredSubscriptions = subscriptionRepository.countByStatus("EXPIRED");

        long totalUsers = userRepository.countByIsActiveTrue();
        Long totalAllowedUsers = subscriptionRepository.sumMaxUsersFromActiveSubscriptions();

        return GlobalDashboardSummaryResponse.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .inactiveTenants(inactiveTenants)
                .tenantsCreatedThisMonth(tenantsCreatedThisMonth)
                .tenantsCreatedLastMonth(tenantsCreatedLastMonth)
                .activeSubscriptions(activeSubscriptions)
                .expiredSubscriptions(expiredSubscriptions)
                .totalUsers(totalUsers)
                .totalAllowedUsers(totalAllowedUsers != null ? totalAllowedUsers : 0)
                .build();
    }

    public Page<EnrichedTenantResponse> getEnrichedTenants(Pageable pageable) {
        Page<Tenant> tenants = tenantRepository.findAll(pageable);
        
        return tenants.map(tenant -> {
            Optional<Subscription> latestSubOpt = subscriptionRepository.findFirstByTenantIdOrderByEndDateDesc(tenant.getId());
            
            String planName = "No Plan";
            String subStatus = "NONE";
            LocalDate subEndDate = null;
            int maxUsers = 0;
            int maxBranches = 0;
            
            if (latestSubOpt.isPresent()) {
                Subscription sub = latestSubOpt.get();
                Plan plan = sub.getPlan();
                if (plan != null) {
                    planName = plan.getName();
                    maxUsers = plan.getMaxUsers() != null ? plan.getMaxUsers() : 0;
                    maxBranches = plan.getMaxBranches() != null ? plan.getMaxBranches() : 0;
                }
                subStatus = sub.getStatus();
                subEndDate = sub.getEndDate();
            }

            int currentUsers = (int) userRepository.countByTenantIdAndIsActiveTrue(tenant.getId());
            int currentBranches = (int) branchRepository.countByTenantIdAndIsActiveTrue(tenant.getId());

            boolean limitReached = false;
            if ((maxUsers > 0 && currentUsers >= maxUsers) || (maxBranches > 0 && currentBranches >= maxBranches)) {
                limitReached = true;
            }

            boolean nearExpiration = false;
            if (subEndDate != null && "ACTIVE".equals(subStatus)) {
                if (subEndDate.isBefore(LocalDate.now().plusDays(15))) {
                    nearExpiration = true;
                }
            }

            return EnrichedTenantResponse.builder()
                    .id(tenant.getId())
                    .subdomain(tenant.getSubdomain())
                    .name(tenant.getName())
                    .status(tenant.getStatus())
                    .createdAt(tenant.getCreatedAt())
                    .planName(planName)
                    .subscriptionStatus(subStatus)
                    .subscriptionEndDate(subEndDate)
                    .currentUsers(currentUsers)
                    .maxUsers(maxUsers)
                    .currentBranches(currentBranches)
                    .maxBranches(maxBranches)
                    .limitReached(limitReached)
                    .nearExpiration(nearExpiration)
                    .build();
        });
    }
}
