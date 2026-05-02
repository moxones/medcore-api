package com.medical.medcore.service.user.impl;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.*;
import com.medical.medcore.dto.response.UserResponse;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.Role;
import com.medical.medcore.entity.User;
import com.medical.medcore.mapper.UserMapper;
import com.medical.medcore.repository.PersonRepository;
import com.medical.medcore.repository.RoleRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import com.medical.medcore.service.auth.RefreshTokenService;
import com.medical.medcore.service.user.UserService;
import com.medical.medcore.util.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final com.medical.medcore.repository.PersonDocumentRepository personDocumentRepository;
    private final com.medical.medcore.repository.DocumentTypeRepository documentTypeRepository;

    public UserServiceImpl(UserRepository userRepository,
                           PersonRepository personRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           RefreshTokenService refreshTokenService,
                           com.medical.medcore.repository.PersonDocumentRepository personDocumentRepository,
                           com.medical.medcore.repository.DocumentTypeRepository documentTypeRepository) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.personDocumentRepository = personDocumentRepository;
        this.documentTypeRepository = documentTypeRepository;
    }

    @Override
    public UserResponse create(CreateUserRequest request) {

        Long tenantId = TenantContext.requireTenantId();

        ensureEmailAvailable(request.getEmail(), tenantId, "Email ya existe");

        if (request.getPerson().getDocumentTypeCode() == null || request.getPerson().getDocumentNumber() == null) {
            throw new BadRequestException("El tipo y número de documento son obligatorios");
        }

        com.medical.medcore.entity.DocumentType docType = findDocumentTypeOrThrow(request.getPerson().getDocumentTypeCode());

        com.medical.medcore.entity.PersonDocument existingDoc = personDocumentRepository
                .findByDocumentTypeIdAndDocumentNumberAndTenantId(docType.getId(), request.getPerson().getDocumentNumber(), tenantId)
                .orElse(null);

        Person person;

        if (existingDoc != null) {
            person = existingDoc.getPerson();
        } else {
            person = new Person();
            person.setTenantId(tenantId);
            person.setFirstName(request.getPerson().getFirstName());
            person.setLastName(request.getPerson().getLastName());
            person.setBirthDate(request.getPerson().getBirthDate());
            person.setGender(request.getPerson().getGender());
            person.setPhone(request.getPerson().getPhone());
            person.setContactEmail(request.getEmail());

            person.recalculateProfileCompleted(true);
            person = personRepository.save(person);

            com.medical.medcore.entity.PersonDocument document = com.medical.medcore.entity.PersonDocument.builder()
                    .person(person)
                    .documentType(docType)
                    .documentNumber(request.getPerson().getDocumentNumber())
                    .build();
            personDocumentRepository.save(document);
            person.getDocuments().add(document);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setTenantId(tenantId);
        user.setPerson(person);

        Set<Role> roles = resolveRoles(request.getRoleIds(), request.getRoles());

        if (!roles.isEmpty()) {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                boolean isSuperAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> RoleConstants.SUPER_ADMIN.equals(a.getAuthority()));
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> RoleConstants.ADMIN.equals(a.getAuthority()));

                if (!isSuperAdmin && isAdmin) {
                    boolean hasRestrictedRole = roles.stream()
                            .anyMatch(role -> RoleConstants.SUPER_ADMIN.equals(role.getCode()) ||
                                    RoleConstants.ADMIN.equals(role.getCode()));
                    if (hasRestrictedRole) {
                        throw new BadRequestException("No tienes permisos para asignar roles de administrador");
                    }
                }
            }
        }

        user.setRoles(roles);

        user = userRepository.save(user);

        return UserMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> findAll() {

        Long tenantId = TenantContext.requireTenantId();

        return userRepository.findAllByTenantId(tenantId)
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest request) {

        Long tenantId = TenantContext.getTenantId();

        User user;
        Long targetTenantId;

        if (isCurrentUserSuperAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
            targetTenantId = user.getTenantId();
        } else {
            if (tenantId == null) {
                throw new BadRequestException("Tenant no disponible");
            }
            user = findUserByIdAndTenantOrThrow(id, tenantId);
            targetTenantId = tenantId;
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {

            ensureEmailAvailable(request.getEmail(), targetTenantId, "Email ya existe");

            user.setEmail(request.getEmail());
            user.getPerson().setContactEmail(request.getEmail());
        }

        Person person = user.getPerson();
        person.setFirstName(request.getPerson().getFirstName());
        person.setLastName(request.getPerson().getLastName());
        person.setBirthDate(request.getPerson().getBirthDate());
        person.setGender(request.getPerson().getGender());
        person.setPhone(request.getPerson().getPhone());

        if (request.getPerson().getDocumentTypeCode() != null && request.getPerson().getDocumentNumber() != null) {
            com.medical.medcore.entity.DocumentType docType = findDocumentTypeOrThrow(request.getPerson().getDocumentTypeCode());

            com.medical.medcore.entity.PersonDocument doc = person.getDocuments().isEmpty() ? null : person.getDocuments().get(0);

            if (doc != null) {
                doc.setDocumentType(docType);
                doc.setDocumentNumber(request.getPerson().getDocumentNumber());
                personDocumentRepository.save(doc);
            } else {
                doc = com.medical.medcore.entity.PersonDocument.builder()
                        .person(person)
                        .documentType(docType)
                        .documentNumber(request.getPerson().getDocumentNumber())
                        .build();
                personDocumentRepository.save(doc);
                person.getDocuments().add(doc);
            }
        }

        boolean hasDocument = !person.getDocuments().isEmpty();
        person.recalculateProfileCompleted(hasDocument);
        personRepository.save(person);

        if (request.getRoleIds() != null || request.getRoles() != null) {
            Set<Role> newRoles = resolveRoles(request.getRoleIds(), request.getRoles());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                boolean isSuperAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> RoleConstants.SUPER_ADMIN.equals(a.getAuthority()));
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> RoleConstants.ADMIN.equals(a.getAuthority()));

                if (!isSuperAdmin && isAdmin) {
                    boolean hasRestrictedRole = newRoles.stream()
                            .anyMatch(role -> RoleConstants.SUPER_ADMIN.equals(role.getCode()) ||
                                    RoleConstants.ADMIN.equals(role.getCode()));
                    if (hasRestrictedRole) {
                        throw new BadRequestException("No tienes permisos para asignar roles de administrador");
                    }
                }
            }

            user.setRoles(newRoles);
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateForSuperAdmin(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Long targetTenantId = user.getTenantId();

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            ensureEmailAvailable(request.getEmail(), targetTenantId, "Email ya existe");
            user.setEmail(request.getEmail());
            user.getPerson().setContactEmail(request.getEmail());
        }

        Person person = user.getPerson();
        if (request.getPerson() != null) {
            person.setFirstName(request.getPerson().getFirstName());
            person.setLastName(request.getPerson().getLastName());
            person.setBirthDate(request.getPerson().getBirthDate());
            person.setGender(request.getPerson().getGender());
            person.setPhone(request.getPerson().getPhone());

            if (request.getPerson().getDocumentTypeCode() != null && request.getPerson().getDocumentNumber() != null) {
                com.medical.medcore.entity.DocumentType docType = findDocumentTypeOrThrow(request.getPerson().getDocumentTypeCode());
                com.medical.medcore.entity.PersonDocument doc = person.getDocuments().isEmpty() ? null : person.getDocuments().get(0);
                if (doc != null) {
                    doc.setDocumentType(docType);
                    doc.setDocumentNumber(request.getPerson().getDocumentNumber());
                    personDocumentRepository.save(doc);
                } else {
                    doc = com.medical.medcore.entity.PersonDocument.builder()
                            .person(person)
                            .documentType(docType)
                            .documentNumber(request.getPerson().getDocumentNumber())
                            .build();
                    personDocumentRepository.save(doc);
                    person.getDocuments().add(doc);
                }
            }
        }

        boolean hasDocument = !person.getDocuments().isEmpty();
        person.recalculateProfileCompleted(hasDocument);
        personRepository.save(person);

        if (request.getRoleIds() != null || request.getRoles() != null) {
            Set<Role> newRoles = resolveRoles(request.getRoleIds(), request.getRoles());
            user.setRoles(newRoles);
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void updateStatus(Long id, UpdateUserStatusRequest request) {

        Long tenantId = TenantContext.getTenantId();

        User user;
        if (isCurrentUserSuperAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        } else {
            if (tenantId == null) {
                throw new BadRequestException("Tenant no disponible");
            }
            user = findUserByIdAndTenantOrThrow(id, tenantId);
        }

        user.setIsActive(request.getIsActive());

        userRepository.save(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {

        Long tenantId = TenantContext.requireTenantId();
        Long currentUserId = TenantContext.requireCurrentUserId();

        if (!userId.equals(currentUserId)) {
            throw new AccessDeniedException("No autorizado para cambiar este password");
        }

        User user = findUserByIdAndTenantOrThrow(userId, tenantId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Password actual incorrecto");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        refreshTokenService.revokeAllByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public UserResponse assignRoles(Long id, List<Long> roleIds) {
        Long tenantId = TenantContext.getTenantId();

        User user;
        if (isCurrentUserSuperAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        } else {
            if (tenantId == null) {
                throw new BadRequestException("Tenant no disponible");
            }
            user = findUserByIdAndTenantOrThrow(id, tenantId);
        }

        Set<Role> newRoles = roleRepository.findAllById(roleIds).stream()
                .collect(java.util.stream.Collectors.toSet());

        if (newRoles.size() != roleIds.size()) {
            throw new BadRequestException("Uno o más roleIds no existen");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> RoleConstants.SUPER_ADMIN.equals(a.getAuthority()));
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> RoleConstants.ADMIN.equals(a.getAuthority()));

            if (!isSuperAdmin && isAdmin) {
                boolean hasRestrictedRole = newRoles.stream()
                        .anyMatch(role -> RoleConstants.SUPER_ADMIN.equals(role.getCode()) ||
                                RoleConstants.ADMIN.equals(role.getCode()));
                if (hasRestrictedRole) {
                    throw new BadRequestException("No tienes permisos para asignar roles de administrador");
                }
            }
        }

        user.setRoles(newRoles);

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse createForTenant(CreateSuperAdminUserRequest request) {

        Long tenantId = request.getTenantId();

        ensureEmailAvailable(request.getEmail(), tenantId, "Email ya existe en ese tenant");

        if (request.getPerson().getDocumentTypeCode() == null || request.getPerson().getDocumentNumber() == null) {
            throw new BadRequestException("El tipo y número de documento son obligatorios");
        }

        com.medical.medcore.entity.DocumentType docType = findDocumentTypeOrThrow(request.getPerson().getDocumentTypeCode());

        com.medical.medcore.entity.PersonDocument existingDoc = personDocumentRepository
                .findByDocumentTypeIdAndDocumentNumberAndTenantId(docType.getId(), request.getPerson().getDocumentNumber(), tenantId)
                .orElse(null);

        Person person;

        if (existingDoc != null) {
            person = existingDoc.getPerson();
        } else {
            person = new Person();
            person.setTenantId(tenantId);
            person.setFirstName(request.getPerson().getFirstName());
            person.setLastName(request.getPerson().getLastName());
            person.setBirthDate(request.getPerson().getBirthDate());
            person.setGender(request.getPerson().getGender());
            person.setPhone(request.getPerson().getPhone());
            person.setContactEmail(request.getEmail());

            person.recalculateProfileCompleted(true);
            person = personRepository.save(person);

            com.medical.medcore.entity.PersonDocument document = com.medical.medcore.entity.PersonDocument.builder()
                    .person(person)
                    .documentType(docType)
                    .documentNumber(request.getPerson().getDocumentNumber())
                    .build();
            personDocumentRepository.save(document);
            person.getDocuments().add(document);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setTenantId(tenantId);
        user.setPerson(person);

        user.setRoles(resolveRoles(request.getRoleIds(), request.getRoles()));

        user = userRepository.save(user);

        return UserMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> findAllByTenant(Long tenantId) {
        List<User> users;
        if (tenantId == null) {
            users = userRepository.findAll();
        } else {
            users = userRepository.findAllByTenantId(tenantId);
        }

        return users
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse findById(Long id) {
        Long tenantId = TenantContext.getTenantId();

        User user;
        if (isCurrentUserSuperAdmin()) {
            user = userRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        } else {
            if (tenantId == null) {
                throw new BadRequestException("Tenant no disponible");
            }
            user = findUserByIdAndTenantOrThrow(id, tenantId);
        }

        return UserMapper.toResponse(user);
    }

    private User findUserByIdAndTenantOrThrow(Long id, Long tenantId) {
        return userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private User findAccessibleUserByIdOrThrow(Long id, Long tenantId) {
        if (isCurrentUserSuperAdmin()) {
            return userRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        }

        return findUserByIdAndTenantOrThrow(id, tenantId);
    }

    private void ensureEmailAvailable(String email, Long tenantId, String message) {
        userRepository.findByEmailAndTenantId(email, tenantId)
                .ifPresent(u -> {
                    throw new BadRequestException(message);
                });
    }

    private com.medical.medcore.entity.DocumentType findDocumentTypeOrThrow(String code) {
        return documentTypeRepository.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Tipo de documento inválido"));
    }

    private Set<Role> resolveRoles(List<Long> roleIds, List<String> roleCodes) {
        boolean hasRoleIds = roleIds != null && !roleIds.isEmpty();
        boolean hasRoleCodes = roleCodes != null && !roleCodes.isEmpty();

        if (!hasRoleIds && !hasRoleCodes) {
            return new HashSet<>();
        }

        Set<Role> roles = new HashSet<>();

        if (hasRoleIds) {
            Set<Long> uniqueRoleIds = new java.util.HashSet<>(roleIds);
            Set<Role> rolesById = roleRepository.findAllById(uniqueRoleIds).stream()
                    .collect(java.util.stream.Collectors.toSet());

            if (rolesById.size() != uniqueRoleIds.size()) {
                throw new BadRequestException("Uno o más roleIds no existen");
            }

            roles.addAll(rolesById);
        }

        if (hasRoleCodes) {
            Set<String> uniqueRoleCodes = roleCodes.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.toSet());

            if (uniqueRoleCodes.isEmpty()) {
                return roles;
            }

            Set<Role> rolesByCode = roleRepository.findByCodeIn(uniqueRoleCodes).stream()
                    .collect(java.util.stream.Collectors.toSet());

            if (rolesByCode.size() != uniqueRoleCodes.size()) {
                throw new BadRequestException("Uno o más roles no existen");
            }

            roles.addAll(rolesByCode);
        }

        return roles;
    }

    private boolean isCurrentUserSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> RoleConstants.SUPER_ADMIN.equals(a.getAuthority()));
    }

}
