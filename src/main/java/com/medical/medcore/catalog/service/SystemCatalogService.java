package com.medical.medcore.catalog.service;

import com.medical.medcore.appointment.domain.AppointmentStatusCode;
import com.medical.medcore.catalog.domain.AppointmentStatus;
import com.medical.medcore.catalog.dto.PlanRequest;
import com.medical.medcore.catalog.dto.PlanResponse;
import com.medical.medcore.catalog.dto.SystemCatalogRequest;
import com.medical.medcore.catalog.dto.SystemCatalogResponse;
import com.medical.medcore.catalog.repository.AppointmentStatusRepository;
import com.medical.medcore.platform.domain.Plan;
import com.medical.medcore.platform.domain.Subscription;
import com.medical.medcore.platform.domain.SubscriptionStatus;
import com.medical.medcore.platform.repository.PlanRepository;
import com.medical.medcore.platform.repository.SubscriptionRepository;
import com.medical.medcore.platform.repository.SubscriptionStatusRepository;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemCatalogService {

    private static final String CODE_TAKEN = "CODE_ALREADY_EXISTS";
    private static final String CODE_SYSTEM_VALUE = "SYSTEM_CATALOG_VALUE";
    private static final String CODE_IN_USE = "CATALOG_VALUE_IN_USE";
    private static final Set<String> SYSTEM_APPOINTMENT_STATUSES = Arrays.stream(AppointmentStatusCode.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> SYSTEM_SUBSCRIPTION_STATUSES =
            Set.of(Subscription.STATUS_ACTIVE, Subscription.STATUS_CANCELLED);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionStatusRepository subscriptionStatusRepository;
    private final AppointmentStatusRepository appointmentStatusRepository;

    public List<PlanResponse> listPlans(boolean includeInactive) {
        return planRepository.findAll().stream()
                .filter(plan -> includeInactive || !Boolean.FALSE.equals(plan.getIsActive()))
                .sorted(Comparator.comparing(Plan::getId))
                .map(PlanResponse::from)
                .toList();
    }

    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        String code = normalize(request.code());
        if (planRepository.existsByCode(code)) {
            throw new ConflictException(CODE_TAKEN, "Ya existe un plan con ese código");
        }
        Plan plan = new Plan();
        plan.setCode(code);
        applyPlan(plan, request);
        return PlanResponse.from(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse updatePlan(Long id, PlanRequest request) {
        Plan plan = loadPlan(id);
        String code = normalize(request.code());
        if (!code.equals(plan.getCode()) && planRepository.existsByCode(code)) {
            throw new ConflictException(CODE_TAKEN, "Ya existe un plan con ese código");
        }
        plan.setCode(code);
        applyPlan(plan, request);
        return PlanResponse.from(planRepository.save(plan));
    }

    @Transactional
    public void deactivatePlan(Long id) {
        Plan plan = loadPlan(id);
        plan.setIsActive(false);
        planRepository.save(plan);
    }

    public List<SystemCatalogResponse> listAppointmentStatuses() {
        return appointmentStatusRepository.findAll().stream()
                .sorted(Comparator.comparing(AppointmentStatus::getId))
                .map(SystemCatalogService::toResponse)
                .toList();
    }

    @Transactional
    public SystemCatalogResponse createAppointmentStatus(SystemCatalogRequest request) {
        String code = normalize(request.code());
        if (appointmentStatusRepository.existsByCode(code)) {
            throw new ConflictException(CODE_TAKEN, "Ya existe un estado de cita con ese código");
        }
        AppointmentStatus status = new AppointmentStatus();
        status.setCode(code);
        status.setName(displayName(request, code));
        status.setIsActive(!Boolean.FALSE.equals(request.isActive()));
        return toResponse(appointmentStatusRepository.save(status));
    }

    @Transactional
    public SystemCatalogResponse updateAppointmentStatus(Long id, SystemCatalogRequest request) {
        AppointmentStatus status = loadAppointmentStatus(id);
        String code = normalize(request.code());
        boolean systemValue = SYSTEM_APPOINTMENT_STATUSES.contains(status.getCode());
        if (systemValue && (!code.equals(status.getCode()) || Boolean.FALSE.equals(request.isActive()))) {
            throw new ConflictException(CODE_SYSTEM_VALUE,
                    "Los estados de cita del sistema solo permiten cambiar su nombre");
        }
        if (!code.equals(status.getCode()) && appointmentStatusRepository.existsByCode(code)) {
            throw new ConflictException(CODE_TAKEN, "Ya existe un estado de cita con ese código");
        }
        status.setCode(code);
        status.setName(displayName(request, code));
        if (request.isActive() != null) {
            status.setIsActive(request.isActive());
        }
        return toResponse(appointmentStatusRepository.save(status));
    }

    @Transactional
    public void deleteAppointmentStatus(Long id) {
        AppointmentStatus status = loadAppointmentStatus(id);
        if (SYSTEM_APPOINTMENT_STATUSES.contains(status.getCode())) {
            throw new ConflictException(CODE_SYSTEM_VALUE, "Los estados de cita del sistema no se pueden eliminar");
        }
        appointmentStatusRepository.delete(status);
    }

    public List<SystemCatalogResponse> listSubscriptionStatuses() {
        return subscriptionStatusRepository.findAll().stream()
                .sorted(Comparator.comparing(SubscriptionStatus::getId))
                .map(SystemCatalogService::toResponse)
                .toList();
    }

    @Transactional
    public SystemCatalogResponse createSubscriptionStatus(SystemCatalogRequest request) {
        String code = normalize(request.code());
        if (subscriptionStatusRepository.existsByCode(code)) {
            throw new ConflictException(CODE_TAKEN, "Ya existe un estado de suscripción con ese código");
        }
        SubscriptionStatus status = new SubscriptionStatus();
        status.setCode(code);
        status.setName(displayName(request, code));
        status.setIsActive(!Boolean.FALSE.equals(request.isActive()));
        return toResponse(subscriptionStatusRepository.save(status));
    }

    @Transactional
    public SystemCatalogResponse updateSubscriptionStatus(Long id, SystemCatalogRequest request) {
        SubscriptionStatus status = loadSubscriptionStatus(id);
        String code = normalize(request.code());
        boolean codeChanged = !code.equals(status.getCode());
        if (codeChanged) {
            assertSubscriptionStatusFree(status);
            if (subscriptionStatusRepository.existsByCode(code)) {
                throw new ConflictException(CODE_TAKEN, "Ya existe un estado de suscripción con ese código");
            }
        }
        status.setCode(code);
        status.setName(displayName(request, code));
        if (request.isActive() != null) {
            status.setIsActive(request.isActive());
        }
        return toResponse(subscriptionStatusRepository.save(status));
    }

    @Transactional
    public void deleteSubscriptionStatus(Long id) {
        SubscriptionStatus status = loadSubscriptionStatus(id);
        assertSubscriptionStatusFree(status);
        subscriptionStatusRepository.delete(status);
    }

    private void assertSubscriptionStatusFree(SubscriptionStatus status) {
        if (SYSTEM_SUBSCRIPTION_STATUSES.contains(status.getCode())) {
            throw new ConflictException(CODE_SYSTEM_VALUE, "Este estado es usado por el sistema y no se puede modificar");
        }
        if (subscriptionRepository.countByStatus(status.getCode()) > 0) {
            throw new ConflictException(CODE_IN_USE, "Hay suscripciones que usan este estado");
        }
    }

    private void applyPlan(Plan plan, PlanRequest request) {
        plan.setName(request.name().trim());
        plan.setPrice(request.price());
        plan.setMaxUsers(request.maxUsers());
        plan.setMaxBranches(request.maxBranches());
        plan.setIsActive(!Boolean.FALSE.equals(request.isActive()));
    }

    private Plan loadPlan(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new NotFoundException("Plan no encontrado"));
    }

    private AppointmentStatus loadAppointmentStatus(Long id) {
        return appointmentStatusRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Estado de cita no encontrado"));
    }

    private SubscriptionStatus loadSubscriptionStatus(Long id) {
        return subscriptionStatusRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Estado de suscripción no encontrado"));
    }

    private String normalize(String code) {
        return code.trim().toUpperCase();
    }

    private String displayName(SystemCatalogRequest request, String code) {
        return request.name() == null || request.name().isBlank() ? code : request.name().trim();
    }

    private static SystemCatalogResponse toResponse(AppointmentStatus status) {
        return new SystemCatalogResponse(status.getId(), status.getCode(), status.getName(),
                !Boolean.FALSE.equals(status.getIsActive()), status.getCreatedAt(), status.getUpdatedAt());
    }

    private static SystemCatalogResponse toResponse(SubscriptionStatus status) {
        return new SystemCatalogResponse(status.getId(), status.getCode(),
                status.getName() != null ? status.getName() : status.getCode(),
                !Boolean.FALSE.equals(status.getIsActive()), status.getCreatedAt(), status.getUpdatedAt());
    }
}
