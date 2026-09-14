package com.mpi.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.dto.request.CreateSourceSystemRequest;
import com.mpi.demo.dto.request.SourceSystemSearchRequest;
import com.mpi.demo.dto.request.UpdateSourceSystemRequest;
import com.mpi.demo.dto.response.SourceSystemResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.SourceSystemService;

import jakarta.validation.Valid;

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

@RestController
@RequestMapping("/api/v1/source-systems")
public class SourceSystemController {
    private final SourceSystemService sourceSystemService;

    public SourceSystemController(SourceSystemService sourceSystemService) {
        this.sourceSystemService = sourceSystemService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultPagination>> search(
            @ParameterObject SourceSystemSearchRequest search,
            @ParameterObject Pageable pageable) {

        ResultPagination result = sourceSystemService.search(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hệ thống nguồn thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SourceSystemResponse>> getById(@PathVariable Long id) {
        SourceSystemResponse sourceSystem = sourceSystemService.getById(id);
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin hệ thống nguồn thành công", sourceSystem));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SourceSystemResponse>> create(
            @Valid @RequestBody CreateSourceSystemRequest request) {
        SourceSystemResponse response = sourceSystemService.create(request);
        URI location = URI.create(String.format("api/v1/source-systems/%s", response.id()));
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Tạo hệ thống nguồn thành công", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SourceSystemResponse>> update(@Valid @RequestBody UpdateSourceSystemRequest request) {
        SourceSystemResponse response = sourceSystemService.update(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin hệ thống nguồn thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        sourceSystemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa hệ thống nguồn thành công", null));
    }

}
