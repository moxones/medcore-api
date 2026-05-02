# MedCore API - Multi-Tenant User Management

## Base URL
```
http://{tenant}.localhost:8080
```

## Authentication

All protected endpoints require:
```
Authorization: Bearer {accessToken}
```

---

## 1. AUTH - Autenticación

### POST /auth/register
Registro de paciente público dentro de un tenant.

**Request:**
```json
{
  "firstName": "Juan",
  "lastName": "Pérez",
  "birthDate": "1990-05-15",
  "documentTypeCode": "DNI",
  "documentNumber": "12345678",
  "email": "juan.perez@mail.com",
  "password": "securePassword123"
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
  },
  "message": "Usuario registrado exitosamente"
}
```

**Errores:**
- `400`: Email ya existe / Documento ya tiene cuenta
- `400`: Tipo de documento inválido

---

### POST /auth/login
Inicio de sesión dentro de un tenant.

**Request:**
```json
{
  "email": "juan.perez@mail.com",
  "password": "securePassword123"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
  },
  "message": "Login exitoso"
}
```

**Errores:**
- `400`: Credenciales inválidas
- `400`: Usuario inactivo

---

### POST /auth/refresh
Renovar access token usando refresh token.

**Request:**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "bmV3IHJlZnJlc2ggdG9rZW4..."
  },
  "message": "Token renovado"
}
```

**Errores:**
- `400`: Token no pertenece a este tenant
- `400`: Token expirado o inválido

---

### POST /auth/logout
Cerrar sesión (revocar refresh token).

**Request:**
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

**Response (200):**
```json
{
  "success": true,
  "data": null,
  "message": "Sesión cerrada"
}
```

---

### GET /auth/me
Obtener información del usuario autenticado.

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "userId": 15,
    "email": "juan.perez@mail.com",
    "firstName": "Juan",
    "lastName": "Pérez",
    "roles": ["PATIENT"],
    "tenantId": 1
  },
  "message": "Usuario actual"
}
```

**Errores:**
- `401`: No autenticado
- `403`: Acceso denegado

---

## 2. USERS - Gestión de Usuarios (Admin/Súper Admin)

### POST /users
Crear nuevo usuario dentro del tenant actual. **Requiere rol ADMIN o SUPER_ADMIN.**

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "email": "dr.smith@clinic.com",
  "password": "doctorPassword123",
  "person": {
    "firstName": "Carlos",
    "lastName": "Smith",
    "birthDate": "1985-03-20",
    "gender": "M",
    "phone": "+5491123456789",
    "documentTypeCode": "DNI",
    "documentNumber": "87654321"
  },
  "roleIds": [2, 3],
  "roles": ["DOCTOR", "ADMIN"]
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "email": "dr.smith@clinic.com",
    "isActive": true,
    "tenantId": 1,
    "person": {
      "id": 28,
      "firstName": "Carlos",
      "lastName": "Smith",
      "birthDate": "1985-03-20",
      "gender": "M",
      "phone": "+5491123456789",
      "contactEmail": "dr.smith@clinic.com",
      "profileCompleted": true,
      "documents": [
        {
          "id": 15,
          "documentType": { "id": 1, "code": "DNI", "name": "Documento Nacional de Identidad" },
          "documentNumber": "87654321"
        }
      ]
    },
    "roles": [
      { "id": 2, "name": "Doctor", "code": "DOCTOR" },
      { "id": 3, "name": "Administrador", "code": "ADMIN" }
    ],
    "createdAt": "2026-05-01T10:30:00"
  },
  "message": "Usuario creado"
}
```

**Errores:**
- `400`: Email ya existe
- `400`: El tipo y número de documento son obligatorios
- `400`: No tienes permisos para asignar roles de administrador (si eres ADMIN tratando de asignar ADMIN/SUPER_ADMIN)
- `400`: Password debe tener al menos 8 caracteres
- `403`: Acceso denegado

---

### GET /users
Listar todos los usuarios del tenant actual. **Requiere rol ADMIN o SUPER_ADMIN.**

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 42,
      "email": "dr.smith@clinic.com",
      "isActive": true,
      "tenantId": 1,
      "person": {
        "id": 28,
        "firstName": "Carlos",
        "lastName": "Smith",
        "profileCompleted": true
      },
      "roles": [
        { "id": 2, "name": "Doctor", "code": "DOCTOR" }
      ],
      "createdAt": "2026-05-01T10:30:00"
    },
    {
      "id": 15,
      "email": "juan.perez@mail.com",
      "isActive": true,
      "tenantId": 1,
      "person": {
        "id": 10,
        "firstName": "Juan",
        "lastName": "Pérez",
        "profileCompleted": true
      },
      "roles": [
        { "id": 1, "name": "Paciente", "code": "PATIENT" }
      ],
      "createdAt": "2026-04-15T08:00:00"
    }
  ],
  "message": "Lista de usuarios"
}
```

