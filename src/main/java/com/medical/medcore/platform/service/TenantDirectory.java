package com.medical.medcore.platform.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.medical.medcore.platform.domain.SubscriptionAccessPolicy;
import com.medical.medcore.platform.domain.Tenant;
import com.medical.medcore.platform.domain.TenantStatus;
import com.medical.medcore.platform.repository.SubscriptionRepository;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.tenancy.AfterTransaction;
import com.medical.medcore.shared.tenancy.TenantContext;
import com.medical.medcore.shared.tenancy.TenantHostResolver.TenantLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class TenantDirectory {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionAccessPolicy accessPolicy;
    private final boolean subscriptionsEnforced;
    private final String platformSubdomain;
    private final Cache<TenantLookup, Optional<TenantSnapshot>> cache;

    public TenantDirectory(TenantRepository tenantRepository,
                           SubscriptionRepository subscriptionRepository,
                           @Value("${app.subscriptions.enforce:true}") boolean subscriptionsEnforced,
                           @Value("${app.subscriptions.grace-days:7}") int graceDays,
                           @Value("${app.bootstrap.platform-subdomain:admin}") String platformSubdomain,
                           @Value("${app.tenancy.cache-seconds:30}") long cacheSeconds) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.accessPolicy = new SubscriptionAccessPolicy(graceDays);
        this.subscriptionsEnforced = subscriptionsEnforced;
        this.platformSubdomain = platformSubdomain;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(cacheSeconds))
                .maximumSize(10_000)
                .build();
    }

    public Optional<TenantSnapshot> find(TenantLookup lookup) {
        return cache.get(lookup, key -> TenantContext.runAsSystem(() -> load(key)));
    }

    public Optional<TenantSnapshot> platform() {
        return find(new TenantLookup(TenantLookup.Kind.SUBDOMAIN, platformSubdomain));
    }

    public boolean isPlatform(Long tenantId) {
        return tenantId != null && platform().map(snapshot -> snapshot.id().equals(tenantId)).orElse(false);
    }

    public boolean isPlatformSubdomain(String subdomain) {
        return platformSubdomain.equals(subdomain);
    }

    public void evict(Long tenantId) {
        AfterTransaction.nowAndAfterCompletion(() -> cache.asMap().entrySet().removeIf(entry ->
                entry.getValue().map(snapshot -> snapshot.id().equals(tenantId)).orElse(true)));
    }

    private Optional<TenantSnapshot> load(TenantLookup lookup) {
        Optional<Tenant> tenant = switch (lookup.kind()) {
            case SUBDOMAIN -> tenantRepository.findBySubdomain(lookup.value());
            case CUSTOM_DOMAIN -> tenantRepository.findByCustomDomain(lookup.value());
        };
        return tenant.map(this::snapshot);
    }

    private TenantSnapshot snapshot(Tenant tenant) {
        boolean platform = isPlatformSubdomain(tenant.getSubdomain());
        boolean blocked = subscriptionsEnforced && !platform && accessPolicy.blocks(
                subscriptionRepository.findFirstByTenantIdOrderByStartDateDescIdDesc(tenant.getId()),
                LocalDate.now(ZoneId.of(tenant.getTimezone())));
        return new TenantSnapshot(tenant.getId(), tenant.getSubdomain(),
                tenant.getStatus() == TenantStatus.ACTIVE, platform, blocked);
    }

    public record TenantSnapshot(Long id, String subdomain, boolean active, boolean platform,
                                 boolean subscriptionBlocked) {
    }
}
