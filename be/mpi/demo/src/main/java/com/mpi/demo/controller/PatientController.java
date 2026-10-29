package com.mpi.demo.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.constant.MatchStatusEnum;
import com.mpi.demo.dto.request.CreatePatientRequest;
import com.mpi.demo.dto.request.PatientSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientRequest;
import com.mpi.demo.dto.response.PatientResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.PatientService;

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
@RequestMapping("/api/v1/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultPagination>> search(
            @ParameterObject PatientSearchRequest search,
            @ParameterObject Pageable pageable) {

        ResultPagination result = patientService.search(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", result));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCounts() {
        Map<String, Long> counts = patientService.getPatientCounts();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê bệnh nhân thành công", counts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> getById(@PathVariable Long id) {
        PatientResponse patient = patientService.getById(id);
        return ResponseEntity
                .ok(ApiResponse.success("Lấy thông tin bệnh nhân thành công", patient));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PatientResponse>> create(
            @Valid @RequestBody CreatePatientRequest request) {
        PatientResponse response = patientService.create(request);
        URI location = URI.create(String.format("api/v1/patients/%s", response.id()));

        String message;
        if (response.matchStatus() == MatchStatusEnum.MATCHED) {
            String eid = response.enterpriseId() != null ? response.enterpriseId() : "";
            message = "Tạo bệnh nhân thành công. Đã tự động gộp vào hồ sơ chủ EID: " + eid;
        } else if (response.matchStatus() == MatchStatusEnum.PENDING) {
            String scoreStr = response.matchScore() != null ? (" (" + response.matchScore() + "%)") : "";
            message = "Đã lưu bệnh nhân. Phát hiện nghi vấn trùng khớp" + scoreStr
                    + ", hồ sơ đã được chuyển vào Hàng đợi duyệt";
        } else {
            String eid = response.enterpriseId() != null ? response.enterpriseId() : "";
            message = "Tạo bệnh nhân mới thành công. Mã định danh EID: " + eid;
        }

        return ResponseEntity.created(location)
                .body(ApiResponse.success(message, response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PatientResponse>> update(@Valid @RequestBody UpdatePatientRequest request) {
        PatientResponse response = patientService.update(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin bệnh nhân thành công", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        patientService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bệnh nhân thành công", null));
    }

}
