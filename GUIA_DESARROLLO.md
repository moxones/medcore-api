# 🚀 MedCore API - Guía Práctica de Desarrollo

## 📋 Índice Rápido
1. [Checklist de Setup](#checklist-de-setup)
2. [Patrones Comunes](#patrones-comunes)
3. [Ejemplos de Código](#ejemplos-de-código)
4. [Errores Comunes y Soluciones](#errores-comunes-y-soluciones)
5. [Testing](#testing)
6. [Debugging](#debugging)

---

## ✅ Checklist de Setup

### Primera Vez (One-time setup)
```bash
# 1. Instalar dependencias
export DB_PASSWORD="tu_contraseña_postgres"
export JWT_SECRET="tu_secret_key_de_al_menos_32_caracteres"

# 2. Crear base de datos
psql -U postgres -c "CREATE DATABASE health_system_db;"

# 3. Compilar
mvn clean install

# 4. Ejecutar
mvn spring-boot:run
```

### Cada que se abre el proyecto
```bash
# Terminal 1: Iniciar API
mvn spring-boot:run

# Terminal 2 (Opcional): Ver logs
mvn spring-boot:run -X

# Verificar que esté corriendo
curl http://localhost:8080/test-security/public
```

### Verificar Setup Correcto
```bash
# 1. API respondiendo
curl -X GET http://localhost:8080/test-security/public
# Esperar: {"message":"Public endpoint"}

# 2. Base de datos conectada
psql -U postgres health_system_db -c "SELECT * FROM users LIMIT 1;"

# 3. Logs sin errores
# Buscar: "Started MedcoreApiApplication"
```

---

## 🎯 Patrones Comunes

### Patrón: Crear Nuevo Controlador

```java
// 1. Controller
@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {
    
    private final ItemService itemService;
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> createItem(
            @Valid @RequestBody CreateItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.createItem(request));
    }
}

// 2. Service Interface
@Service
public interface ItemService {
    List<ItemResponse> getAllItems();
    ItemResponse createItem(CreateItemRequest request);
}

// 3. Service Implementation
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    
    @Override
    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll()
                .stream()
                .map(itemMapper::toResponse)
                .toList();
    }
    
    @Override
    @Transactional
    public ItemResponse createItem(CreateItemRequest request) {
        Item item = itemMapper.toEntity(request);
        Item saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }
}

// 4. DTOs
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateItemRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotNull(message = "Price is required")
    @DecimalMin("0.01")
    private BigDecimal price;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {
    private Long id;
    private String name;
    private BigDecimal price;
}

// 5. Repository
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByName(String name);
}

// 6. Entity
@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Patrón: Manejo de Errores

```java
// 1. Custom Exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// 2. Global Exception Handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}

// 3. Error Response DTO
@Data
@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
}
```

### Patrón: Multi-tenancy

```java
// 1. Obtener tenant actual
@Service
@RequiredArgsConstructor
public class TenantContextService {
    
    public Long getCurrentTenantId() {
        // Desde JWT token o contexto de seguridad
        UserPrincipal principal = 
            (UserPrincipal) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.getTenantId();
    }
}

// 2. Filtro automático por tenant
@Entity
@Table(name = "items")
@Data
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    
    private String name;
}

// 3. Repository con filtro
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByTenant(Tenant tenant);
}

// 4. Service con contexto
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    
    private final ItemRepository itemRepository;
    private final TenantContextService tenantContext;
    
    @Override
    public List<Item> getAllItems() {
        Tenant currentTenant = tenantContext.getCurrentTenant();
        return itemRepository.findByTenant(currentTenant);
    }
}
```

### Patrón: Transacciones

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    
    @Override
    @Transactional  // Rollback automático si hay error
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1. Crear orden
        Order order = new Order();
        Order savedOrder = orderRepository.save(order);
        
        // 2. Procesar pago
        PaymentResponse payment = paymentService.process(request);
        
        // 3. Reducir inventario
        inventoryService.reduceStock(request.getItems());
        
        // Si alguna operación falla, todo se revierte
        return mapToResponse(savedOrder);
    }
    
    @Transactional(readOnly = true)  // Solo lectura, más eficiente
    public OrderResponse getOrder(Long id) {
        return orderRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow();
    }
}
```

### Patrón: Paginación

```java
// 1. Controller
@GetMapping
public ResponseEntity<Page<ItemResponse>> getAllItems(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id,desc") String[] sort) {
    
    Sort sortOrder = Sort.by(
        new Sort.Order(Sort.Direction.DESC, sort[0])
    );
    Pageable pageable = PageRequest.of(page, size, sortOrder);
    return ResponseEntity.ok(itemService.getAllItems(pageable));
}

// 2. Service
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    
    private final ItemRepository itemRepository;
    
    public Page<ItemResponse> getAllItems(Pageable pageable) {
        return itemRepository.findAll(pageable)
                .map(this::mapToResponse);
    }
}

// 3. Repository
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    // Ya tiene soporte para Pageable automáticamente
}

// 4. Query
GET /api/v1/items?page=0&size=10&sort=createdAt,desc
```

### Patrón: Validación Personalizada

```java
// 1. Custom Annotation
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
@Documented
public @interface UniqueEmail {
    String message() default "Email already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// 2. Custom Validator
@Component
public class UniqueEmailValidator 
        implements ConstraintValidator<UniqueEmail, String> {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public boolean isValid(String email, 
            ConstraintValidatorContext context) {
        return email == null || !userRepository
                .existsByEmail(email);
    }
}

// 3. Uso en DTO
@Data
public class CreateUserRequest {
    @NotBlank
    @Email
    @UniqueEmail
    private String email;
}
```

---

## 💻 Ejemplos de Código

### Login y Obtener Token JWT

```bash
# 1. Registrarse
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Juan",
    "lastName": "Pérez",
    "email": "juan@example.com",
    "password": "Password123!",
    "documentNumber": "12345678",
    "documentTypeCode": "CC"
  }'

# Respuesta:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
#   "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
# }

# 2. Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "Password123!"
  }'

# 3. Usar token en request
TOKEN="eyJhbGciOiJIUzI1NiJ9..."
curl -X GET http://localhost:8080/users \
  -H "Authorization: Bearer $TOKEN"
```

### Crear Cita Médica

```bash
TOKEN="tu_access_token"

curl -X POST http://localhost:8080/appointments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "patientId": 1,
    "doctorId": 2,
    "appointmentDate": "2024-05-15T10:30:00",
    "type": "CONSULTATION",
    "notes": "Dolor de cabeza persistente"
  }'
```

### Refrescar Token JWT

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "tu_refresh_token"
  }'
```

---

## 🐛 Errores Comunes y Soluciones

### Error 1: "No database connection"
```
Error: org.postgresql.util.PSQLException: Connection refused
```
**Solución:**
```bash
# Verificar PostgreSQL esté corriendo
sudo service postgresql status  # Linux
brew services list             # macOS
services.msc                   # Windows

# Crear BD si no existe
createdb -U postgres health_system_db

# En application.yml verificar:
spring.datasource.url: jdbc:postgresql://localhost:5432/health_system_db
spring.datasource.username: postgres
spring.datasource.password: ${DB_PASSWORD}
```

### Error 2: "JWT token is expired"
```
Error: io.jsonwebtoken.ExpiredJwtException
```
**Solución:**
```bash
# Refrescar token
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "tu_refresh_token"}'

