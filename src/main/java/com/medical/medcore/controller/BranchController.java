package com.medical.medcore.controller;

import com.medical.medcore.entity.Branch;
import com.medical.medcore.service.branch.BranchService;
import com.medical.medcore.types.ApiResponse;
import com.medical.medcore.types.PageableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageableResponse<Branch>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, branchService.findAll(page, size), "Sucursales"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Branch>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, branchService.findById(id), "Sucursal"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Branch>> create(@RequestBody Branch branch) {
        return ResponseEntity.ok(new ApiResponse<>(true, branchService.create(branch), "Sucursal creada"));
    }
}
