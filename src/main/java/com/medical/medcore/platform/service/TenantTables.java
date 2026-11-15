package com.medical.medcore.platform.service;

import java.util.List;
import java.util.stream.Stream;

public final class TenantTables {

    public static final String AUDIT_LOG = "audit_log";

    public static final List<String> PARENTS_FIRST = List.of(
            "tenant_modules",
            "tenant_process_config",
            "tenant_specialties",
            "tenant_appointment_types",
            "tenant_document_types",
            "tenant_logos",
            "subscriptions",
            "persons",
            "person_documents",
            "branches",
            "users",
            "user_roles",
            "staff_branches",
            "refresh_tokens",
            "password_reset_tokens",
            "email_verifications",
            "doctors",
            "doctor_specialties",
            "doctor_branches",
            "doctor_schedules",
            "schedule_exceptions",
            "branch_closures",
            "note_templates",
            "patients",
            "patient_allergies",
            "patient_conditions",
            "patient_family_history",
            "patient_habits",
            "patient_surgical_history",
            "appointments",
            "appointment_flow_history",
            "appointment_reschedules",
            "payments",
            "triage",
            "medical_records",
            "medical_entries",
            "medical_entry_diagnoses",
            "medical_procedures",
            "medical_files",
            "medical_certificates",
            "prescriptions",
            "medical_orders",
            "medical_order_results");

    private TenantTables() {
    }

    public static List<String> forExport() {
        return Stream.concat(PARENTS_FIRST.stream(), Stream.of(AUDIT_LOG)).toList();
    }

    public static List<String> forDeletion() {
        return Stream.concat(PARENTS_FIRST.reversed().stream(), Stream.of(AUDIT_LOG)).toList();
    }
}
