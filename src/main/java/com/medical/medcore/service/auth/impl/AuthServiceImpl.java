package com.medical.medcore.service.auth.impl;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.auth.RegisterRequest;
import com.medical.medcore.dto.request.LoginRequest;
import com.medical.medcore.dto.response.AuthResponse;
import com.medical.medcore.dto.response.UserMeResponse;
import com.medical.medcore.entity.*;
import com.medical.medcore.entity.enums.TenantStatus;
import com.medical.medcore.repository.*;
import com.medical.medcore.security.jwt.JwtProvider;
import com.medical.medcore.service.auth.AuthService;
import com.medical.medcore.service.auth.RefreshTokenService;
import com.medical.medcore.util.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final PersonRepository personRepository;
    private final PatientRepository patientRepository;
    private final RoleRepository roleRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final DocumentTypeRepository documentTypeRepository;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           TenantRepository tenantRepository,
                           JwtProvider jwtProvider,
                           RefreshTokenService refreshTokenService,
                           PersonRepository personRepository,
                           PatientRepository patientRepository,
                           RoleRepository roleRepository,
                           PersonDocumentRepository personDocumentRepository,
                           DocumentTypeRepository documentTypeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantRepository = tenantRepository;
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.personRepository = personRepository;
        this.patientRepository = patientRepository;
        this.roleRepository = roleRepository;
        this.personDocumentRepository = personDocumentRepository;
        this.documentTypeRepository = documentTypeRepository;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {

        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new BadRequestException("Tenant no resuelto");
        }

        Tenant tenant = tenantRepository
                .findByIdAndStatus(tenantId, TenantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Tenant no encontrado o inactivo"));

        userRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
                .ifPresent(u -> {
                    throw new BadRequestException("Email ya existe");
                });

        if (request.getDocumentTypeCode() == null || request.getDocumentNumber() == null) {
            throw new BadRequestException("El tipo y número de documento son obligatorios");
        }

        DocumentType docType = documentTypeRepository.findByCode(request.getDocumentTypeCode())
                .orElseThrow(() -> new BadRequestException("Tipo de documento inválido"));

        PersonDocument existingDoc = personDocumentRepository
                .findByDocumentTypeIdAndDocumentNumber(docType.getId(), request.getDocumentNumber())
                .orElse(null);

        Person person;

        if (existingDoc != null) {
            person = existingDoc.getPerson();
        } else {
            person = Person.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .birthDate(request.getBirthDate())
                    .contactEmail(request.getEmail())
                    .tenantId(tenantId)
                    .build();

            person.recalculateProfileCompleted(false);
            person = personRepository.save(person);

            PersonDocument document = PersonDocument.builder()
                    .person(person)
                    .documentType(docType)
                    .documentNumber(request.getDocumentNumber())
                    .build();
            personDocumentRepository.save(document);
            person.recalculateProfileCompleted(true);
            personRepository.save(person);
        }

        User existingUser = userRepository
                .findByPersonIdAndTenantId(person.getId(), tenantId)
                .orElse(null);

        if (existingUser != null) {
            throw new BadRequestException("Este documento ya tiene una cuenta. Inicie sesión");
        }
        Patient patient = patientRepository.findByPersonIdAndTenantId(person.getId(), tenantId)
                .orElse(null);

        if (patient == null) {
            patientRepository.save(
                    Patient.builder()
                            .tenantId(tenantId)
                            .person(person)
                            .build()
            );
        }
        Role rolePatient = roleRepository.findByCode("PATIENT")
                .orElseThrow(() -> new BadRequestException("Rol PATIENT no configurado"));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .tenantId(tenantId)
                .person(person)
                .isActive(true)
                .roles(Set.of(rolePatient))
                .build();

        user = userRepository.save(user);

        List<String> roles = extractRoles(user);

        String accessToken = jwtProvider.generateToken(
                user.getId(),
                tenant.getId(),
                roles
        );

        String refreshToken = refreshTokenService.create(
                user.getId(),
                tenant.getId()
        );

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new BadRequestException("Tenant no resuelto");
        }

        Tenant tenant = tenantRepository
                .findByIdAndStatus(tenantId, TenantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Tenant no encontrado o inactivo"));

        User user = userRepository
                .findByEmailAndTenantId(request.getEmail(), tenant.getId())
                .orElseThrow(() -> new BadRequestException("Credenciales inválidas"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Credenciales inválidas");
        }

        List<String> roles = extractRoles(user);

        String accessToken = jwtProvider.generateToken(
                user.getId(),
                tenant.getId(),
                roles
        );

        String refreshToken = refreshTokenService.create(
                user.getId(),
                tenant.getId()
        );

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refresh(String refreshTokenRaw) {

        RefreshToken token = refreshTokenService.validate(refreshTokenRaw);

        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(token.getTenantId())) {
            throw new BadRequestException("Token no pertenece a este tenant");
        }

        Tenant tenant = tenantRepository
                .findByIdAndStatus(token.getTenantId(), TenantStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Tenant inactivo"));

        User user = userRepository
                .findByIdAndTenantId(token.getUserId(), token.getTenantId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Usuario inactivo");
        }

        List<String> roles = extractRoles(user);

        String newAccess = jwtProvider.generateToken(
                user.getId(),
                tenant.getId(),
                roles
        );

        refreshTokenService.revoke(token);

        String newRefresh = refreshTokenService.create(
                user.getId(),
                tenant.getId()
        );

        return new AuthResponse(newAccess, newRefresh);
    }

    @Override
    public void logout(String refreshTokenRaw) {

        try {
            RefreshToken token = refreshTokenService.validate(refreshTokenRaw);
            refreshTokenService.revoke(token);
        } catch (BadRequestException | NotFoundException ignored) {
        }
    }

    @Override
    public UserMeResponse me() {

        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getCurrentUserId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        return UserMeResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .roles(
                        user.getRoles()
                                .stream()
                                .map(role -> role.getName())
                                .collect(java.util.stream.Collectors.toSet())
                )
                .firstName(user.getPerson().getFirstName())
                .lastName(user.getPerson().getLastName())
                .tenantId(tenantId)
                .build();
    }

    private List<String> extractRoles(User user) {
        return user.getRoles()
                .stream()
                .map(r -> r.getCode())
                .toList();
    }
}