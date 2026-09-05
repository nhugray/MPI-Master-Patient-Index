package com.mpi.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.dto.request.CreateFacilityRequest;
import com.mpi.demo.dto.request.FacilitySearchRequest;
import com.mpi.demo.dto.request.UpdateFacilityRequest;
import com.mpi.demo.dto.response.FacilityResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.FacilityService;

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
@RequestMapping("/api/v1/facilities")
public class FacilityController {
    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultPagination>> search(
            @ParameterObject FacilitySearchRequest search,
            @ParameterObject Pageable pageable) {

        ResultPagination result = facilityService.search(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cơ sở y tế thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacilityResponse>> getById(@PathVariable Long id) {
        FacilityResponse facility = facilityService.getById(id);
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin cơ sở y tế thành công", facility));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FacilityResponse>> create(
            @Valid @RequestBody CreateFacilityRequest request) {
        FacilityResponse response = facilityService.create(request);
        URI location = URI.create(String.format("api/v1/facilities/%s", response.id()));
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Tạo cơ sở y tế thành công", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<FacilityResponse>> update(@Valid @RequestBody UpdateFacilityRequest request) {
        FacilityResponse response = facilityService.update(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin cơ sở y tế thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        facilityService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cơ sở y tế thành công", null));
    }

}
