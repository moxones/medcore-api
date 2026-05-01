package com.medical.medcore.controller.publicapi;

import com.medical.medcore.dto.request.PublicBookingRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.service.booking.PublicBookingService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/bookings")
@RequiredArgsConstructor
public class PublicBookingController {

    private final PublicBookingService publicBookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> registerAndBook(
            @Valid @RequestBody PublicBookingRequest request) {
            
        return ResponseEntity.ok(
                new ApiResponse<>(true, publicBookingService.registerAndBook(request), "Paciente registrado y cita confirmada desde la web")
        );
    }
}
