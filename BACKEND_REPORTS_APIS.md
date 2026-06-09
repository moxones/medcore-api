# BACKEND — Módulo de Reportería (MedCore)

> Documento de requerimientos para el equipo backend (Spring Boot).
> Define el contrato HTTP, el sobre estándar de respuesta, los reportes por rol,
> y los campos de base de datos necesarios.
>
> **Regla de oro de este documento:** si un campo/columna que se menciona aquí **no existe**
> en la base de datos, **asúmelo como pendiente de creación**: créalo en la entidad/DTO y
> **agrégalo también en la BD** (migración Flyway/Liquibase). Cada sección marca con
> 🆕 los campos que probablemente haya que crear.

---

## 1. Filosofía: ¿por qué el backend genera los reportes?

El frontend **no calcula** reportes. Solo pide y muestra/descarga. El backend es responsable de:

1. **Agregar** grandes volúmenes (citas, pagos, diagnósticos) con SQL indexado — nunca enviar filas crudas al navegador.
2. **Imponer el aislamiento multi-tenant** y el filtrado por rol/sucursal en el servidor. El cliente jamás define el tenant.
3. **Generar archivos** PDF/Excel con formato corporativo (logo del tenant, totales, paginación).
4. Devolver **dos formas** del mismo reporte:
   - **JSON** (sobre estándar) → para la vista interactiva en pantalla (KPIs + tablas + barras).
   - **Binario** (PDF/XLSX) → para descarga.

---

## 2. Tecnología recomendada (Spring Boot)

| Necesidad | Recomendación | Alternativa |
|---|---|---|
| **PDF** | **JasperReports** (`.jrxml`, estándar en clínicas, soporta logo/encabezados/totales) | OpenPDF + Flying Saucer (HTML/CSS → PDF con Thymeleaf) |
| **Excel (XLSX)** | **Apache POI** (`SXSSFWorkbook` para streaming en reportes grandes) | FastExcel |
| **Agregaciones** | JPQL/SQL nativo con **DTO projections** (interfaces o `record`) | jOOQ si las consultas son muy complejas |
| **Reportes pesados** | `@Async` + tabla de jobs (estado + descarga diferida) | Spring Batch / cola (RabbitMQ) |
| **Caché** | Spring Cache (`@Cacheable`) en reportes costosos con TTL corto | Redis |
| **Fechas** | `LocalDate` (rango `from`/`to` inclusivo) y `ZoneId` del tenant | — |

**Dependencias sugeridas (`pom.xml`):**
```xml
<dependency>
  <groupId>net.sf.jasperreports</groupId>
  <artifactId>jasperreports</artifactId>
  <version>6.21.3</version>
</dependency>
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi-ooxml</artifactId>
  <version>5.3.0</version>
</dependency>
```

---

## 3. Contrato HTTP general

Cada reporte se identifica por una **`key`** estable (ver catálogo en §5). Hay dos endpoints:

### 3.1. Datos (JSON, para la vista interactiva)

```
GET /api/reports/{key}
```

**Query params comunes** (todos opcionales salvo lo indicado por reporte):

| Param | Tipo | Descripción |
|---|---|---|
| `from` | `string` (ISO `yyyy-MM-dd`) | Inicio del rango (inclusive). |
| `to` | `string` (ISO `yyyy-MM-dd`) | Fin del rango (inclusive). |
| `branchId` | `number` | Filtra por sucursal. Si se omite → **todas** las sucursales del tenant a las que el usuario tiene acceso. |
| `doctorId` | `number` | Filtra por médico (reportes que lo soporten). |
| `specialtyId` | `number` | Filtra por especialidad. |
| `status` | `string` | Filtra por estado (p. ej. estado de cita o de suscripción). |

**Respuesta:** `ApiResponse<ReportResult>` (sobre estándar del proyecto):

```jsonc
{
  "success": true,
  "message": "OK",
  "data": { /* ReportResult, ver §4 */ }
}
```

### 3.2. Exportación (binario, para descarga)

```
GET /api/reports/{key}/export?format=PDF|XLSX&from=...&to=...&branchId=...
```