---

### GET /users/{id}
Obtener usuario específico por ID. **Requiere rol ADMIN o SUPER_ADMIN.**

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "email": "dr.smith@clinic.com",
    "isActive": true,
    "tenantId": 1,
    "person": {
      "id": 28,
      "firstName": "Carlos",
      "lastName": "Smith",
      "birthDate": "1985-03-20",
      "gender": "M",
      "phone": "+5491123456789",
      "contactEmail": "dr.smith@clinic.com",
      "profileCompleted": true,
      "documents": [...]
    },
    "roles": [...],
    "createdAt": "2026-05-01T10:30:00"
  },
  "message": "Usuario encontrado"
}
```

**Errores:**
- `404`: Usuario no encontrado

---

### PUT /users/{id}
Actualizar usuario. **Requiere rol ADMIN o SUPER_ADMIN.**

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "email": "dr.smith.new@clinic.com",
  "person": {
    "firstName": "Carlos",
    "lastName": "Smith Rodriguez",
    "birthDate": "1985-03-20",
    "gender": "M",
    "phone": "+5491198765432",
    "documentTypeCode": "DNI",
    "documentNumber": "87654321"
  },
  "roles": ["DOCTOR"]
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "email": "dr.smith.new@clinic.com",
    "isActive": true,
    "tenantId": 1,
    "person": {
      "id": 28,
      "firstName": "Carlos",
      "lastName": "Smith Rodriguez",
      "phone": "+5491198765432",
      "profileCompleted": true
    },
    "roles": [
      { "id": 2, "name": "Doctor", "code": "DOCTOR" }
    ]
  },
  "message": "Usuario actualizado"
}
```

**Errores:**
- `400`: Email ya existe
- `400`: No tienes permisos para asignar roles de administrador

---

### PATCH /users/{id}/status
Activar/desactivar usuario. **Requiere rol ADMIN o SUPER_ADMIN.**

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "isActive": false
}
```

**Response (200):**
```json
{
  "success": true,
  "data": null,
  "message": "Estado actualizado"
}
```

---

### POST /users/{id}/roles
Asignar roles a un usuario. **Requiere rol ADMIN o SUPER_ADMIN.**

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "roleIds": [2, 4]
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "email": "dr.smith@clinic.com",
    "roles": [
      { "id": 2, "name": "Doctor", "code": "DOCTOR" },
      { "id": 4, "name": "Asistente", "code": "ASSISTANT" }
    ]
  },
  "message": "Roles asignados"
}
```

**Errores:**
- `400`: Uno o más roleIds no existen
- `400`: No tienes permisos para asignar roles de administrador

---

## 3. SUPER ADMIN - Gestión Multi-Tenant

