package com.medical.medcore.controller;

import com.medical.medcore.dto.response.report.ReportResult;
import com.medical.medcore.report.ReportFormat;
import com.medical.medcore.report.ReportQuery;
import com.medical.medcore.report.ReportService;
import com.medical.medcore.types.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<ReportResult>> getReport(
            @PathVariable String key,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) String status) {

        ReportQuery query = new ReportQuery(from, to, branchId, doctorId, specialtyId, status);
        ReportResult result = reportService.getReport(key, query);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "OK"));
    }

    @GetMapping("/{key}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String key,
            @RequestParam ReportFormat format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) String status) {

        ReportQuery query = new ReportQuery(from, to, branchId, doctorId, specialtyId, status);
        byte[] file = reportService.export(key, format, query);

        String filename = key
                + (from != null ? "-" + from : "")
                + (to != null ? "-" + to : "")
                + "." + format.extension();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(format.contentType()))
                .body(file);
    }
}
