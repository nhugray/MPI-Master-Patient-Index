package com.mpi.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientMasterRequest;
import com.mpi.demo.dto.response.PatientMasterResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.PatientMasterService;
import com.mpi.demo.service.PatientService;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/patient-masters")
public class PatientMasterController {
    private final PatientMasterService patientMasterService;
    private final PatientService patientService;

    public PatientMasterController(PatientMasterService patientMasterService, PatientService patientService) {
        this.patientMasterService = patientMasterService;
        this.patientService = patientService;
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

    @GetMapping("/{id}/patients")
    public ResponseEntity<ApiResponse<ResultPagination>> getLinkedPatients(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        ResultPagination result = patientService.getByMasterPatientId(id, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hồ sơ nguồn thành công", result));
    }
}
