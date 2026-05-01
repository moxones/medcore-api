package com.medical.medcore.service.subscription.impl;

import com.medical.medcore.dto.request.CreateSubscriptionRequest;
import com.medical.medcore.dto.request.UpdateSubscriptionRequest;
import com.medical.medcore.dto.response.SubscriptionResponse;
import com.medical.medcore.entity.Plan;
import com.medical.medcore.entity.Subscription;
import com.medical.medcore.repository.PlanRepository;
import com.medical.medcore.repository.SubscriptionRepository;
import com.medical.medcore.service.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    @Override
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        Subscription subscription = Subscription.builder()
                .tenantId(request.getTenantId())
                .plan(plan)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .build();

        return mapToResponse(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public SubscriptionResponse updateSubscription(Long id, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        subscription.setPlan(plan);
        subscription.setStartDate(request.getStartDate());
        subscription.setEndDate(request.getEndDate());
        subscription.setStatus(request.getStatus());

        return mapToResponse(subscriptionRepository.save(subscription));
    }

    @Override
    public SubscriptionResponse getSubscriptionById(Long id) {
        return mapToResponse(subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found")));
    }

    @Override
    public List<SubscriptionResponse> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSubscription(Long id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new RuntimeException("Subscription not found");
        }
        subscriptionRepository.deleteById(id);
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .tenantId(subscription.getTenantId())
                .plan(subscription.getPlan())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
