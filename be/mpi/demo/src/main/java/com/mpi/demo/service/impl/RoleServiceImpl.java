package com.mpi.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.dto.request.CreateRoleRequest;
import com.mpi.demo.dto.request.RoleSearchRequest;
import com.mpi.demo.dto.request.UpdateRoleRequest;
import com.mpi.demo.dto.response.RoleResponse;
import com.mpi.demo.entity.Role;
import com.mpi.demo.exception.DuplicateResourceException;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.RoleRepository;
import com.mpi.demo.service.RoleService;
import com.mpi.demo.specification.RoleSpecification;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPagination search(RoleSearchRequest search, Pageable pageable) {
        Specification<Role> spec = RoleSpecification.build(search);
        Page<RoleResponse> pageResult = roleRepository.findAll(spec, pageable)
                .map(RoleResponse::fromEntity);
        return ResultPagination.fromPage(pageResult);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", "id", id));
        return RoleResponse.fromEntity(role);
    }

    @Override
    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        String name = request.getName().trim();
        if (roleRepository.existsByName(name)) {
            throw new DuplicateResourceException("Vai trò", "tên", name);
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(request.getDescription());
        return RoleResponse.fromEntity(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse update(UpdateRoleRequest request) {
        Role role = roleRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", "id", request.getId()));
        String name = request.getName().trim();
        if (roleRepository.existsByNameAndIdNot(name, request.getId())) {
            throw new DuplicateResourceException("Vai trò", "tên", name);
        }
        role.setName(name);
        role.setDescription(request.getDescription());
        return RoleResponse.fromEntity(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vai trò", "id", id));
        roleRepository.delete(role);
    }
}
