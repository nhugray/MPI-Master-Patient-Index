package com.mpi.demo.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.UserStatusEnum;
import com.mpi.demo.dto.request.CreateUserRequest;
import com.mpi.demo.dto.request.UpdateUserRequest;
import com.mpi.demo.dto.request.UserSearchRequest;
import com.mpi.demo.dto.response.UserResponse;
import com.mpi.demo.entity.Role;
import com.mpi.demo.entity.User;
import com.mpi.demo.exception.DuplicateResourceException;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.RoleRepository;
import com.mpi.demo.repository.UserRepository;
import com.mpi.demo.service.UserService;
import com.mpi.demo.specification.UserSpecification;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPagination search(UserSearchRequest search, Pageable pageable) {
        Specification<User> spec = UserSpecification.build(search);
        Page<UserResponse> pageResult = userRepository.findAll(spec, pageable)
                .map(UserResponse::fromEntity);
        return ResultPagination.fromPage(pageResult);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.getEmail().trim();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Người dùng", "email", email);
        }
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAddress(request.getAddress());
        user.setGender(request.getGender() != null ? request.getGender() : GenderEnum.OTHER);
        user.setAvatar(request.getAvatar());
        user.setStatus(request.getStatus() != null ? request.getStatus() : UserStatusEnum.ACTIVE);
        user.setRoles(resolveRoles(request.getRoleIds()));
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse update(UpdateUserRequest request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", request.getId()));

        String email = request.getEmail().trim();
        if (userRepository.existsByEmailAndIdNot(email, request.getId())) {
            throw new DuplicateResourceException("Người dùng", "email", email);
        }

        user.setName(request.getName().trim());
        user.setEmail(email);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setAddress(request.getAddress());
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        user.setAvatar(request.getAvatar());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getRoleIds() != null) {
            user.setRoles(resolveRoles(request.getRoleIds()));
        }
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getUserCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("TOTAL", userRepository.count());
        counts.put("ACTIVE", userRepository.countByStatus(UserStatusEnum.ACTIVE));
        counts.put("INACTIVE", userRepository.countByStatus(UserStatusEnum.INACTIVE));
        counts.put("SUSPENDED", userRepository.countByStatus(UserStatusEnum.SUSPENDED));
        counts.put("ADMIN", userRepository.countByRoleName("ADMIN"));
        return counts;
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> found = new HashSet<>();
            roles.forEach(r -> found.add(r.getId()));
            Long missing = roleIds.stream().filter(i -> !found.contains(i)).findFirst().orElse(null);
            throw new ResourceNotFoundException("Vai trò", "id", missing);
        }
        return new HashSet<>(roles);
    }
}
