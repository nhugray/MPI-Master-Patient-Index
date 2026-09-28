package com.mpi.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.dto.response.PatientMasterResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.PatientMasterService;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
