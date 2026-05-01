package com.medical.medcore.service.user.impl;

import com.medical.medcore.dto.request.ChangePasswordRequest;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.Role;
import com.medical.medcore.entity.User;
import com.medical.medcore.dto.request.CreateUserRequest;
import com.medical.medcore.dto.request.UpdateUserRequest;
import com.medical.medcore.dto.request.UpdateUserStatusRequest;
import com.medical.medcore.dto.response.UserResponse;
import com.medical.medcore.mapper.UserMapper;
import com.medical.medcore.repository.PersonRepository;
import com.medical.medcore.repository.RoleRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.service.auth.RefreshTokenService;
import com.medical.medcore.service.user.UserService;
import com.medical.medcore.util.TenantContext;
import com.medical.medcore.security.authorization.constants.RoleConstants;
import com.medical.medcore.config.exception.BadRequestException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
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

        Long tenantId = TenantContext.getTenantId();

        userRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
                .ifPresent(u -> {
                    throw new RuntimeException("Email ya existe");
                });

        if (request.getPerson().getDocumentTypeCode() == null || request.getPerson().getDocumentNumber() == null) {
            throw new BadRequestException("El tipo y número de documento son obligatorios");
        }

        com.medical.medcore.entity.DocumentType docType = documentTypeRepository.findByCode(request.getPerson().getDocumentTypeCode())
                .orElseThrow(() -> new BadRequestException("Tipo de documento inválido"));

        com.medical.medcore.entity.PersonDocument existingDoc = personDocumentRepository
                .findByDocumentTypeIdAndDocumentNumber(docType.getId(), request.getPerson().getDocumentNumber())
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
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setTenantId(tenantId);
        user.setPerson(person);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = roleRepository.findAllById(request.getRoleIds()).stream()
                    .collect(java.util.stream.Collectors.toSet());

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

            user.setRoles(roles);
        }

        user = userRepository.save(user);

        return UserMapper.toResponse(user);
    }

    @Override
    public List<UserResponse> findAll() {

        Long tenantId = TenantContext.getTenantId();

        return userRepository.findAllByTenantId(tenantId)
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse findById(Long id) {

        Long tenantId = TenantContext.getTenantId();

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("User no encontrado"));

        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest request) {

        Long tenantId = TenantContext.getTenantId();

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("User no encontrado"));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {

            userRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
                    .ifPresent(u -> {
                        throw new RuntimeException("Email ya existe");
                    });

            user.setEmail(request.getEmail());
            user.getPerson().setContactEmail(request.getEmail());
        }

        Person person = user.getPerson();
        person.setFirstName(request.getPerson().getFirstName());
        person.setLastName(request.getPerson().getLastName());
        person.setBirthDate(request.getPerson().getBirthDate());
        person.setGender(request.getPerson().getGender());
        person.setPhone(request.getPerson().getPhone());
        
        boolean hasDocument = personDocumentRepository.existsByPersonId(person.getId());
        person.recalculateProfileCompleted(hasDocument);
        personRepository.save(person);

        if (request.getRoleIds() != null) {
            Set<Role> roles = roleRepository.findAllById(request.getRoleIds()).stream()
                    .collect(java.util.stream.Collectors.toSet());
            user.setRoles(roles);
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void updateStatus(Long id, UpdateUserStatusRequest request) {

        Long tenantId = TenantContext.getTenantId();

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("User no encontrado"));

        user.setIsActive(request.getIsActive());

        userRepository.save(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {

        Long tenantId = TenantContext.getTenantId();
        Long currentUserId = TenantContext.getCurrentUserId();

        if (!userId.equals(currentUserId)) {
            throw new RuntimeException("No autorizado para cambiar este password");
        }

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("User no encontrado"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Password actual incorrecto");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        refreshTokenService.revokeAllByUserIdAndTenantId(userId, tenantId);
    }

}