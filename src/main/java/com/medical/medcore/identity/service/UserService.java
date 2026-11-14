package com.medical.medcore.identity.service;

import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.identity.domain.Role;
import com.medical.medcore.identity.domain.User;
import com.medical.medcore.identity.dto.CreateSuperAdminUserRequest;
import com.medical.medcore.identity.dto.CreateUserRequest;
import com.medical.medcore.identity.dto.PersonRequest;
import com.medical.medcore.identity.dto.RoleResponse;
import com.medical.medcore.identity.dto.UpdateUserRequest;
import com.medical.medcore.identity.dto.UserResponse;
import com.medical.medcore.identity.repository.RoleRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.platform.repository.TenantRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.RoleConstants;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String USER_NOT_FOUND = "Usuario no encontrado";
    private static final Set<String> PRIVILEGED_ROLES = Set.of(RoleConstants.SUPER_ADMIN, RoleConstants.ADMIN);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PersonRegistry personRegistry;
    private final UserAccounts userAccounts;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Set<Role> roles = resolveRoles(request.roleIds(), request.roles());
        assertCanGrant(roles);
        return createAccount(tenantId, request.email(), request.password(), request.person(), roles);
    }

    @Transactional
    public UserResponse createForTenant(CreateSuperAdminUserRequest request) {
        tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new NotFoundException("Organización no encontrada"));
        Set<Role> roles = resolveRoles(request.roleIds(), request.roles());
        return createAccount(request.tenantId(), request.email(), request.password(), request.person(), roles);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAllByTenantIdOrderByIdAsc(TenantContext.requireTenantId()).stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    public List<UserResponse> findAllForPlatform(Long tenantId) {
        List<User> users = tenantId == null
                ? userRepository.findAllByOrderByTenantIdAscIdAsc()
                : userRepository.findAllByTenantIdOrderByIdAsc(tenantId);
        return users.stream().map(UserMapper::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return UserMapper.toResponse(loadVisible(id));
    }

    public List<RoleResponse> listAssignableRoles() {
        return roleRepository.findAll().stream()
                .filter(role -> SecurityUtils.isSuperAdmin() || !PRIVILEGED_ROLES.contains(role.getCode()))
                .sorted(Comparator.comparing(Role::getId))
                .map(role -> new RoleResponse(role.getId(), role.getCode(), role.getName()))
                .toList();
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = loadManageable(id, true);
        applyEmailChange(user, request.email());
        if (request.person() != null) {
            personRegistry.applyChanges(user.getPerson(), UserMapper.toPersonData(request.person(), null));
        }
        if (request.roleIds() != null || request.roles() != null) {
            Set<Role> roles = resolveRoles(request.roleIds(), request.roles());
            if (!sameRoles(user.getRoles(), roles)) {
                replaceRoles(user, roles);
            }
        }
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void updateStatus(Long id, boolean active) {
        User user = loadManageable(id, false);
        if (!active) {
            assertNotSelf(user, "No puedes desactivar tu propia cuenta");
        }
        user.setIsActive(active);
        if (active) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }
        userRepository.save(user);
        if (!active) {
            refreshTokenService.revokeAll(user.getId(), user.getTenantId());
        }
    }

    @Transactional
    public UserResponse assignRoles(Long id, List<Long> roleIds) {
        User user = loadManageable(id, false);
        replaceRoles(user, resolveRoles(roleIds, null));
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void setPassword(Long id, String newPassword) {
        User user = loadManageable(id, false);
        boolean resetByAnotherUser = !user.getId().equals(TenantContext.getCurrentUserId());
        user.changePassword(userAccounts.encode(newPassword), LocalDateTime.now(), resetByAnotherUser);
        userRepository.save(user);
        refreshTokenService.revokeAll(user.getId(), user.getTenantId());
    }

    private UserResponse createAccount(Long tenantId, String email, String password, PersonRequest personRequest,
                                       Set<Role> roles) {
        String normalizedEmail = userAccounts.requireEmail(email);
        userAccounts.assertEmailAvailable(normalizedEmail, tenantId);
        PersonData data = UserMapper.toPersonData(personRequest, normalizedEmail);
        personRegistry.requireName(data.firstName(), "El nombre");
        personRegistry.requireName(data.lastName(), "El apellido");
        if (!data.hasDocument()) {
            throw new BadRequestException("El tipo y número de documento son obligatorios");
        }
        Person person = personRegistry.findByDocument(tenantId, data.documentTypeCode(), data.documentNumber())
                .map(existing -> reuseExistingPerson(existing, tenantId))
                .orElseGet(() -> personRegistry.create(tenantId, data));
        User user = userAccounts.create(tenantId, person, normalizedEmail, password, roles);
        publishRoles(user);
        return UserMapper.toResponse(user);
    }

    private Person reuseExistingPerson(Person existing, Long tenantId) {
        if (userRepository.existsByPersonIdAndTenantId(existing.getId(), tenantId)) {
            throw new ConflictException("DOCUMENT_ALREADY_REGISTERED",
                    "Ya existe un usuario registrado con ese documento");
        }
        return existing;
    }

    private void applyEmailChange(User user, String requestedEmail) {
        String email = PersonRegistry.normalizeEmail(requestedEmail);
        if (email == null || email.equals(user.getEmail())) {
            return;
        }
        userAccounts.assertEmailAvailable(email, user.getTenantId());
        Person person = user.getPerson();
        if (Objects.equals(person.getContactEmail(), user.getEmail())) {
            person.setContactEmail(email);
        }
        user.setEmail(email);
    }

    private void replaceRoles(User user, Set<Role> roles) {
        if (roles.isEmpty()) {
            throw new BadRequestException("Debe asignar al menos un rol");
        }
        assertNotSelf(user, "No puedes modificar tus propios roles");
        assertCanGrant(roles);
        user.setRoles(new HashSet<>(roles));
        publishRoles(user);
    }

    private void publishRoles(User user) {
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        eventPublisher.publishEvent(new UserRolesAssignedEvent(user.getTenantId(), user.getPerson().getId(), codes));
    }

    private User loadVisible(Long id) {
        if (SecurityUtils.isSuperAdmin()) {
            return userRepository.findById(id).orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        }
        return userRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
    }

    private User loadManageable(Long id, boolean allowSelf) {
        User user = loadVisible(id);
        if (SecurityUtils.isSuperAdmin()) {
            return user;
        }
        boolean self = user.getId().equals(TenantContext.getCurrentUserId());
        boolean privilegedTarget = user.getRoles().stream().anyMatch(role -> PRIVILEGED_ROLES.contains(role.getCode()));
        if (privilegedTarget && !(self && allowSelf)) {
            throw new ForbiddenException("No tienes permisos para administrar cuentas de administrador");
        }
        return user;
    }

    private void assertNotSelf(User user, String message) {
        if (user.getId().equals(TenantContext.getCurrentUserId())) {
            throw new BadRequestException(message);
        }
    }

    private void assertCanGrant(Set<Role> roles) {
        if (SecurityUtils.isSuperAdmin()) {
            return;
        }
        if (roles.stream().anyMatch(role -> PRIVILEGED_ROLES.contains(role.getCode()))) {
            throw new ForbiddenException("No tienes permisos para asignar roles de administrador");
        }
    }

    private boolean sameRoles(Set<Role> current, Set<Role> requested) {
        Set<Long> currentIds = current.stream().map(Role::getId).collect(Collectors.toSet());
        Set<Long> requestedIds = requested.stream().map(Role::getId).collect(Collectors.toSet());
        return currentIds.equals(requestedIds);
    }

    private Set<Role> resolveRoles(List<Long> roleIds, List<String> roleCodes) {
        Set<Role> roles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Long> uniqueIds = new HashSet<>(roleIds);
            List<Role> found = roleRepository.findAllById(uniqueIds);
            if (found.size() != uniqueIds.size()) {
                throw new BadRequestException("Uno o más roles no existen");
            }
            roles.addAll(found);
        }
        if (roleCodes != null && !roleCodes.isEmpty()) {
            Set<String> uniqueCodes = roleCodes.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(code -> code.trim().toUpperCase())
                    .collect(Collectors.toSet());
            List<Role> found = roleRepository.findByCodeIn(uniqueCodes);
            if (found.size() != uniqueCodes.size()) {
                throw new BadRequestException("Uno o más roles no existen");
            }
            roles.addAll(found);
        }
        return roles;
    }
}
