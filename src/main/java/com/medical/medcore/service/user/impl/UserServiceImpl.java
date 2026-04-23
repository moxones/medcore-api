package com.medical.medcore.service.user.impl;

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
import com.medical.medcore.service.user.UserService;
import com.medical.medcore.util.TenantContext;

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

    public UserServiceImpl(UserRepository userRepository,
                           PersonRepository personRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse create(CreateUserRequest request) {

        Long tenantId = TenantContext.getTenantId();

        userRepository.findByEmailAndTenantId(request.getEmail(), tenantId)
                .ifPresent(u -> { throw new RuntimeException("Email ya existe"); });

        Person person = new Person();
        person.setTenantId(tenantId);
        person.setFirstName(request.getPerson().getFirstName());
        person.setLastName(request.getPerson().getLastName());
        person.setBirthDate(request.getPerson().getBirthDate());
        person.setGender(request.getPerson().getGender());
        person.setPhone(request.getPerson().getPhone());
        person.setEmail(request.getEmail());
        person.setIsActive(true);

        person = personRepository.save(person);

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setTenantId(tenantId);
        user.setPerson(person);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = roleRepository.findAllById(request.getRoleIds()).stream().collect(java.util.stream.Collectors.toSet());
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
                    .ifPresent(u -> { throw new RuntimeException("Email ya existe"); });

            user.setEmail(request.getEmail());
            user.getPerson().setEmail(request.getEmail());
        }

        Person person = user.getPerson();
        person.setFirstName(request.getPerson().getFirstName());
        person.setLastName(request.getPerson().getLastName());
        person.setBirthDate(request.getPerson().getBirthDate());
        person.setGender(request.getPerson().getGender());
        person.setPhone(request.getPerson().getPhone());

        if (request.getRoleIds() != null) {
            Set<Role> roles = roleRepository.findAllById(request.getRoleIds()).stream().collect(java.util.stream.Collectors.toSet());
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
        user.getPerson().setIsActive(request.getIsActive());

        userRepository.save(user);
    }


}