# O hacer login de nuevo
```

### Error 3: "Access Denied"
```
Error: 403 Forbidden - Access Denied
```
**Solución:**
```java
// Verificar en el controller:
@PreAuthorize("hasRole('ADMIN')")  // Usar rol correcto
public ResponseEntity<?> createUser(...) {
    // ...
}

// Verificar JWT contiene el rol correcto
// Verificar usuario tiene ese rol en BD
```

### Error 4: "No such table"
```
Error: ERROR: relation "users" does not exist
```
**Solución:**
```sql
-- Ejecutar SQL de inicialización
-- Script debe crear todas las tablas

-- O cambiar en application.yml:
spring.jpa.hibernate.ddl-auto: create-drop  # Desarrollo
spring.jpa.hibernate.ddl-auto: validate     # Producción
```

### Error 5: "Constraint violation"
```
Error: java.sql.SQLIntegrityConstraintViolationException
```
**Solución:**
```java
// Agregar validaciones en DTO
@Data
public class CreateUserRequest {
    @NotBlank
    @Email
    @UniqueEmail  // Custom validator
    private String email;
}

// Usar @Unique en entidad
@Entity
public class User {
    @Column(unique = true)
    private String email;
}
```

### Error 6: "Cannot autowire"
```
Error: Field injection resulted in NullPointerException
```
**Solución:**
```java
// Usar @RequiredArgsConstructor en lugar de @Autowired
@Service
@RequiredArgsConstructor  // Genera constructor automático
public class UserServiceImpl {
    private final UserRepository userRepository;  // Final es importante
}

