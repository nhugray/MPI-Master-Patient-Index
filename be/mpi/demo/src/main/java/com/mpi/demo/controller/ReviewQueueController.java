package com.mpi.demo.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.dto.request.ReviewDecisionRequest;
import com.mpi.demo.dto.response.MatchCandidateResponse;
import com.mpi.demo.entity.MatchCandidate;
import com.mpi.demo.helper.ApiResponse;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.service.PatientMatchingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/review-queue")
public class ReviewQueueController {
    private final PatientMatchingService patientMatchingService;

    public ReviewQueueController(PatientMatchingService patientMatchingService) {
        this.patientMatchingService = patientMatchingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultPagination>> search(
            @RequestParam(required = false) MatchDecisionEnum decision,
            @ParameterObject Pageable pageable) {

        Page<MatchCandidate> page = patientMatchingService.searchCandidates(decision, pageable);
        
        Page<MatchCandidateResponse> responsePage = page.map(MatchCandidateResponse::fromEntity);
        ResultPagination result = ResultPagination.fromPage(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hồ sơ duyệt thành công", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchCandidateResponse>> getById(@PathVariable Long id) {
        MatchCandidate candidate = patientMatchingService.getCandidateById(id);
        MatchCandidateResponse response = MatchCandidateResponse.fromEntity(candidate);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ duyệt thành công", response));
    }

    @PutMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<MatchCandidateResponse>> submitDecision(
            @PathVariable Long id,
            @Valid @RequestBody ReviewDecisionRequest request) {

        patientMatchingService.processMatchDecision(
                id,
                request.decision(),
                request.reviewerId(),
                request.reviewNote());

        MatchCandidate updated = patientMatchingService.getCandidateById(id);
        MatchCandidateResponse response = MatchCandidateResponse.fromEntity(updated);

        return ResponseEntity.ok(ApiResponse.success("Xử lý quyết định duyệt thành công", response));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCounts() {
        Map<String, Long> counts = patientMatchingService.getDecisionCounts();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê thành công", counts));
    }
}
