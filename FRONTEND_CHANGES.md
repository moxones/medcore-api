# Cambios de API para el Frontend

Fecha: 2026-05-17

---

## 1. Flujo de registro rápido de paciente y modal de perfil incompleto

### Contexto
Un paciente puede registrarse de forma rápida desde la app sin llenar todos sus datos.
Cuando el paciente inicia sesión, el frontend debe verificar si su perfil está completo
y, en caso contrario, mostrar un modal para que lo complete.

### Endpoint público de registro rápido

```
POST /public/patients/register
```

No requiere autenticación. Es el primer paso para que un paciente se auto-registre.

**Body:**
```json
{
  "documentTypeCode": "DNI",       // requerido
  "documentNumber": "12345678",    // requerido, único por tenant
  "email": "paciente@email.com",   // requerido, válido y único
  "password": "segura123",         // requerido
  "firstName": "Juan",             // opcional
  "lastName": "Pérez",             // opcional
  "phone": "987654321"             // opcional
}
```

**Respuesta exitosa (200):**
```json
{
  "success": true,
  "data": { /* PatientResponse */ },
  "message": "Paciente registrado exitosamente"
}
```

Al crearse, el `profileCompleted` queda en `false` porque falta: género, fecha de nacimiento y otros campos.

---

### Verificar disponibilidad de email (antes de registrar)

```
GET /public/patients/check-email?email=paciente@email.com
```

**Respuesta:**
```json
{
  "success": true,
  "data": { "available": true },
  "message": null
}
```

---

### Endpoint `/auth/me` — ahora incluye `profileCompleted`

Después de que el paciente inicia sesión, el frontend debe llamar a este endpoint
para obtener el estado del perfil:

```
GET /auth/me
Authorization: Bearer <token>
```

**Respuesta (NUEVO campo `profileCompleted`):**
```json
{
  "success": true,
  "data": {
    "userId": 42,
    "email": "paciente@email.com",
    "roles": ["PATIENT"],
    "firstName": "Juan",
    "lastName": "Pérez",
    "tenantId": 1,
    "profileCompleted": false   // <-- NUEVO
  }
}
```

### Lógica del modal en el frontend

```
Al iniciar sesión (o al cargar la app con sesión activa):
  1. Llamar GET /auth/me
  2. Si data.profileCompleted === false Y el rol es "PATIENT"
     → Mostrar modal "Completa tu perfil"
  3. Si data.profileCompleted === true
     → No mostrar el modal, flujo normal
```

**Campos que se deben completar en el modal** (para que `profileCompleted` pase a `true`):
- Teléfono (`phone`)
- Género (`gender`) — ej: "M", "F", "OTHER"
- Fecha de nacimiento (`birthDate`) — formato `YYYY-MM-DD`
- Email de contacto (`contactEmail`) — ya viene del registro

El endpoint para actualizar el perfil es:
```
PUT /patients/profile
Authorization: Bearer <token>
```

---

## 2. Origen de la cita (`bookingSource`)

### Contexto
Al crear una cita, ahora se debe indicar cómo fue agendada:
el paciente lo hizo él mismo (app/web), o el staff lo agendó por teléfono o presencialmente.

### Campo nuevo en `POST /appointments`

```
POST /appointments
Authorization: Bearer <token>
```

**Body (campo nuevo: `bookingSource`):**
```json
{
  "patientId": 10,
  "doctorId": 5,
  "branchId": 2,
  "scheduledAt": "2026-06-01T10:00:00",
  "appointmentTypeId": 1,
  "reason": "Consulta general",
  "bookingSource": "SELF"   // <-- NUEVO (opcional, pero recomendado)
}
```

**Valores válidos de `bookingSource`:**

| Valor       | Cuándo usarlo                                              |
|-------------|------------------------------------------------------------|
| `SELF`      | El paciente agendó desde la app o portal web               |
| `PHONE`     | El staff agendó la cita tras una llamada telefónica         |
| `IN_PERSON` | El paciente llegó a la clínica y el staff agendó en sitio  |

> Si no se envía `bookingSource`, quedará `null` en la base de datos.
> Se recomienda siempre enviarlo para mantener trazabilidad.

### Campo nuevo en la respuesta de citas

Todos los endpoints que devuelven citas (`GET /appointments`, `GET /appointments/{id}`,
`GET /appointments/calendar`) ahora incluyen el campo `bookingSource` en la respuesta:

```json
{
  "success": true,
  "data": {
    "id": 101,
    "patientId": 10,
    "patientName": "Juan Pérez",
    "patientPhone": "987654321",
    "doctorId": 5,
    "doctorName": "Dra. Ana García",
    "branchId": 2,
    "branchName": "Sede Central",
    "scheduledAt": "2026-06-01T10:00:00",
    "statusId": 1,
    "appointmentTypeId": 1,
    "reason": "Consulta general",
    "durationMinutes": 30,
    "flowStatus": "WAITING",
    "createdAt": "2026-05-17T09:00:00",
    "bookingSource": "SELF"   // <-- NUEVO (puede ser null si no fue enviado)
  }
}
```

---

## 3. Migración de base de datos requerida

Los cambios en entidades requieren ejecutar el siguiente SQL en la base de datos:

```sql
-- Campo para origen de la cita
ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS booking_source VARCHAR(30) NULL;

-- El campo profile_completed ya existe en persons,
-- no se requiere migración adicional para ese flujo.
```

---

## Resumen de cambios

| Área              | Cambio                                                   | Impacto frontend                                    |
|-------------------|----------------------------------------------------------|-----------------------------------------------------|
| `GET /auth/me`    | Nuevo campo `profileCompleted` (Boolean)                 | Mostrar/ocultar modal de completar perfil           |
| `POST /public/patients/register` | Sin cambios en contrato             | Flujo existente sin modificación                    |
| `POST /appointments` | Nuevo campo opcional `bookingSource` en el body      | Enviar el valor según contexto (SELF/PHONE/IN_PERSON)|
| `GET /appointments*`  | Nuevo campo `bookingSource` en la respuesta (String) | Mostrar etiqueta de origen en listados/detalle      |
