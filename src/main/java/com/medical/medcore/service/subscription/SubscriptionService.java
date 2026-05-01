package com.medical.medcore.service.subscription;

import com.medical.medcore.dto.request.CreateSubscriptionRequest;
import com.medical.medcore.dto.request.UpdateSubscriptionRequest;
import com.medical.medcore.dto.response.SubscriptionResponse;
import java.util.List;

public interface SubscriptionService {
    SubscriptionResponse createSubscription(CreateSubscriptionRequest request);
    SubscriptionResponse updateSubscription(Long id, UpdateSubscriptionRequest request);
    SubscriptionResponse getSubscriptionById(Long id);
    List<SubscriptionResponse> getAllSubscriptions();
    void deleteSubscription(Long id);
}
