package com.medical.medcore.catalog.controller;

import com.medical.medcore.catalog.dto.Cie10Response;
import com.medical.medcore.catalog.domain.Cie10Code;
import com.medical.medcore.catalog.repository.Cie10CodeRepository;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.api.PageableResponse;
import com.medical.medcore.platform.domain.ClinicModule;
import com.medical.medcore.shared.security.annotation.RequireModule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cie10")
@RequiredArgsConstructor
@RequireStaff
@RequireModule(ClinicModule.CONSULTATION)
public class Cie10Controller {

    private final Cie10CodeRepository cie10CodeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageableResponse<Cie10Response>>> search(
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageableResponse<Cie10Response> result = PageableResponse.from(
                cie10CodeRepository.search(q, PageRequest.of(page, size)).map(this::map));
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Catálogo CIE-10"));
    }

    private Cie10Response map(Cie10Code c) {
        return new Cie10Response(c.getId(), c.getCode(), c.getDescription(), c.getCategory());
    }
}