### POST /super-admin/users
Crear usuario en un tenant específico. **Solo SUPER_ADMIN.**

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "tenantId": 3,
  "email": "admin@othertenant.com",
  "password": "adminPassword123",
  "person": {
    "firstName": "Admin",
    "lastName": "Otro",
    "documentTypeCode": "DNI",
    "documentNumber": "11223344"
  },
  "roles": ["ADMIN"]
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 55,
    "email": "admin@othertenant.com",
    "tenantId": 3,
    "roles": [
      { "id": 3, "name": "Administrador", "code": "ADMIN" }
    ]
  },
  "message": "Usuario creado"
}
```

**Errores:**
- `400`: Email ya existe en ese tenant
- `403`: Acceso denegado (requiere SUPER_ADMIN)

---

### GET /super-admin/users?tenantId={id}
Listar usuarios de un tenant específico. **Solo SUPER_ADMIN.**

**Headers:** `Authorization: Bearer {accessToken}`

**Query Params:**
- `tenantId` (required): ID del tenant

**Response (200):**
```json
{
  "success": true,
  "data": [...],
  "message": "Lista de usuarios"
}
```

**Errores:**
- `400`: TenantId es requerido

---

## 4. PROFILE - Perfil de Usuario

### PUT /profile/password
Cambiar password del usuario autenticado.

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newSecurePassword456"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": null,
  "message": "Password actualizado"
}
```

**Errores:**
- `400`: Password actual incorrecto
- `403`: No autorizado para cambiar este password

---

## Estructura de Respuestas

### ApiResponse (wrapper estándar)
```json
{
  "success": true|false,
  "data": {...} | [...],
  "message": "Descripción del resultado"
}
```

### AuthResponse
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

### UserMeResponse
```json
{
  "userId": 1,
  "email": "user@example.com",
  "firstName": "Nombre",
  "lastName": "Apellido",
  "roles": ["ROLE1", "ROLE2"],
  "tenantId": 1
}
```

### UserResponse
```json
{
  "id": 1,
  "email": "user@example.com",
  "isActive": true,
  "tenantId": 1,
  "person": {
    "id": 1,
    "firstName": "Nombre",
    "lastName": "Apellido",
    "birthDate": "1990-01-01",
    "gender": "M|F|O",
    "phone": "+5491112345678",
    "contactEmail": "user@example.com",
    "profileCompleted": true,
    "documents": [
      {
        "id": 1,
        "documentType": { "id": 1, "code": "DNI", "name": "Documento Nacional de Identidad" },
        "documentNumber": "12345678"
      }
    ]
  },
  "roles": [
    { "id": 1, "name": "Paciente", "code": "PATIENT" }
  ],
  "createdAt": "2026-05-01T10:00:00"
}
```

---

## Códigos de Error Comunes

| Código | Descripción |
|--------|-------------|
| 400 | Bad Request - Datos inválidos o negocio no permitido |
| 401 | Unauthorized - No autenticado |
| 403 | Forbidden - Sin permisos para la operación |
| 404 | Not Found - Recurso no encontrado |
| 409 | Conflict - Recurso duplicado (ej: email ya existe) |
| 500 | Internal Server Error - Error inesperado |

---

## Roles Disponibles

| Code | Nombre | Descripción |
|------|--------|-------------|
| SUPER_ADMIN | Súper Administrador | Acceso total a todos los tenants |
| ADMIN | Administrador | Administrador del tenant |
| DOCTOR | Doctor | Profesional médico |
| ASSISTANT | Asistente | Asistente administrativo |
| PATIENT | Paciente | Usuario paciente |

---

## Notas de Implementación

1. **Tenant Isolation**: Cada consulta está filtrada por `tenant_id` extraído del subdomain
2. **Documentos únicos por tenant**: `(tenant_id, document_type_id, document_number)` debe ser único
3. **Mismo documento en diferentes tenants**: Una persona puede existir en múltiples tenants con el mismo documento
4. **Concurrent registrations**: Se valida email y documento únicos dentro del mismo tenant
5. **Password mínimo**: 8 caracteres