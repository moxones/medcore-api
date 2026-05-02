# 🏥 MedCore API - Documentación Completa del Proyecto

## 📋 Tabla de Contenidos
1. [Descripción General](#descripción-general)
2. [Stack Tecnológico](#stack-tecnológico)
3. [Requisitos del Sistema](#requisitos-del-sistema)
4. [Estructura del Proyecto](#estructura-del-proyecto)
5. [Modelos de Datos Principales](#modelos-de-datos-principales)
6. [Controladores Disponibles](#controladores-disponibles)
7. [Arquitectura y Componentes](#arquitectura-y-componentes)
8. [Seguridad](#seguridad)
9. [Configuración](#configuración)
10. [Guía de Desarrollo](#guía-de-desarrollo)
11. [Base de Datos](#base-de-datos)

---

## 📖 Descripción General

**MedCore** es un API REST multi-tenant para un Sistema de Gestión Médica (SaaS). Proporciona funcionalidades completas para:
- Gestión de pacientes y citas médicas
- Control de usuarios y autorización por roles
- Gestión de registros médicos y triaje
- Gestión de clínicas/sucursales (Multi-tenant)
- Sistema de suscripciones y planes
- Dashboard administrativo
- APIs públicas para integración externa

### Características Principales
- ✅ **Multi-tenant**: Soporte para múltiples organizaciones independientes
- ✅ **Seguridad**: Autenticación JWT, autorización basada en roles (RBAC)
- ✅ **RESTful**: API completamente REST con convenciones estándar
- ✅ **Persistencia**: JPA/Hibernate con PostgreSQL
- ✅ **Auditoría**: Registro de cambios y acciones

---

## 🛠️ Stack Tecnológico

### Versiones
| Componente | Versión |
|-----------|---------|
| **Java** | 21 LTS |
| **Spring Boot** | 3.5.0 |
| **Spring Security** | Incluida en Boot |
| **Spring Data JPA** | Incluida en Boot |
| **PostgreSQL** | Latest |
| **JWT (JJWT)** | 0.11.5 |
| **Lombok** | Incluida en Boot |
| **Maven** | 3.6+ |

### Dependencias Principales
```xml
- Spring Boot Starter Data JPA (ORM)
- Spring Boot Starter Security (Autenticación/Autorización)
- Spring Boot Starter Validation (Validación de datos)
- Spring Boot Starter Web (REST API)
- JJWT (JWT Token management)
- PostgreSQL Driver (Base de datos)
- Lombok (Reduce boilerplate)
```

---

## ✅ Requisitos del Sistema

### Software Requerido
- **Java 21 LTS** instalado
- **Maven 3.6+** para compilación
- **PostgreSQL 12+** para base de datos
- **Git** para control de versiones

### Configuración Inicial
1. Clonar el repositorio
2. Configurar variables de entorno:
   ```bash
   export DB_PASSWORD=your_password
   export JWT_SECRET=your_secret_key
   ```
3. Crear base de datos PostgreSQL:
   ```sql
   CREATE DATABASE health_system_db;
   ```
4. Compilar: `mvn clean install`
5. Ejecutar: `mvn spring-boot:run`

El servidor iniciará en `http://localhost:8080`

---

## 📁 Estructura del Proyecto

```
medcore-api/
├── src/
│   ├── main/
│   │   ├── java/com/medical/medcore/
│   │   │   ├── MedcoreApiApplication.java          # Punto de entrada
│   │   │   ├── config/                              # Configuración
│   │   │   │   ├── audit/                           # Auditoría
│   │   │   │   ├── exception/                       # Manejo de excepciones
│   │   │   │   └── web/                             # Config web
│   │   │   ├── controller/                          # REST Controllers
│   │   │   │   ├── AppointmentController.java       # Citas médicas
│   │   │   │   ├── AuthController.java              # Autenticación
│   │   │   │   ├── BranchController.java            # Sucursales
│   │   │   │   ├── CatalogController.java           # Catálogos
│   │   │   │   ├── DashboardController.java         # Dashboard
│   │   │   │   ├── DoctorController.java            # Médicos
│   │   │   │   ├── MedicalRecordController.java     # Registros médicos
│   │   │   │   ├── PatientController.java           # Pacientes
│   │   │   │   ├── SubscriptionController.java      # Suscripciones
│   │   │   │   ├── SuperAdminUserController.java    # SuperAdmin users
│   │   │   │   ├── SuperAdminDashboardController.java
│   │   │   │   ├── TenantController.java            # Multi-tenancy
│   │   │   │   ├── TestSecurityController.java      # Testing
│   │   │   │   ├── TriageController.java            # Triaje
│   │   │   │   ├── UserController.java              # Usuarios
│   │   │   │   └── publicapi/                       # APIs públicas
│   │   │   ├── dto/                                 # Data Transfer Objects
│   │   │   │   ├── auth/                            # Auth DTOs
│   │   │   │   ├── request/                         # Request DTOs
│   │   │   │   └── response/                        # Response DTOs
│   │   │   ├── entity/                              # JPA Entities
│   │   │   │   ├── Appointment.java
│   │   │   │   ├── User.java
│   │   │   │   ├── Patient.java
│   │   │   │   ├── Doctor.java
│   │   │   │   ├── MedicalRecord.java
│   │   │   │   ├── Tenant.java
│   │   │   │   └── enums/
│   │   │   ├── mapper/                              # DTOs <-> Entities
│   │   │   ├── repository/                          # Data Access Layer
│   │   │   ├── security/                            # Seguridad
│   │   │   │   ├── authorization/
│   │   │   │   ├── config/
│   │   │   │   └── jwt/
│   │   │   ├── service/                             # Business Logic
│   │   │   │   ├── appointment/
│   │   │   │   ├── auth/
│   │   │   │   ├── user/
│   │   │   │   ├── patient/
│   │   │   │   ├── doctor/
│   │   │   │   ├── tenant/
│   │   │   │   └── ...
│   │   │   ├── types/                               # Custom types/enums
│   │   │   └── util/                                # Utilidades
│   │   └── resources/
│   │       └── application.yml                      # Configuración
│   └── test/
│       └── java/com/medical/medcore/
│           └── MedcoreApiApplicationTests.java
├── pom.xml                                          # Maven config
├── mvnw / mvnw.cmd                                  # Maven wrapper
├── API_DOCUMENTATION.md                             # Docs APIs
├── HELP.md                                          # Ayuda
└── target/                                          # Build output

```

---

## 🗄️ Modelos de Datos Principales

### 1. **User (Usuario)**
Representa un usuario del sistema
```
- id: Long (PK)
- email: String (Unique)
- password: String (Hashed)
- firstName: String
- lastName: String
- role: Role (ENUM: SUPER_ADMIN, ADMIN, DOCTOR, STAFF, PATIENT)
- tenant: Tenant (FK - Multi-tenant)
- status: Boolean
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### 2. **Patient (Paciente)**
Representa un paciente en el sistema
```
- id: Long (PK)
- user: User (FK)
- dateOfBirth: LocalDate
- documentNumber: String
- documentType: DocumentType
- gender: String
- medicalRecords: List<MedicalRecord>
- appointments: List<Appointment>
```

### 3. **Doctor (Médico)**
Representa un médico en el sistema
```
- id: Long (PK)
- user: User (FK)
- license: String (Cédula profesional)
- specialties: List<DoctorSpecialty>
- schedule: List<DoctorSchedule>
- appointments: List<Appointment>
- branch: Branch
```

### 4. **Appointment (Cita Médica)**
Representa una cita médica
```
- id: Long (PK)
- patient: Patient (FK)
- doctor: Doctor (FK)
- appointmentDate: LocalDateTime
- status: AppointmentStatus (PENDING, CONFIRMED, COMPLETED, CANCELLED)
- type: AppointmentType
- notes: String
- reschedules: List<AppointmentReschedule>
```

### 5. **MedicalRecord (Registro Médico)**
Histórico médico del paciente
```
- id: Long (PK)
- patient: Patient (FK)
- entries: List<MedicalEntry>
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### 6. **Tenant (Multi-tenant)**
Representa una organización independiente
```
- id: Long (PK)
- name: String
- domain: String
- status: Boolean
- branches: List<Branch>
- users: List<User>
```

### 7. **Branch (Sucursal)**
Representa una sucursal de una clínica
```
- id: Long (PK)
- tenant: Tenant (FK)
- name: String
- address: String
- phone: String
- doctors: List<Doctor>
```

### 8. **Subscription (Suscripción)**
Gestión de planes y suscripciones
```
- id: Long (PK)
- tenant: Tenant (FK)
- plan: Plan (FK)
- status: SubscriptionStatus
- startDate: LocalDate
- endDate: LocalDate
```

### 9. **Triage (Triaje)**
Evaluación inicial del paciente
```
- id: Long (PK)
- patient: Patient (FK)
- appointment: Appointment (FK)
- symptoms: String
- vitals: String (Signos vitales)
- priority: Integer
```

### 10. **Role (Rol)**
Roles disponibles en el sistema
```
ROLES DISPONIBLES:
- SUPER_ADMIN: Acceso total al sistema
- ADMIN: Administrador de tenant
- DOCTOR: Médico del sistema
- STAFF: Personal de apoyo
- PATIENT: Paciente
```

---

## 🎮 Controladores Disponibles

### 1. **AuthController** (`/auth`)
Gestión de autenticación y autorización

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/auth/register` | POST | Registrar nuevo usuario | ❌ |
| `/auth/login` | POST | Iniciar sesión | ❌ |
| `/auth/refresh` | POST | Refrescar token JWT | ❌ |
| `/auth/logout` | POST | Cerrar sesión | ✅ |

### 2. **UserController** (`/users`)
Gestión de usuarios

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/users` | GET | Listar usuarios | ✅ |
| `/users/{id}` | GET | Obtener usuario por ID | ✅ |
| `/users` | POST | Crear nuevo usuario | ✅ |
| `/users/{id}` | PUT | Actualizar usuario | ✅ |
| `/users/{id}` | DELETE | Eliminar usuario | ✅ |

### 3. **PatientController** (`/patients`)
Gestión de pacientes

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/patients` | GET | Listar pacientes | ✅ |
| `/patients/{id}` | GET | Obtener paciente | ✅ |
| `/patients` | POST | Crear paciente | ✅ |
| `/patients/{id}` | PUT | Actualizar paciente | ✅ |
| `/patients/{id}/medical-records` | GET | Registros médicos | ✅ |

### 4. **DoctorController** (`/doctors`)
Gestión de médicos

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/doctors` | GET | Listar médicos | ✅ |
| `/doctors/{id}` | GET | Obtener médico | ✅ |
| `/doctors` | POST | Crear médico | ✅ |
| `/doctors/{id}/schedule` | GET | Horario del médico | ✅ |

### 5. **AppointmentController** (`/appointments`)
Gestión de citas médicas

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/appointments` | GET | Listar citas | ✅ |
| `/appointments` | POST | Crear cita | ✅ |
| `/appointments/{id}` | GET | Obtener cita | ✅ |
| `/appointments/{id}` | PUT | Actualizar cita | ✅ |
| `/appointments/{id}/reschedule` | POST | Reprogramar cita | ✅ |

### 6. **MedicalRecordController** (`/medical-records`)
Gestión de registros médicos

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/medical-records` | GET | Listar registros | ✅ |
| `/medical-records/{id}` | GET | Obtener registro | ✅ |
| `/medical-records` | POST | Crear registro | ✅ |

### 7. **TriageController** (`/triage`)
Gestión de triaje médico

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/triage` | GET | Listar triajes | ✅ |
| `/triage` | POST | Crear triaje | ✅ |

### 8. **BranchController** (`/branches`)
Gestión de sucursales

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/branches` | GET | Listar sucursales | ✅ |
| `/branches/{id}` | GET | Obtener sucursal | ✅ |
| `/branches` | POST | Crear sucursal | ✅ |

### 9. **TenantController** (`/tenants`)
Gestión multi-tenant

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/tenants` | GET | Listar tenants | ✅ |
| `/tenants` | POST | Crear tenant | ✅ |

### 10. **SubscriptionController** (`/subscriptions`)
Gestión de suscripciones y planes

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/subscriptions` | GET | Listar suscripciones | ✅ |
| `/subscriptions` | POST | Crear suscripción | ✅ |

### 11. **DashboardController** (`/dashboard`)
Dashboard de usuario

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/dashboard` | GET | Dashboard personal | ✅ |

### 12. **SuperAdminDashboardController** (`/super-admin/dashboard`)
Dashboard SuperAdmin

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/super-admin/dashboard` | GET | Stats globales | ✅ (SUPER_ADMIN) |

### 13. **SuperAdminUserController** (`/super-admin/users`)
Gestión de usuarios por SuperAdmin

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/super-admin/users` | GET | Listar usuarios | ✅ (SUPER_ADMIN) |

### 14. **CatalogController** (`/catalogs`)
Catálogos del sistema

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/catalogs/specialties` | GET | Especialidades | ✅ |
| `/catalogs/document-types` | GET | Tipos de documento | ✅ |

### 15. **TestSecurityController** (`/test-security`)
Endpoints para testing de seguridad

| Endpoint | Método | Descripción | Auth |
|----------|--------|-------------|------|
| `/test-security/public` | GET | Endpoint público | ❌ |
| `/test-security/authenticated` | GET | Requiere auth | ✅ |

### 16. **Public API Controller** (`/api/public`)
APIs públicas para integración externa

---

## 🏗️ Arquitectura y Componentes

### Patrón de Arquitectura: **Layered Architecture**

```
┌─────────────────────────────────────┐
│      REST Controllers               │  (Presentation Layer)
├─────────────────────────────────────┤
│      DTOs & Mappers                 │
├─────────────────────────────────────┤
│      Services (Business Logic)      │  (Business Logic Layer)
├─────────────────────────────────────┤
│      Repositories (Data Access)     │  (Data Access Layer)
├─────────────────────────────────────┤
│      JPA Entities & Database        │  (Persistence Layer)
├─────────────────────────────────────┤
│      PostgreSQL                     │
└─────────────────────────────────────┘
```

### Componentes Clave

#### **Controllers**
- Reciben las solicitudes HTTP
- Validan entrada
- Llaman a servicios
- Retornan respuestas

#### **Services**
- Contienen la lógica de negocio
- Realizan validaciones complejas
- Manejan transacciones
- Interactúan con repositorios

#### **Repositories**
- Acceso a datos usando Spring Data JPA
- Queries personalizadas si es necesario
- Implementan operaciones CRUD

#### **DTOs**
- Transfer Objects para comunicación cliente-servidor
- Reducen exposición de entidades
- Validación de entrada
- Transformación de datos

#### **Entities**
- Modelos JPA mapeados a base de datos
- Anotaciones Hibernate
- Relaciones y constraints

#### **Security**
- JWT para autenticación stateless
- RBAC (Role-Based Access Control)
- Filtros de seguridad
- Contexto de seguridad de Spring

---

## 🔐 Seguridad

### Autenticación: JWT (JSON Web Tokens)

**Flow:**
1. Usuario hace login con email/password
2. Sistema valida y genera JWT
3. Cliente incluye JWT en header `Authorization: Bearer {token}`
4. Servidor valida token en cada request

**Configuración JWT:**
```yaml
security:
  jwt:
    secret: ${JWT_SECRET:my-secret-key}
    expiration: 900000                # 15 minutos
    refresh-expiration: 604800000     # 7 días
```

### Autorización: RBAC (Control de Acceso Basado en Roles)

**Roles Disponibles:**
- **SUPER_ADMIN**: Acceso total al sistema
- **ADMIN**: Administrador de su tenant
- **DOCTOR**: Acceso a pacientes y citas
- **STAFF**: Personal de soporte
- **PATIENT**: Acceso a sus propios datos

### Seguridad en Controladores

Anotaciones de Spring Security:
```java
@PreAuthorize("hasRole('ADMIN')")        // Solo admin
@PreAuthorize("hasAnyRole('DOCTOR')")     // Múltiples roles
@PreAuthorize("@customValidator.check()") // Validación custom
```

### Multi-tenancy Security
- Filtrado automático de datos por tenant
- Aislamiento de datos entre organizaciones
- Validación de acceso a recursos

---

## ⚙️ Configuración

### application.yml - Configuración Principal

```yaml
server:
  port: 8080

spring:
  application:
    name: medcore
  
  profiles:
    active: dev              # dev, prod, test
  
  datasource:
    url: jdbc:postgresql://localhost:5432/health_system_db
    username: postgres
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: none         # Gestión manual de schema
    show-sql: false
  
  jackson:
    time-zone: America/Lima

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 900000       # 15 minutos
```

### Variables de Entorno

```bash
# Base de datos
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your-secret-key-min-32-chars

# Otros (opcional)
SPRING_PROFILES_ACTIVE=dev
```

### Perfiles de Configuración
- **dev**: Desarrollo local con logs detallados
- **prod**: Producción con optimizaciones
- **test**: Testing con base de datos en memoria

---

## 👨‍💻 Guía de Desarrollo

### Setup Inicial

```bash
# 1. Clonar repositorio
git clone <repository-url>
cd medcore-api

# 2. Configurar variables de entorno
export DB_PASSWORD=your_password
export JWT_SECRET=your_secret_key

# 3. Crear base de datos
createdb -U postgres health_system_db

# 4. Compilar proyecto
mvn clean install

# 5. Ejecutar aplicación
mvn spring-boot:run

# La API estará disponible en http://localhost:8080
```

### Comandos Maven Útiles

```bash
# Compilar
mvn clean compile

# Tests
mvn test
mvn test -Dtest=NombreTest

# Empacar JAR
mvn clean package

# Ejecutar
mvn spring-boot:run

# Limpiar
mvn clean
```

### Convenciones de Código

#### **Nomenclatura**
- **Paquetes**: `com.medical.medcore.{layer}.{domain}`
- **Clases**: PascalCase (`UserService.java`)
- **Métodos**: camelCase (`getUserById()`)
- **Variables**: camelCase (`userName`)
- **Constantes**: UPPER_CASE (`MAX_USERS`)

#### **DTOs**
- Request: `{Entity}Request` (ej: `CreateUserRequest`)
- Response: `{Entity}Response` (ej: `UserResponse`)

#### **Services**
- Interfaz: `{Domain}Service`
- Implementación: `{Domain}ServiceImpl`

#### **Repositories**
- `{Entity}Repository extends JpaRepository`

### Estructura de un Nuevo Feature

```
feature-name/
├── dto/
│   ├── {Feature}Request.java
│   └── {Feature}Response.java
├── entity/
│   └── {Feature}.java
├── repository/
│   └── {Feature}Repository.java
├── service/
│   ├── {Feature}Service.java
│   └── {Feature}ServiceImpl.java
└── controller/
    └── {Feature}Controller.java
```

### Mapeo de Entidades a DTOs

```java
// Usar MapStruct o similar
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User entity);
    User toEntity(CreateUserRequest request);
}
```

### Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testGetUser() throws Exception {
        mockMvc.perform(get("/users/1"))
               .andExpect(status().isOk());
    }
}
```

---

## 🗄️ Base de Datos

### Configuración de Conexión
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/health_system_db
    username: postgres
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    
  jpa:
    hibernate:
      ddl-auto: none  # Scripts SQL manuales en flyway/liquibase
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Pool de Conexiones (Hikari)
```yaml
hikari:
  maximum-pool-size: 10
  minimum-idle: 5
  idle-timeout: 30000
  connection-timeout: 20000
  max-lifetime: 1800000
```

### Migraciones
- Ejecutar scripts SQL manualmente o
- Usar Flyway/Liquibase para versionamiento

### Indices Recomendados
```sql
-- Users
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_tenant ON users(tenant_id);

-- Appointments
CREATE INDEX idx_appointment_doctor ON appointments(doctor_id);
CREATE INDEX idx_appointment_patient ON appointments(patient_id);
CREATE INDEX idx_appointment_date ON appointments(appointment_date);

-- Tenants
CREATE INDEX idx_tenant_domain ON tenants(domain);
```

---

## 📊 Flujos Principales

### 1. Flujo de Registro
```
POST /auth/register
  ↓
Validar email único
  ↓
Hashear contraseña
  ↓
Crear User + Persona
  ↓
Generar JWT tokens
  ↓
Response: {accessToken, refreshToken}
```

### 2. Flujo de Login
```
POST /auth/login
  ↓
Validar email existe
  ↓
Verificar contraseña
  ↓
Generar JWT tokens
  ↓
Response: {accessToken, refreshToken}
```

### 3. Flujo de Cita Médica
```
POST /appointments
  ↓
Validar paciente + doctor
  ↓
Verificar disponibilidad
  ↓
Crear appointment
  ↓
Registrar en AppointmentFlowHistory
  ↓
Notificar (email/SMS)
  ↓
Response: AppointmentResponse
```

### 4. Flujo Multi-tenant
```
Request con JWT
  ↓
Extraer tenant_id del token
  ↓
Validar acceso a recurso
  ↓
Filtrar datos por tenant
  ↓
Retornar solo datos del tenant
```

---

## 🚀 Deployment

### Build para Producción
```bash
mvn clean package -DskipTests -Pproduction
```

### Docker (Opcional)
```dockerfile
FROM eclipse-temurin:21-jdk
COPY target/medcore-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Requisitos de Producción
- Java 21 JDK
- PostgreSQL 12+
- Variables de entorno configuradas
- Base de datos pre-creada
- Certificados SSL (HTTPS)

---

## 📝 Notas Importantes

- **Timezone**: Configurado para `America/Lima`
- **DDL Management**: `ddl-auto: none` - No auto-update del schema
- **SQL Logging**: Desactivado en dev para mejor rendimiento
- **JWT Expiration**: 15 minutos para access token, 7 días para refresh
- **Pool de conexiones**: Máximo 10 conexiones simultáneas

---

## 🔗 Links Útiles

- [Documentación API](./API_DOCUMENTATION.md)
- [Ayuda General](./HELP.md)
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/)
- [Spring Security](https://docs.spring.io/spring-security/docs/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)

---

**Última actualización**: Mayo 2026  
**Versión**: 0.0.1-SNAPSHOT  
**Autor**: Medical Systems Team
