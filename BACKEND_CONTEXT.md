# Backend Context — Multi-Tenant Medical Appointments

> Single source of truth for the data model. Read this before generating/modifying backend code.
> DB: PostgreSQL. All tables in schema `public`. PK = `id bigserial`.

## 1. Domain

SaaS platform. A **super admin** registers **tenants** (clinics, hospitals, medical centers, single-doctor offices). Each tenant is an isolated organization with its own branches, staff, patients, schedules and appointments. Tenants subscribe to a **plan**.

## 2. Conventions (apply to ALL generated code)

- **Tenant isolation**: every query on a tenant-scoped table MUST filter by `tenant_id`. Never leak rows across tenants. Indirectly-scoped tables reach `tenant_id` via their parent (see §4 scope column).
- **Audit columns** (where present): `created_at`, `created_by`, `updated_at`, `updated_by`. Set `created_by`/`updated_by` to the acting user id.
- **Soft delete / enable flag**: `is_active boolean`. Default filter = `is_active = true`. Do not hard-delete catalog rows.
- **Timestamps**: `timestamp without time zone`, server default `CURRENT_TIMESTAMP`/`now()`.
- **Catalog vs tenant catalog**: global catalogs (`appointment_types`, `document_types`, `specialties`) are linked to a tenant through `tenant_*` join tables that may override values (e.g. `tenant_appointment_types.duration_minutes`).
- **Codes**: catalog tables expose a unique `code` (use it for lookups, not the name).
- **Money**: `numeric(10,2)`.

## 3. Scope classes

| Scope | Meaning | Tables |
|---|---|---|
| GLOBAL | shared, super-admin managed | `plans`, `subscription_status`, `roles`, `appointment_status`, `appointment_types`, `document_types`, `specialties` |
| TENANT | has direct `tenant_id` | `tenants`, `users`, `persons`, `branches`, `doctors`, `patients`, `subscriptions`, `appointments`, `refresh_tokens`, `tenant_appointment_types`, `tenant_document_types`, `tenant_specialties` |
| CHILD | scoped via parent FK | `person_documents`→persons, `user_roles`→users, `doctor_branches`/`doctor_schedules`/`doctor_specialties`→doctors, `medical_records`→patients, `medical_entries`→medical_records, `medical_files`→medical_entries, `appointment_flow_history`/`appointment_reschedules`/`triage`/`payments`→appointments |

## 4. Entities

Format: `column type [flags]`. Flags: PK, FK→table, U=unique, NN=not null, D=has default.

### Tenancy & Billing

**tenants** (root)
`id` PK · `name` NN · `subdomain` NN U · `logo_url` · `primary_color` · `secondary_color` · `subtitle` · `status` NN D='ACTIVE' · audit
status ∈ {ACTIVE, ...} (string enum, app-controlled).

**plans** GLOBAL
`id` PK · `name` NN · `code` NN U · `price` numeric NN · `max_users` · `max_branches` · `is_active` D=true · audit

**subscription_status** GLOBAL
`id` PK · `code` U · audit

**subscriptions** TENANT
`id` PK · `tenant_id` FK→tenants(CASCADE-isolation) · `plan_id` FK→plans · `start_date` NN · `end_date` · `status` NN

### Identity & Auth

**persons** TENANT (real human; reused by users/doctors/patients)
`id` PK · `tenant_id` FK→tenants NN · `first_name` · `last_name` · `birth_date` · `gender` · `phone` · `contact_email` · `profile_completed` NN D=false · audit

**person_documents** CHILD→persons
`id` PK · `person_id` FK→persons · `document_type_id` FK→document_types · `document_number` NN · `is_primary` D=true
U(document_type_id, document_number)

**document_types** GLOBAL
`id` PK · `code` NN U · `name` NN · `is_active` NN D=true · audit

**users** TENANT (login account)
`id` PK · `tenant_id` FK→tenants NN · `email` NN · `password` NN(hash) · `person_id` FK→persons · `branch_id` FK→branches · `is_active` D=true · audit
U(tenant_id, email)

**roles** GLOBAL
`id` PK · `name` NN · `code` NN U · `is_active` D=true · audit

**user_roles** CHILD→users
`id` PK · `user_id` FK→users(CASCADE) · `role_id` FK→roles · U(user_id, role_id)

**refresh_tokens** TENANT
`id` PK · `user_id` FK→users(CASCADE) · `tenant_id` FK→tenants · `token` NN U · `expires_at` NN · `is_revoked` NN D=false

### Org Structure

**branches** TENANT (physical site)
`id` PK · `tenant_id` FK→tenants NN · `name` NN · `address` NN D='' · `ruc` · `opening_time` · `closing_time` · `appointment_duration_minutes` D=30 · `is_active` D=true

**doctors** TENANT
`id` PK · `tenant_id` FK→tenants NN · `person_id` FK→persons NN · `license_number` · `is_active` D=true · audit
U(tenant_id, person_id)

**doctor_branches** CHILD→doctors (which branches a doctor works at)
`id` PK · `doctor_id` FK→doctors(CASCADE) · `branch_id` FK→branches(CASCADE) · `is_active` NN D=true · U(doctor_id, branch_id)

**doctor_schedules** CHILD→doctors (weekly availability)
`id` PK · `doctor_id` FK→doctors(CASCADE) · `branch_id` · `doctor_branch_id` FK→doctor_branches(CASCADE) · `day_of_week` int NN [0=Mon..6=Sun] · `start_time` NN · `end_time` NN · `slot_duration_minutes` NN D=30 · `max_patients_per_slot` · `valid_from` · `valid_until` · `is_active` D=true · audit

