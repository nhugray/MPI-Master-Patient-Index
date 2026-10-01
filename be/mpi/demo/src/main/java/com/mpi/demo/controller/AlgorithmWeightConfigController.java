package com.mpi.demo.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mpi.demo.dto.request.CreateWeightConfigRequest;
import com.mpi.demo.dto.response.WeightConfigResponse;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.AlgorithmWeightConfigService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/algorithm-config")
public class AlgorithmWeightConfigController {

    private final AlgorithmWeightConfigService algorithmWeightConfigService;

    public AlgorithmWeightConfigController(AlgorithmWeightConfigService algorithmWeightConfigService) {
        this.algorithmWeightConfigService = algorithmWeightConfigService;
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<WeightConfigResponse>> getActiveConfig() {
        WeightConfigResponse response = algorithmWeightConfigService.getActiveConfig();
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình trọng số đang active thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultPagination>> getAllConfigs(@ParameterObject Pageable pageable) {
        Page<WeightConfigResponse> page = algorithmWeightConfigService.getAllConfigs(pageable);
        ResultPagination result = ResultPagination.fromPage(page);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử cấu hình trọng số thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WeightConfigResponse>> getById(@PathVariable Long id) {
        WeightConfigResponse response = algorithmWeightConfigService.getConfigById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cấu hình trọng số thành công", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WeightConfigResponse>> create(
            @Valid @RequestBody CreateWeightConfigRequest request) {
        WeightConfigResponse response = algorithmWeightConfigService.createConfig(request);
        return ResponseEntity.ok(ApiResponse.created("Tạo phiên bản cấu hình trọng số mới thành công", response));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<WeightConfigResponse>> activate(@PathVariable Long id) {
        WeightConfigResponse response = algorithmWeightConfigService.activateConfig(id);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt phiên bản cấu hình trọng số thành công", response));
    }
}
