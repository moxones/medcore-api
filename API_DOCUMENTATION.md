# 📋 DOCUMENTACIÓN COMPLETA DE APIS - MEDCORE

## Índice General
1. [Auth Controller](#auth-controller)
2. [Patient Controller](#patient-controller)
3. [User Controller](#user-controller)
4. [Test Security Controller](#test-security-controller)
5. [Tenant Public Controller](#tenant-public-controller)
6. [DTOs Compartidos](#dtos-compartidos)

---

## 🔐 AUTH CONTROLLER
**Base Path:** `/auth`

### 1. Registro
- **Endpoint:** `POST /auth/register`
- **Descripción:** Registra un nuevo usuario en el sistema
- **Autenticación:** NO REQUERIDA
- **Body Request:**
```json
{
  "firstName": "string",
  "lastName": "string",
  "birthDate": "2024-01-15",
  "documentTypeCode": "string (ej: CC, CE)",
  "documentNumber": "string",
  "email": "usuario@example.com",
  "password": "string"
}
```
- **Response:** `200 OK`
```json
{
  "accessToken": "jwt_token",
  "refreshToken": "jwt_token"
}
```

### 2. Login
- **Endpoint:** `POST /auth/login`
- **Descripción:** Inicia sesión con email y contraseña
- **Autenticación:** NO REQUERIDA
- **Body Request:**
```json
{
  "email": "usuario@example.com",
  "password": "string"
}
```
- **Response:** `200 OK`
```json
{
  "accessToken": "jwt_token",
  "refreshToken": "jwt_token"
}
```

### 3. Refresh Token
- **Endpoint:** `POST /auth/refresh`
- **Descripción:** Obtiene un nuevo access token usando el refresh token
- **Autenticación:** NO REQUERIDA
- **Body Request:**
```json
{
  "refreshToken": "jwt_token"
}
```
- **Response:** `200 OK`
```json
{
  "accessToken": "jwt_token",
  "refreshToken": "jwt_token"
}
```

### 4. Logout
- **Endpoint:** `POST /auth/logout`
- **Descripción:** Cierra la sesión actual
- **Autenticación:** REQUERIDA
- **Body Request:**
```json
{
  "refreshToken": "jwt_token"
}
```
- **Response:** `200 OK` (Sin body)

---

## 👥 PATIENT CONTROLLER
**Base Path:** `/patients`

### 1. Crear Paciente
- **Endpoint:** `POST /patients`
- **Descripción:** Crea un nuevo registro de paciente
- **Autenticación:** REQUERIDA
- **Body Request:**
```json
{
  "firstName": "string",
  "lastName": "string",
  "birthDate": "2024-01-15",
  "documentTypeCode": "string (ej: CC)",
  "documentNumber": "string"
}
```
- **Response:** `200 OK`
```json
{
  "id": 1,
  "firstName": "string",
  "lastName": "string",
  "contactEmail": "string"
}
```

### 2. Obtener Todos los Pacientes
- **Endpoint:** `GET /patients`
- **Descripción:** Obtiene la lista completa de pacientes
- **Autenticación:** REQUERIDA
- **Parameters:** Ninguno
- **Response:** `200 OK`
```json
[
  {
    "id": 1,
    "firstName": "string",
    "lastName": "string",
    "contactEmail": "string"
  }
]
```

### 3. Obtener Paciente por ID
- **Endpoint:** `GET /patients/{id}`
- **Descripción:** Obtiene un paciente específico por ID
- **Autenticación:** REQUERIDA
- **URL Parameters:**
  - `id`: Long (ID del paciente)
- **Response:** `200 OK`
```json
{
  "id": 1,
  "firstName": "string",
  "lastName": "string",
  "contactEmail": "string"
}
```

### 4. Actualizar Perfil del Paciente
- **Endpoint:** `PUT /patients/profile`
- **Descripción:** Actualiza el perfil del paciente autenticado
- **Autenticación:** REQUERIDA
- **Body Request:**
```json
{
  "phone": "string",
  "gender": "string (ej: M, F, O)",
  "birthDate": "2024-01-15",
  "contactEmail": "usuario@example.com"
}
```
- **Response:** `200 OK` (Sin body)

### 5. Buscar Pacientes
- **Endpoint:** `GET /patients/search`
- **Descripción:** Busca pacientes por término
- **Autenticación:** REQUERIDA
- **Query Parameters:**
  - `term`: string (término de búsqueda)
- **Response:** `200 OK`
```json
[
  {
    "id": 1,
    "firstName": "string",
    "lastName": "string",
    "contactEmail": "string"
  }
]
```

---

## 👨‍💼 USER CONTROLLER
**Base Path:** `/users`
**Nota:** Todos los endpoints requieren rol ADMIN o SUPERADMIN

### 1. Crear Usuario
- **Endpoint:** `POST /users`
- **Descripción:** Crea un nuevo usuario en el sistema
- **Autenticación:** REQUERIDA (ADMIN/SUPERADMIN)
- **Body Request:**
```json
{
  "email": "usuario@example.com",
  "password": "string",
  "person": {
    "firstName": "string",
    "lastName": "string",
    "birthDate": "2024-01-15",
    "gender": "string",
    "phone": "string",
    "documentTypeCode": "string",
    "documentNumber": "string"
  },
  "roleIds": [1, 2]
}
```
- **Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "usuario@example.com",
    "isActive": true,
    "person": {
      "firstName": "string",
      "lastName": "string",
      "phone": "string"
    },
    "roles": ["ADMIN", "USER"]
  },
  "message": "Usuario creado"
}
```

### 2. Obtener Todos los Usuarios
- **Endpoint:** `GET /users`
- **Descripción:** Obtiene la lista de todos los usuarios
- **Autenticación:** REQUERIDA (ADMIN/SUPERADMIN)
- **Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "email": "usuario@example.com",
      "isActive": true,
      "person": {
        "firstName": "string",
        "lastName": "string",
        "phone": "string"
      },
      "roles": ["ADMIN"]
    }
  ],
  "message": "Lista de usuarios"
}
```

### 3. Obtener Usuario por ID
- **Endpoint:** `GET /users/{id}`
- **Descripción:** Obtiene un usuario específico
- **Autenticación:** REQUERIDA (ADMIN/SUPERADMIN)
- **URL Parameters:**
  - `id`: Long (ID del usuario)
- **Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "usuario@example.com",
    "isActive": true,
    "person": {
      "firstName": "string",
      "lastName": "string",
      "phone": "string"
    },
    "roles": ["ADMIN"]
  },
  "message": "Usuario encontrado"
}
```

### 4. Actualizar Usuario
- **Endpoint:** `PUT /users/{id}`
- **Descripción:** Actualiza la información de un usuario
- **Autenticación:** REQUERIDA (ADMIN/SUPERADMIN)
- **URL Parameters:**
  - `id`: Long (ID del usuario)
- **Body Request:**
```json
{
  "email": "nuevo@example.com",
  "person": {
    "firstName": "string",
    "lastName": "string",
    "birthDate": "2024-01-15",
    "gender": "string",
    "phone": "string",
    "documentTypeCode": "string",
    "documentNumber": "string"
  },
  "roleIds": [1]
}
```
- **Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "nuevo@example.com",
    "isActive": true,
    "person": {
      "firstName": "string",
      "lastName": "string",
      "phone": "string"
    },
    "roles": ["USER"]
  },
  "message": "Usuario actualizado"
}
```

### 5. Actualizar Estado del Usuario
- **Endpoint:** `PATCH /users/{id}/status`
- **Descripción:** Activa o desactiva un usuario
- **Autenticación:** REQUERIDA (ADMIN/SUPERADMIN)
- **URL Parameters:**
  - `id`: Long (ID del usuario)
- **Body Request:**
```json
{
  "isActive": true
}
```
- **Response:** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "Estado actualizado"
}
```

---

## 🧪 TEST SECURITY CONTROLLER
**Base Path:** `/test-security`

### 1. Endpoint Público
- **Endpoint:** `GET /test-security/public`
- **Descripción:** Endpoint de prueba sin autenticación
- **Autenticación:** NO REQUERIDA
- **Response:** `200 OK`
```
"public ok"
```

### 2. Endpoint Solo Admin
- **Endpoint:** `GET /test-security/admin`
- **Descripción:** Endpoint de prueba solo para SUPERADMIN
- **Autenticación:** REQUERIDA (SUPERADMIN)
- **Response:** `200 OK`
```
"admin ok"
```

---

## 🏢 TENANT PUBLIC CONTROLLER
**Base Path:** `/public`

### 1. Obtener Información del Tenant
- **Endpoint:** `GET /public/tenant-info`
- **Descripción:** Obtiene información pública del tenant/organización
- **Autenticación:** NO REQUERIDA
- **Response:** `200 OK`
```json
{
  "name": "string (nombre de la organización)",
  "logoUrl": "string (URL del logo)",
  "primaryColor": "string (color principal, ej: #FF5733)",
  "subtitle": "string (subtítulo)"
}
```

---

## 📦 DTOs COMPARTIDOS

### RegisterRequest
```json
{
  "firstName": "string",
  "lastName": "string",
  "birthDate": "LocalDate (YYYY-MM-DD)",
  "documentTypeCode": "string",
  "documentNumber": "string",
  "email": "string",
  "password": "string"
}
```

### LoginRequest
```json
{
  "email": "string",
  "password": "string"
}
```

### RefreshRequest
```json
{
  "refreshToken": "string"
}
```

### AuthResponse
```json
{
  "accessToken": "string (JWT)",
  "refreshToken": "string (JWT)"
}
```

### CreatePatientRequest
```json
{
  "firstName": "string",
  "lastName": "string",
  "birthDate": "LocalDate (YYYY-MM-DD)",
  "documentTypeCode": "string",
  "documentNumber": "string"
}
```

### UpdateProfileRequest
```json
{
  "phone": "string",
  "gender": "string",
  "birthDate": "LocalDate (YYYY-MM-DD)",
  "contactEmail": "string (debe ser email válido)"
}
```

### PatientResponse
```json
{
  "id": "Long",
  "firstName": "string",
  "lastName": "string",
  "contactEmail": "string"
}
```

### CreateUserRequest
```json
{
  "email": "string (debe ser válido y único)",
  "password": "string",
  "person": {
    "firstName": "string",
    "lastName": "string",
    "birthDate": "LocalDate",
    "gender": "string",
    "phone": "string",
    "documentTypeCode": "string",
    "documentNumber": "string"
  },
  "roleIds": "List<Long>"
}
```

### UpdateUserRequest
```json
{
  "email": "string",
  "person": {
    "firstName": "string",
    "lastName": "string",
    "birthDate": "LocalDate",
    "gender": "string",
    "phone": "string",
    "documentTypeCode": "string",
    "documentNumber": "string"
  },
  "roleIds": "List<Long>"
}
```

### UpdateUserStatusRequest
```json
{
  "isActive": "boolean"
}
```

### UserResponse
```json
{
  "id": "Long",
  "email": "string",
  "isActive": "boolean",
  "person": {
    "firstName": "string",
    "lastName": "string",
    "phone": "string"
  },
  "roles": ["string"]
}
```

### PersonRequest
```json
{
  "firstName": "string",
  "lastName": "string",
  "birthDate": "LocalDate",
  "gender": "string",
  "phone": "string",
  "documentTypeCode": "string",
  "documentNumber": "string"
}
```

### PersonResponse
```json
{
  "firstName": "string",
  "lastName": "string",
  "phone": "string"
}
```

### TenantInfoResponse
```json
{
  "name": "string",
  "logoUrl": "string",
  "primaryColor": "string",
  "subtitle": "string"
}
```

### ApiResponse (Wrapper genérico para respuestas de Users)
```json
{
  "success": "boolean",
  "data": "T (tipo genérico)",
  "message": "string"
}
```

---

## 🔑 Información de Autenticación

### Headers Requeridos (para endpoints autenticados)
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### Tipos de Roles
- **SUPERADMIN**: Acceso total a funciones administrativas
- **ADMIN**: Acceso a gestión de usuarios y configuración
- **USER**: Usuario estándar del sistema
- **PATIENT**: Usuario paciente

### Códigos de Tipo de Documento
Ejemplos comunes:
- `CC`: Cédula de Ciudadanía (Colombia)
- `CE`: Cédula de Extranjería (Colombia)
- `PA`: Pasaporte
- `DNI`: Documento Nacional de Identidad

---

## 📝 Notas Importantes

1. **Validaciones de Email**: Todos los campos de email deben ser válidos
2. **Contraseñas**: Deben cumplir con los requisitos de seguridad del sistema
3. **Tokens JWT**: Tienen una duración limitada, usar refresh token para renovar
4. **Roles**: Los roleIds deben existir en la base de datos
5. **Formato de Fechas**: Use formato ISO 8601 (YYYY-MM-DD)
6. **Endpoint de Perfil de Paciente**: Usa ruta `/patients/profile` en PUT, no `/patients/{id}`
7. **Búsqueda de Pacientes**: La búsqueda es por término general (nombre, documento, etc.)

---

## 🚀 Resumen de Endpoints por Método HTTP

### POST (Crear)
- `POST /auth/register` - Registrar nuevo usuario
- `POST /auth/login` - Iniciar sesión
- `POST /auth/refresh` - Renovar token
- `POST /auth/logout` - Cerrar sesión
- `POST /patients` - Crear paciente
- `POST /users` - Crear usuario

### GET (Obtener)
- `GET /patients` - Listar todos los pacientes
- `GET /patients/{id}` - Obtener paciente por ID
- `GET /patients/search?term=...` - Buscar pacientes
- `GET /users` - Listar todos los usuarios
- `GET /users/{id}` - Obtener usuario por ID
- `GET /test-security/public` - Prueba pública
- `GET /test-security/admin` - Prueba admin
- `GET /public/tenant-info` - Información del tenant

### PUT (Actualizar completo)
- `PUT /patients/profile` - Actualizar perfil del paciente
- `PUT /users/{id}` - Actualizar usuario

### PATCH (Actualizar parcial)
- `PATCH /users/{id}/status` - Cambiar estado del usuario

---

**Última actualización:** 2026-04-24
**Versión API:** 1.0
