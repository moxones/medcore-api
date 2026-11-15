package com.medical.medcore.dashboard.controller;

import com.medical.medcore.dashboard.dto.DashboardSummaryResponse;
import com.medical.medcore.dashboard.dto.DoctorDashboardSummaryResponse;
import com.medical.medcore.dashboard.dto.DoctorProductivityResponse;
import com.medical.medcore.dashboard.dto.ReceptionCashResponse;
import com.medical.medcore.dashboard.service.ReceptionCashService;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.shared.security.annotation.RequireModule;
import com.medical.medcore.shared.security.annotation.RequireReception;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import com.medical.medcore.dashboard.service.DashboardService;
import com.medical.medcore.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@RequireStaff
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReceptionCashService receptionCashService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, dashboardService.getSummaryMetrics(), "Métricas del dashboard principal")
        );
    }

    @GetMapping("/productivity")
    public ResponseEntity<ApiResponse<List<DoctorProductivityResponse>>> getDoctorProductivity() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, dashboardService.getDoctorProductivity(), "Productividad mensual por profesional")
        );
    }

    @GetMapping("/doctor-summary")
    public ResponseEntity<ApiResponse<DoctorDashboardSummaryResponse>> getDoctorSummary() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, dashboardService.getDoctorSummary(), "Resumen del dashboard del profesional")
        );
    }

    @RequireReception
    @RequireModule(ClinicModule.BILLING)
    @GetMapping("/reception-cash")
    public ResponseEntity<ApiResponse<ReceptionCashResponse>> getReceptionCash(
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, receptionCashService.today(branchId), "Caja del día de recepción")
        );
    }
}