// O explícitamente:
@Service
public class UserServiceImpl {
    @Autowired
    private UserRepository userRepository;
    
    // Constructor
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

### Error 7: "Invalid JWT signature"
```
Error: io.jsonwebtoken.SignatureException
```
**Solución:**
```bash
# Verificar que JWT_SECRET es igual en generación y validación
export JWT_SECRET="mi-secret-key-muy-segura-de-32-caracteres"

# En application.yml:
security:
  jwt:
    secret: ${JWT_SECRET}
```

---

## 🧪 Testing

### Test Unitario - Service

```java
@SpringBootTest
public class UserServiceImplTest {
    
    @MockBean
    private UserRepository userRepository;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    public void testGetUserById_Success() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        
        // Act
        User result = userService.getUserById(userId);
        
        // Assert
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).findById(userId);
    }
    
    @Test
    public void testGetUserById_NotFound() {
        // Arrange
        when(userRepository.findById(anyLong()))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, 
                () -> userService.getUserById(999L));
    }
}
```

### Test Integración - Controller

```java
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    public void testGetUser_Success() throws Exception {
        // Arrange
        UserResponse userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail("test@example.com");
        
        when(userService.getUserById(1L))
                .thenReturn(userResponse);
        
        // Act & Assert
        mockMvc.perform(
                get("/users/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                    .value("test@example.com"));
    }
    
    @Test
    public void testCreateUser_InvalidEmail() throws Exception {
        mockMvc.perform(
                post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest());
    }
}
```

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Test específico
mvn test -Dtest=UserServiceImplTest

# Test específico con método
mvn test -Dtest=UserServiceImplTest#testGetUserById_Success

# Con logs
mvn test -X

# Generar reporte
mvn test jacoco:report
```

---

## 🔍 Debugging

### Activar Logs Detallados

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.medical.medcore: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Breakpoints en IntelliJ

```
1. Click en el número de línea (left margin)
2. Run → Debug 'MedcoreApiApplication'
3. Usar variables panel para inspeccionar
4. Step Over (F10), Step Into (F11), Resume (F8)
```

### Print Debugging

```java
// En el código
System.out.println("DEBUG: " + variable);
logger.debug("User created: {}", user);

// Luego buscar en logs
[main] com.medical.medcore.service.UserServiceImpl : User created: User(id=1, email=test@example.com)
```

### curl con Logs

```bash
# Ver headers
curl -v http://localhost:8080/test-security/public

# Ver body
curl -v -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

### Inspeccionar Token JWT

```bash
# Copiar el token y ir a
# https://jwt.io

# Pegar el token para ver su contenido
# Header, Payload, Signature
```

---

**¡Éxito en el desarrollo!** 🎉
