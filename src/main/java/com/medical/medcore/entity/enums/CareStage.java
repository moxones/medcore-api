package com.medical.medcore.entity.enums;

public enum CareStage {
    BOOKED,
    IN_RECEPTION,
    IN_TRIAGE,
    READY,
    IN_CONSULTATION,
    ATTENDED;

    public static CareStage resolve(AppointmentFlowStatus flowStatus, boolean triageEnabled, boolean triageCompleted) {
        AppointmentFlowStatus status = flowStatus != null ? flowStatus : AppointmentFlowStatus.SCHEDULED;
        return switch (status) {
            case IN_PROCESS -> IN_CONSULTATION;
            case PENDING_PAYMENT, COMPLETED -> ATTENDED;
            case WAITING, CALLED -> (!triageEnabled || triageCompleted) ? READY : IN_RECEPTION;
            case SCHEDULED -> BOOKED;
        };
    }
}
