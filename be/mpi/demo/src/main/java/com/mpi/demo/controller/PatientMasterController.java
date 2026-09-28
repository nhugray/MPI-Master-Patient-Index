package com.mpi.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientMasterRequest;
import com.mpi.demo.dto.response.PatientMasterResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.PatientMasterService;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/patient-masters")
public class PatientMasterController {
    private final PatientMasterService patientMasterService;

    public PatientMasterController(PatientMasterService patientMasterService) {
        this.patientMasterService = patientMasterService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultPagination>> search(
            @ParameterObject PatientMasterSearchRequest search,
            @ParameterObject Pageable pageable) {

        ResultPagination result = patientMasterService.searchMasters(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hồ sơ gốc thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientMasterResponse>> getById(@PathVariable Long id) {
        PatientMasterResponse patientMaster = patientMasterService.getById(id);
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin hồ sơ gốc thành công", patientMaster));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientMasterResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePatientMasterRequest request) {
        if (!id.equals(request.id())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("ID trong URL và body không khớp"));
        }
        PatientMasterResponse updated = patientMasterService.update(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ gốc thành công", updated));
    }
}
