package com.mpi.demo.service;

import org.springframework.data.domain.Pageable;

import com.mpi.demo.dto.request.CreateRoleRequest;
import com.mpi.demo.dto.request.RoleSearchRequest;
import com.mpi.demo.dto.request.UpdateRoleRequest;
import com.mpi.demo.dto.response.RoleResponse;
import com.mpi.demo.helper.ResultPagination;

public interface RoleService {

    RoleResponse create(CreateRoleRequest request);

    RoleResponse update(UpdateRoleRequest request);

    RoleResponse getById(Long id);

    ResultPagination search(RoleSearchRequest search, Pageable pageable);

    void delete(Long id);
}