- Mismos query params que el endpoint de datos + `format` (`PDF` | `XLSX`).
- **Respuesta:** el archivo binario.
  - `Content-Type`: `application/pdf` o `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
  - `Content-Disposition: attachment; filename="<key>-<from>-<to>.<ext>"`.
- El frontend descarga el `Blob` directamente (ya implementado).

### 3.3. Seguridad y multi-tenant (obligatorio)

- Todas las rutas `/api/reports/**` son **protegidas** (requieren `Authorization: Bearer`).
- El backend **deriva el tenant** del token/subdominio, **nunca** del cliente.
- **Autorización por rol**: cada `key` solo es accesible por los roles indicados en §5. Si un rol no autorizado pide una `key` → `403`.
- **Scope por sucursal**: si el usuario es staff con sucursales asignadas (`branchIds`), el backend debe limitar los datos a esas sucursales aunque no envíe `branchId`.
- **Paciente / Médico**: los reportes de rol `PATIENT`/`DOCTOR` se limitan **siempre** al propio usuario autenticado (ignorar cualquier intento de ver datos de otros).

### 3.4. Contrato de error

Usar el manejador global existente. Errores esperados:
- `400` rango de fechas inválido (`from > to`).
- `403` rol/tenant sin acceso a la `key`.
- `404` `key` desconocida.
- `422` parámetros requeridos faltantes para ese reporte.

---

## 4. Sobre estándar `ReportResult` (el corazón del contrato)

El frontend tiene **un único visor genérico** que pinta cualquier reporte a partir de esta estructura. **Respeta los nombres de campo exactamente.**

```typescript
ReportResult {
  key: string;            // misma key del request
  title: string;          // título legible del reporte
  subtitle?: string;      // opcional
  generatedAt: string;    // ISO datetime de generación
  range?: { from: string; to: string };

  kpis: ReportKpi[];      // tarjetas grandes superiores
  sections: ReportSection[];  // bloques de detalle (orden = orden de render)
}

ReportKpi {
  label: string;          // "Ingresos del periodo"
  value: string;          // YA FORMATEADO: "S/ 12,450", "87%", "1,204"
  icon: string;           // Material Symbol, p. ej. "payments"
  hint?: string;          // texto secundario opcional
  variant?: "blue" | "green" | "orange" | "purple" | "red";
  trend?: { direction: "up" | "down" | "flat"; label: string }; // p. ej. "+12% vs periodo anterior"
}

// Una sección es de uno de estos 3 tipos (campo discriminador "type"):

ReportSection (type = "table") {
  type: "table";
  title: string;
  columns: { key: string; label: string; align?: "left"|"center"|"right"; format?: "text"|"number"|"currency"|"percent"|"date" }[];
  rows: Array<Record<string, string | number | null>>;  // cada row mapea column.key -> valor
}

ReportSection (type = "bars") {
  type: "bars";
  title: string;
  unit?: string;
  bars: { label: string; value: number; display: string }[];  // value = magnitud para el ancho; display = texto mostrado
}

ReportSection (type = "list") {
  type: "list";
  title: string;
  items: { label: string; value: string; hint?: string }[];
}
```

**Notas de formato:**
- Los **KPI `value`** y los **`bars.display`** y **`list.value`** vienen **ya formateados** como string (moneda, %, miles). El front no recalcula.
- En **tablas**, los valores numéricos pueden venir crudos (`number`); el front aplica `format` de la columna (`currency` → `S/`, `percent` → `%`, `number` → separador de miles `es-PE`). Da igual enviarlos formateados como string si prefieres; el front tolera ambos.
- `generatedAt` y `range.from/to` se muestran tal cual (manda strings ya legibles o ISO).

**Ejemplo mínimo de respuesta:**
```jsonc
{
  "success": true,
  "message": "OK",
  "data": {
    "key": "clinic-financial-summary",
    "title": "Resumen Financiero",
    "generatedAt": "2026-06-09 17:40",
    "range": { "from": "2026-06-01", "to": "2026-06-09" },
    "kpis": [
      { "label": "Ingresos del periodo", "value": "S/ 18,420", "icon": "payments", "variant": "green", "trend": { "direction": "up", "label": "+9% vs mes pasado" } },
      { "label": "Cobros pendientes", "value": "S/ 2,100", "icon": "pending", "variant": "orange" },
      { "label": "Ticket promedio", "value": "S/ 95", "icon": "receipt_long", "variant": "blue" },
      { "label": "Pagos registrados", "value": "194", "icon": "point_of_sale", "variant": "purple" }
    ],
    "sections": [
      {
        "type": "bars",
        "title": "Ingresos por sucursal",
        "bars": [
          { "label": "Sede Central", "value": 12000, "display": "S/ 12,000" },
          { "label": "Sede Norte", "value": 6420, "display": "S/ 6,420" }
        ]
      },
      {
        "type": "table",
        "title": "Detalle por método de pago",
        "columns": [
          { "key": "method", "label": "Método" },
          { "key": "count", "label": "Operaciones", "align": "right", "format": "number" },
          { "key": "total", "label": "Total", "align": "right", "format": "currency" }
        ],
        "rows": [
          { "method": "Efectivo", "count": 120, "total": 9800 },
          { "method": "Tarjeta", "count": 60, "total": 7200 },
          { "method": "Yape/Plin", "count": 14, "total": 1420 }
        ]
      }
    ]
  }
}
```

---

## 5. Catálogo de reportes por rol

> La columna **Acceso** indica los roles autorizados. Las **keys** son las que el frontend ya envía.
> Para cada reporte: KPIs sugeridos, secciones sugeridas y **fuente de datos** (tablas/campos).
> 🆕 = campo a verificar/crear en BD si no existe.

### 5.1. SUPER_ADMIN — Plataforma

#### `platform-organizations` — Organizaciones y Crecimiento
- **Acceso:** SUPER_ADMIN · **Filtros:** `from`, `to`
- **KPIs:** Tenants activos · Altas en el periodo · Bajas/cancelados · Retención (%)
- **Secciones:**
  - `bars` "Altas por mes"
  - `table` "Organizaciones recientes" (cols: nombre, plan, estado, fecha de alta)
- **Fuente:** `tenants` (`id`, `name`, `created_at`, `status` 🆕 si no existe estado de tenant), `subscriptions` (plan).
- 🆕 `tenants.status` (ACTIVE/SUSPENDED/CANCELLED), 🆕 `tenants.created_at` si falta.

#### `platform-subscription-revenue` — Ingresos por Suscripciones
- **Acceso:** SUPER_ADMIN · **Filtros:** `from`, `to`
- **KPIs:** MRR · Ingreso del periodo · Churn (%) · ARPA (ingreso promedio por cuenta)
- **Secciones:**
  - `bars` "Ingreso por plan"
  - `table` "Suscripciones facturadas" (plan, organización, monto, fecha)
- **Fuente:** `subscriptions` (`tenant_id`, `plan_id`, `amount` 🆕, `billing_date` 🆕, `status`), `plans` (`name`, `price`).
- 🆕 `subscriptions.amount`, 🆕 `subscriptions.billing_date` / historial de facturación (`subscription_invoices` 🆕 si se quiere histórico real de cobros).

#### `platform-usage` — Uso de la Plataforma
- **Acceso:** SUPER_ADMIN · **Filtros:** `from`, `to`
- **KPIs:** Usuarios activos · Citas creadas · Logins · Tenants con actividad
- **Secciones:**
  - `bars` "Citas creadas por organización"
  - `table` "Actividad por organización" (org, usuarios activos, citas, último acceso)
- **Fuente:** `users` (`last_login_at` 🆕), `appointments` (`created_at`, `tenant_id`).
- 🆕 `users.last_login_at` para "usuarios activos / último acceso".

#### `platform-subscription-status` — Estado de Suscripciones
- **Acceso:** SUPER_ADMIN · **Filtros:** `status`
- **KPIs:** Activas · En trial · Por vencer (≤7 días) · Vencidas/morosas
- **Secciones:**
  - `table` "Suscripciones" (org, plan, estado, vence el, días restantes)
- **Fuente:** `subscriptions` (`status`, `trial_ends_at` 🆕, `current_period_end` 🆕), `plans`.
- 🆕 `subscriptions.current_period_end`, 🆕 `subscriptions.trial_ends_at`.

---

### 5.2. CLINIC_ADMIN — Clínica

#### `clinic-financial-summary` — Resumen Financiero
- **Acceso:** CLINIC_ADMIN, SUPER_ADMIN · **Filtros:** `from`, `to`, `branchId`
- **KPIs:** Ingresos del periodo · Cobros pendientes · Ticket promedio · Nº de pagos
- **Secciones:**
  - `bars` "Ingresos por sucursal"
  - `table` "Detalle por método de pago" (método, operaciones, total)
  - `table` "Ingresos por día" (fecha, total)
- **Fuente:** `payments` (`appointment_id`, `amount`, `method` 🆕, `paid_at` 🆕, `status`), `appointments` (`branch_id`, `price` 🆕).
- 🆕 `payments.method` (EFECTIVO/TARJETA/YAPE/PLIN/TRANSFERENCIA), 🆕 `payments.paid_at`, 🆕 `appointments.price` (precio de la cita; ver memoria de agenda: el precio estaba pendiente).

#### `clinic-appointments-analysis` — Análisis de Citas
- **Acceso:** CLINIC_ADMIN, SUPER_ADMIN · **Filtros:** `from`, `to`, `branchId`, `status`
- **KPIs:** Total citas · Atendidas · Tasa de no-show (%) · Canceladas
- **Secciones:**
  - `bars` "Citas por estado"
  - `bars` "Citas por especialidad"
  - `table` "Citas por día" (fecha, agendadas, atendidas, no-show)
- **Fuente:** `appointments` (`status`, `flow_status`, `scheduled_at`, `branch_id`, `specialty_id`), `specialties` (`name`).

#### `clinic-doctor-productivity` — Productividad Médica
- **Acceso:** CLINIC_ADMIN, SUPER_ADMIN · **Filtros:** `from`, `to`, `branchId`, `doctorId`
- **KPIs:** Médicos activos · Consultas atendidas · Tiempo promedio de consulta 🆕 · Ingresos generados
- **Secciones:**
  - `bars` "Consultas por médico"
  - `table` "Ranking de médicos" (médico, especialidad, citas, no-shows, ingresos)
- **Fuente:** `appointments` (`doctor_id`, `status`, `started_at` 🆕, `ended_at` 🆕), `users`/`doctors`, `payments`.
- 🆕 `appointments.started_at` y `appointments.ended_at` (timestamps de inicio/fin de consulta) para tiempo promedio. Ver memoria de sala de espera: ya hay timestamps por transición de `flowStatus`; reutilizarlos si existen.

#### `clinic-patient-insights` — Análisis de Pacientes
- **Acceso:** CLINIC_ADMIN, SUPER_ADMIN · **Filtros:** `from`, `to`, `branchId`
- **KPIs:** Pacientes nuevos · Recurrentes · Total atendidos · Edad promedio
- **Secciones:**
  - `bars` "Pacientes por grupo de edad"
  - `bars` "Nuevos vs recurrentes por mes"
  - `table` "Procedencia / distrito" (opcional)
- **Fuente:** `patients` (`birth_date`, `created_at`, `district` 🆕 opcional, `gender`), `appointments` (`patient_id`, primera vs siguientes).

#### `clinic-branch-utilization` — Ocupación de Agenda
- **Acceso:** CLINIC_ADMIN, SUPER_ADMIN · **Filtros:** `from`, `to`, `branchId`
- **KPIs:** Slots disponibles · Slots ocupados · % de ocupación · Horas no usadas
- **Secciones:**
  - `bars` "% ocupación por sucursal"
  - `table` "Ocupación por médico/día" (médico, capacidad, ocupados, %)
- **Fuente:** `doctor_schedules` (capacidad/slots), `appointments` (ocupación). Calcular capacidad = slots definidos en el horario del médico por sucursal.
- 🆕 Si la capacidad no es derivable, exponer `doctor_schedules.slot_duration` y rango horario (ya existen según registro de médico).

---

### 5.3. DOCTOR — Limitado al médico autenticado

#### `doctor-productivity` — Mi Productividad
- **Acceso:** DOCTOR (solo sus datos) · **Filtros:** `from`, `to`
- **KPIs:** Consultas atendidas · Tiempo promedio 🆕 · Ausencias (no-show) · Pacientes únicos
- **Secciones:**
  - `bars` "Consultas por día"
  - `table` "Resumen por especialidad" (especialidad, citas, no-shows)
- **Fuente:** `appointments` filtradas por `doctor_id = currentUser`.

#### `doctor-diagnoses` — Diagnósticos Frecuentes
- **Acceso:** DOCTOR (solo sus datos) · **Filtros:** `from`, `to`
- **KPIs:** Diagnósticos registrados · CIE-10 distintos · Más frecuente
- **Secciones:**
  - `bars` "Top 10 diagnósticos CIE-10"
  - `table` "Detalle" (código CIE-10, descripción, frecuencia)
- **Fuente:** `medical_record_entries` / diagnósticos (`cie10_code` 🆕 si no se persiste el código), `cie10` catálogo.
- 🆕 Asegurar que el diagnóstico de la consulta persista el **código CIE-10** (no solo texto libre).

#### `doctor-prescriptions` — Recetas y Órdenes Emitidas
- **Acceso:** DOCTOR (solo sus datos) · **Filtros:** `from`, `to`
- **KPIs:** Recetas emitidas · Medicamentos distintos · Órdenes de examen
- **Secciones:**
  - `bars` "Medicamentos más prescritos"
  - `table` "Órdenes por tipo de examen"
- **Fuente:** `prescriptions` / `prescription_items`, `medical_orders` (ya existen `/doctors/me/prescriptions` y `/doctors/me/orders`).

---

### 5.4. RECEPTIONIST — Front desk

#### `reception-daily-appointments` — Citas por Día
- **Acceso:** RECEPTIONIST · **Filtros:** `from`, `to`, `branchId`
- **KPIs:** Agendadas · Atendidas · En espera · No-show
- **Secciones:**
  - `bars` "Citas por estado"
  - `table` "Citas por día" (fecha, agendadas, atendidas, canceladas, no-show)
- **Fuente:** `appointments` (`status`, `flow_status`, `scheduled_at`, `branch_id`).

#### `reception-cancellations` — Ausencias y Cancelaciones
- **Acceso:** RECEPTIONIST · **Filtros:** `from`, `to`, `branchId`
- **KPIs:** Cancelaciones · No-shows · Tasa de no-show (%) · Reprogramadas
- **Secciones:**
  - `bars` "No-show por día"
  - `table` "Motivos de cancelación" (motivo, cantidad)
- **Fuente:** `appointments` (`status` CANCELLED/NO_SHOW, `cancel_reason` 🆕, `rescheduled_from` 🆕).
- 🆕 `appointments.cancel_reason`, 🆕 marca de reprogramación si no existe.

#### `reception-cash-summary` — Resumen de Cobros
- **Acceso:** RECEPTIONIST · **Filtros:** `from`, `to`, `branchId`
- **KPIs:** Total cobrado · Nº de operaciones · Método más usado · Pendiente de cobro
- **Secciones:**
  - `bars` "Cobros por método"
  - `table` "Cobros por día" (fecha, operaciones, total)
- **Fuente:** `payments` (mismos campos 🆕 que en financiero: `method`, `paid_at`, `amount`).

---

### 5.5. ASSISTANT — Triaje

#### `assistant-triage-summary` — Resumen de Triaje
- **Acceso:** ASSISTANT · **Filtros:** `from`, `to`, `branchId`
- **KPIs:** Triajes realizados · Pacientes triados · Prioridad alta · Promedio por día
- **Secciones:**
  - `bars` "Triajes por prioridad"
  - `table` "Triajes por día" (fecha, total, prioridad alta/media/baja)
- **Fuente:** `triage` (`priority` 🆕 si no existe nivel de prioridad, `created_at`, `branch_id`, `assistant_id`).
- 🆕 `triage.priority` (ALTA/MEDIA/BAJA) si aún no se persiste.

#### `assistant-waiting-times` — Tiempos de Espera
- **Acceso:** ASSISTANT · **Filtros:** `from`, `to`, `branchId`
- **KPIs:** Espera promedio (min) · Espera máxima · Pacientes atendidos
- **Secciones:**
  - `bars` "Espera promedio por franja horaria"
  - `table` "Detalle por día" (fecha, espera promedio, máxima)
- **Fuente:** timestamps de la cola de sala de espera (`flow_status` transitions: hora de check-in vs hora de atención — ya existen según memoria de sala de espera). Calcular `wait = atención - check-in`.

---

### 5.6. PATIENT — Limitado al paciente autenticado

#### `patient-appointment-history` — Mi Historial de Citas
- **Acceso:** PATIENT (solo sus datos) · **Filtros:** `from`, `to`
- **KPIs:** Citas totales · Atendidas · Canceladas · Médicos distintos
- **Secciones:**
  - `table` "Mis citas" (fecha, médico, especialidad, estado)
- **Fuente:** `appointments` filtradas por `patient_id = currentUser`.

#### `patient-prescriptions` — Mis Recetas
- **Acceso:** PATIENT (solo sus datos) · **Filtros:** `from`, `to`
- **KPIs:** Recetas · Medicamentos activos · Última receta
- **Secciones:**
  - `table` "Medicamentos prescritos" (medicamento, dosis, indicación, fecha)
- **Fuente:** `prescriptions` / `prescription_items` del paciente.

#### `patient-health-summary` — Resumen de Salud
- **Acceso:** PATIENT (solo sus datos) · **Filtros:** `from`, `to`
- **KPIs:** Última presión · Último peso · IMC · Última consulta
- **Secciones:**
  - `bars` "Peso en el tiempo" (o presión)
  - `list` "Últimas mediciones" (label = signo vital, value = valor + fecha)
- **Fuente:** signos vitales del triaje/consulta (`vitals` 🆕 si no hay tabla de signos vitales con histórico: peso, talla, presión, FC, temperatura, fecha).
- 🆕 Tabla/columnas de **signos vitales con histórico** si no existen.

---

## 6. Checklist de campos a verificar/crear en BD

> Si alguno **ya existe**, ignóralo. Si **no existe**, créalo (entidad + migración) y persístelo donde corresponda.

- [ ] `tenants.status`, `tenants.created_at`
- [ ] `subscriptions.amount`, `subscriptions.billing_date`, `subscriptions.current_period_end`, `subscriptions.trial_ends_at`
- [ ] (opcional) `subscription_invoices` para histórico real de cobros de plataforma
- [ ] `users.last_login_at`
- [ ] `appointments.price`
- [ ] `appointments.started_at`, `appointments.ended_at` (o reutilizar timestamps de transición de `flow_status`)
- [ ] `appointments.cancel_reason`, marca de reprogramación
- [ ] `payments.method`, `payments.paid_at` (y `amount`/`status` si faltan)
- [ ] Diagnóstico con **código CIE-10** persistido en la consulta
- [ ] `triage.priority`
- [ ] Signos vitales con histórico (peso, talla, presión, FC, temperatura, fecha)
- [ ] `patients.district`/procedencia (opcional, para análisis de pacientes)

---

## 7. Resumen para el backend (TL;DR)

1. Implementa **`GET /api/reports/{key}`** → `ApiResponse<ReportResult>` (JSON, §4) y **`GET /api/reports/{key}/export?format=PDF|XLSX`** → archivo.
2. Respeta **exactamente** los nombres de campo del sobre `ReportResult`.
3. Filtra **siempre** por tenant/rol/sucursal en el servidor; PATIENT y DOCTOR solo ven lo suyo.
4. Usa **JasperReports** (PDF) y **Apache POI** (XLSX); reutiliza la misma consulta de agregación para JSON y para el archivo.
5. Crea en BD los campos marcados 🆕 que no existan (§6).
6. Empieza por los reportes de mayor valor: `clinic-financial-summary`, `clinic-appointments-analysis`, `doctor-productivity`, `reception-daily-appointments`. El frontend ya está cableado para consumirlos.
```
