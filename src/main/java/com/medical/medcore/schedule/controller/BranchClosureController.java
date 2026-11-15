package com.medical.medcore.schedule.controller;

import com.medical.medcore.schedule.dto.BranchClosureRequest;
import com.medical.medcore.schedule.dto.BranchClosureResponse;
import com.medical.medcore.schedule.dto.ClosureOutcomeResponse;
import com.medical.medcore.schedule.dto.HolidayImportRequest;
import com.medical.medcore.schedule.dto.HolidayImportResponse;
import com.medical.medcore.schedule.dto.ImpactPreviewResponse;
import com.medical.medcore.schedule.service.BranchClosureService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/branch-closures")
@RequiredArgsConstructor
public class BranchClosureController {

    private final BranchClosureService service;

    @RequireStaff
    @GetMapping
    public ApiResponse<List<BranchClosureResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long branchId) {
        return ApiResponse.ok(service.list(from, to, branchId), "Feriados y cierres");
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/preview")
    public ApiResponse<ImpactPreviewResponse> preview(@Valid @RequestBody BranchClosureRequest request) {
        return ApiResponse.ok(service.preview(request), "Impacto del cierre");
    }

    @RequireAdminOrSuperAdmin
    @PostMapping
    public ApiResponse<ClosureOutcomeResponse> create(@Valid @RequestBody BranchClosureRequest request) {
        return ApiResponse.ok(service.create(request), "Cierre registrado");
    }

    @RequireAdminOrSuperAdmin
    @DeleteMapping("/{id}")
    public ApiResponse<ClosureOutcomeResponse> delete(@PathVariable Long id) {
        return ApiResponse.ok(service.delete(id), "Cierre eliminado");
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/national-holidays")
    public ApiResponse<HolidayImportResponse> importHolidays(@Valid @RequestBody HolidayImportRequest request) {
        return ApiResponse.ok(service.importNationalHolidays(request.year()), "Feriados nacionales cargados");
    }
}