**specialties** GLOBAL
`id` PK · `code` NN U · `name` NN · `description` · `is_active` NN D=true · audit

**doctor_specialties** CHILD→doctors
`id` PK · `doctor_id` FK→doctors(CASCADE) · `specialty_id` FK→specialties(CASCADE) · U(doctor_id, specialty_id)

### Per-Tenant Catalog Overrides

**tenant_appointment_types** TENANT · `tenant_id`+`appointment_type_id` U · `duration_minutes` (override) · `is_active`
**tenant_document_types** TENANT · `tenant_id`+`document_type_id` U · `is_active`
**tenant_specialties** TENANT · `tenant_id`+`specialty_id` U · `is_active`
**appointment_types** GLOBAL · `code` U · `name` · `duration_minutes` D=30 · `is_active`

### Appointments

**appointment_status** GLOBAL (billing/lifecycle status, FK target)
`id` PK · `code` NN U · `name` NN · `is_active` D=true · audit

**appointments** TENANT (core entity)
`id` PK · `tenant_id` FK→tenants(CASCADE) NN · `patient_id` FK→patients NN · `doctor_id` FK→doctors NN · `branch_id` FK→branches · `appointment_type_id` FK→appointment_types(SET NULL) · `status_id` FK→appointment_status(RESTRICT) NN · `scheduled_at` NN · `duration_minutes` NN D=30 · `reason` · `booking_source` · `flow_status` NN D='WAITING' · `checked_in_at` · `called_at` · `started_at` · `finished_at` · `completed_at` · audit

**flow_status** (queue/operational state — string):
`WAITING → CHECKED_IN → CALLED → IN_PROGRESS → FINISHED → COMPLETED` (set matching timestamp column on each transition). Distinct from `status_id` (administrative lifecycle).

**appointment_flow_history** CHILD→appointments · `appointment_id` FK · `flow_status` NN · `changed_at` D
**appointment_reschedules** CHILD→appointments · `appointment_id` FK(CASCADE) · `old_scheduled_at` NN · `new_scheduled_at` NN · `reason` · `created_by`
**triage** CHILD→appointments · `appointment_id` FK(CASCADE) U(1:1) · `weight` · `height` · `temperature` · `heart_rate` · `blood_pressure` · `notes` · `created_by`
**payments** CHILD→appointments · `appointment_id` FK NN · `amount` numeric NN · `status` NN · `payment_date`

### Medical Records

**patients** TENANT
`id` PK · `tenant_id` FK→tenants NN · `person_id` FK→persons NN · `medical_record_number` · `created_by` FK→users · `is_active` D=true · audit
U(tenant_id, person_id)

**medical_records** CHILD→patients · `patient_id` FK U(1:1) · `created_by`
**medical_entries** CHILD→medical_records · `medical_record_id` FK NN · `appointment_id` FK→appointments · `diagnosis` · `treatment` · `notes` · `created_by`
**medical_files** CHILD→medical_entries · `medical_entry_id` FK NN · `file_url` NN · `file_type` · `uploaded_at`

## 5. Key relationship chains

- Auth: `users → person_documents` (via persons) ; `users → roles` (via user_roles)
- Staffing: `doctors → branches` (doctor_branches) → `doctor_schedules` → slot generation
- Visit: `patients + doctors + branches → appointments → {triage, payments, medical_entries, flow_history, reschedules}`
- Clinical history: `patients → medical_records → medical_entries → medical_files`
- A `person` (1) backs at most one `doctor` and/or one `patient` per tenant.

## 6. Indexes present (don't recreate)

tenant_id on: branches, doctors, patients, persons, users, refresh_tokens(+user), tenant_* tables.
appointments: doctor_id, patient_id. doctor_branches: doctor_id, branch_id. doctor_specialties: doctor_id. person_documents: person_id. medical_records: patient_id. triage: appointment_id. appointment_flow_history: appointment_id.

## 7. Endpoint generation rules (for the upcoming missing-endpoint pass)

When asked to add an endpoint:
1. Resolve scope from §3. TENANT/CHILD endpoints derive `tenant_id` from the authenticated user — never from request body.
2. List/read: filter `tenant_id` + `is_active=true` (if column exists). Paginate.
3. Create: set audit `created_by` + `created_at`; validate FKs belong to same tenant; respect UNIQUE constraints (return 409 on conflict).
4. Update: set `updated_by`/`updated_at`; never allow changing `tenant_id`.
5. Delete: soft (`is_active=false`) for catalog/TENANT rows; hard delete only where FK is `ON DELETE CASCADE` and it's a true child (e.g. triage, reschedules).
6. Appointment transitions: update `flow_status`, stamp the matching `*_at` column, insert `appointment_flow_history` row in the same transaction.
7. Reschedule: insert `appointment_reschedules`, then update `appointments.scheduled_at`.

## 8. Open gaps / ambiguities (confirm before relying on)

- `appointment_status.code` values not enumerated here.
- `branches` has no audit `created_by`/`updated_*` and no FK index naming for tenant beyond listed.
- `subscription_status` table exists but `subscriptions.status` is a free string, not an FK → likely intended to become FK.
- `appointments.booking_source` values undefined (e.g. WEB, PHONE, WALK_IN).
- `tenants.status` enum values undefined.
