package com.medical.medcore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DoctorPatientResponse {
    private Long patientId;
    private String fullName;
    private String initials;
    private String gender;
    private String birthDate;
    private Integer age;
    private String phone;
    private String email;
    private String bloodType;
    private Integer allergyCount;
    private Integer conditionCount;
    private Long totalVisits;
    private LocalDateTime lastVisitAt;
    private String lastReason;
    private LocalDateTime nextAppointmentAt;
}
