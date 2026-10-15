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

import com.mpi.demo.dto.request.CreateUserRequest;
import com.mpi.demo.dto.request.UpdateUserRequest;
import com.mpi.demo.dto.request.UserSearchRequest;
import com.mpi.demo.dto.response.UserResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultPagination>> search(
            @ParameterObject UserSearchRequest search,
            @ParameterObject Pageable pageable) {
        ResultPagination result = userService.search(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", userService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        URI location = URI.create(String.format("api/v1/users/%s", response.id()));
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Tạo người dùng thành công", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> update(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", userService.update(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công", null));
    }
}
