package com.medical.medcore.platform.service;

import com.medical.medcore.platform.domain.Plan;
import com.medical.medcore.platform.domain.Subscription;
import com.medical.medcore.platform.dto.CreateSubscriptionRequest;
import com.medical.medcore.platform.dto.SubscriptionResponse;
import com.medical.medcore.platform.dto.UpdateSubscriptionRequest;
import com.medical.medcore.platform.repository.PlanRepository;
import com.medical.medcore.platform.repository.SubscriptionRepository;
import com.medical.medcore.platform.repository.SubscriptionStatusRepository;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private static final long NO_EXCLUSION = -1L;
    private static final String NOT_FOUND = "Suscripción no encontrada";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionStatusRepository statusRepository;
    private final PlanRepository planRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public SubscriptionResponse create(CreateSubscriptionRequest request) {
        if (!tenantRepository.existsById(request.tenantId())) {
            throw new NotFoundException("Organización no encontrada");
        }
        Plan plan = requireActivePlan(request.planId());
        String status = normalizeStatus(request.status());
        validateDates(request.startDate(), request.endDate());
        assertSingleActive(request.tenantId(), status, NO_EXCLUSION);
        return SubscriptionResponse.from(subscriptionRepository.save(Subscription.builder()
                .tenantId(request.tenantId())
                .plan(plan)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(status)
                .build()));
    }

    @Transactional
    public SubscriptionResponse update(Long id, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        String status = normalizeStatus(request.status());
        validateDates(request.startDate(), request.endDate());
        assertSingleActive(subscription.getTenantId(), status, subscription.getId());
        boolean planChanged = !subscription.getPlan().getId().equals(request.planId());
        if (planChanged) {
            subscription.setPlan(requireActivePlan(request.planId()));
        }
        subscription.setStartDate(request.startDate());
        subscription.setEndDate(request.endDate());
        subscription.setStatus(status);
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    public SubscriptionResponse findById(Long id) {
        return SubscriptionResponse.from(loadVisible(id));
    }

    public List<SubscriptionResponse> findAll(Long tenantId) {
        List<Subscription> subscriptions;
        if (!SecurityUtils.isSuperAdmin()) {
            subscriptions = subscriptionRepository.findByTenantIdOrderByStartDateDescIdDesc(TenantContext.requireTenantId());
        } else if (tenantId != null) {
            subscriptions = subscriptionRepository.findByTenantIdOrderByStartDateDescIdDesc(tenantId);
        } else {
            subscriptions = subscriptionRepository.findAllByOrderByTenantIdAscStartDateDescIdDesc();
        }
        return subscriptions.stream().map(SubscriptionResponse::from).toList();
    }

    @Transactional
    public void cancel(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
        subscription.setStatus(Subscription.STATUS_CANCELLED);
        if (subscription.getEndDate() == null || subscription.getEndDate().isAfter(LocalDate.now())) {
            subscription.setEndDate(LocalDate.now());
        }
        subscriptionRepository.save(subscription);
    }

    private Subscription loadVisible(Long id) {
        if (SecurityUtils.isSuperAdmin()) {
            return subscriptionRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        }
        return subscriptionRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private Plan requireActivePlan(Long planId) {
        return planRepository.findById(planId)
                .filter(plan -> !Boolean.FALSE.equals(plan.getIsActive()))
                .orElseThrow(() -> new NotFoundException("Plan no encontrado o inactivo"));
    }

    private String normalizeStatus(String rawStatus) {
        String status = rawStatus.trim().toUpperCase();
        if (!statusRepository.existsByCode(status)) {
            throw new BadRequestException("Estado de suscripción inválido");
        }
        return status;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    private void assertSingleActive(Long tenantId, String status, Long excludeId) {
        boolean conflict = Subscription.STATUS_ACTIVE.equals(status)
                && subscriptionRepository.existsByTenantIdAndStatusAndIdNot(tenantId, status, excludeId);
        if (conflict) {
            throw new ConflictException("ACTIVE_SUBSCRIPTION_EXISTS",
                    "La organización ya tiene una suscripción activa. Finalízala antes de activar otra");
        }
    }
}
