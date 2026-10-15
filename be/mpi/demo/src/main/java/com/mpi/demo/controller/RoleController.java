package com.mpi.demo.controller;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.dto.request.CreateRoleRequest;
import com.mpi.demo.dto.request.RoleSearchRequest;
import com.mpi.demo.dto.request.UpdateRoleRequest;
import com.mpi.demo.dto.response.RoleResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.RoleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultPagination>> search(
            @ParameterObject RoleSearchRequest search,
            @ParameterObject Pageable pageable) {
        ResultPagination result = roleService.search(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách vai trò thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin vai trò thành công", roleService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse response = roleService.create(request);
        URI location = URI.create(String.format("api/v1/roles/%s", response.id()));
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Tạo vai trò thành công", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<RoleResponse>> update(@Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò thành công", roleService.update(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa vai trò thành công", null));
    }
}
