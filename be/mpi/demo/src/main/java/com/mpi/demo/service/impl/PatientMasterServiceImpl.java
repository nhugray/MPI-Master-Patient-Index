package com.mpi.demo.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.constant.MatchActionEnum;
import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.constant.MatchStatusEnum;
import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientMasterRequest;
import com.mpi.demo.dto.response.PatientMasterHistoryResponse;
import com.mpi.demo.dto.response.PatientMasterResponse;
import com.mpi.demo.entity.MatchCandidate;
import com.mpi.demo.entity.MatchDecisionLog;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.MatchCandidateRepository;
import com.mpi.demo.repository.MatchDecisionLogRepository;
import com.mpi.demo.repository.PatientMasterRepository;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.service.PatientMasterService;
import com.mpi.demo.specification.PatientMasterSpecification;

@Service
public class PatientMasterServiceImpl implements PatientMasterService {
    private final PatientMasterRepository patientMasterRepository;
    private final PatientRepository patientRepository;
    private final MatchDecisionLogRepository matchDecisionLogRepository;
    private final MatchCandidateRepository matchCandidateRepository;

    public PatientMasterServiceImpl(PatientMasterRepository patientMasterRepository,
            PatientRepository patientRepository,
            MatchDecisionLogRepository matchDecisionLogRepository,
            MatchCandidateRepository matchCandidateRepository) {
        this.patientMasterRepository = patientMasterRepository;
        this.patientRepository = patientRepository;
        this.matchDecisionLogRepository = matchDecisionLogRepository;
        this.matchCandidateRepository = matchCandidateRepository;
    }

    @Override
    @Transactional
    public PatientMaster createMaster(Patient patient) {
        PatientMaster master = new PatientMaster();
        master.setFullName(patient.getFullName());
        master.setDateOfBirth(patient.getDateOfBirth());
        master.setGender(patient.getGender());
        master.setPhoneNumber(patient.getPhoneNumber());
        master.setNationalId(patient.getNationalId());
        master.setAddress(patient.getAddress());
        master.setEnterpriseId(generateEnterpriseId());
        master.setHealthInsuranceNo(patient.getHealthInsuranceNo());

        patientMasterRepository.save(master);

        patient.setMasterPatient(master);
        patient.setMatchStatus(MatchStatusEnum.NEW_MASTER);
        patientRepository.save(patient);

        return master;
    }

    @Override
    @Transactional
    public void linkToMaster(Long patientId, Long masterId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã bệnh nhân nguồn: " + patientId));
        PatientMaster master = patientMasterRepository.findById(masterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã bệnh nhân đích: " + masterId));

        patient.setMasterPatient(master);
        patient.setMatchStatus(MatchStatusEnum.MATCHED);
        patientRepository.save(patient);
    }

    @Override
    @Transactional
    public void unlinkPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã bệnh nhân: " + patientId));

        patient.setMasterPatient(null);
        patientRepository.save(patient);
    }

    @Override
    public ResultPagination searchMasters(PatientMasterSearchRequest request, Pageable pageable) {
        Specification<PatientMaster> spec = PatientMasterSpecification.build(request);
        Page<PatientMaster> page = patientMasterRepository.findAll(spec, pageable);
        Page<PatientMasterResponse> response = page.map(this::toResponse);

        return ResultPagination.fromPage(response);
    }

    @Override
    public PatientMasterResponse getById(Long id) {
        PatientMaster master = patientMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ gốc với ID: " + id));
        return toResponse(master);
    }

    @Override
    @Transactional
    public PatientMasterResponse update(UpdatePatientMasterRequest request) {
        PatientMaster master = patientMasterRepository.findById(request.id())
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ gốc", "id", request.id()));

        master.setFullName(request.fullName());
        master.setDateOfBirth(request.dateOfBirth());
        master.setGender(request.gender());
        master.setNationalId(request.nationalId());
        master.setHealthInsuranceNo(request.healthInsuranceNo());
        master.setPhoneNumber(request.phoneNumber());
        master.setAddress(request.address());

        PatientMaster saved = patientMasterRepository.save(master);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientMasterHistoryResponse> getHistory(Long masterId) {
        PatientMaster master = patientMasterRepository.findById(masterId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ gốc", "id", masterId));

        List<PatientMasterHistoryResponse> historyList = new ArrayList<>();
        Set<Long> processedCandidateIds = new HashSet<>();

        List<MatchDecisionLog> logs = matchDecisionLogRepository.findByMasterIdOrderByCreatedAtDesc(masterId);
        for (MatchDecisionLog log : logs) {
            MatchCandidate mc = log.getMatchCandidate();
            if (mc != null) {
                processedCandidateIds.add(mc.getId());
            }

            String actionTitle;
            String actionType;
            String colorCode;

            if (log.getAction() == MatchActionEnum.MERGE) {
                if (mc != null && mc.getDecision() == MatchDecisionEnum.AUTO_APPROVED) {
                    actionTitle = "Tự động gộp (Auto Merge)";
                    actionType = "AUTO_MERGE";
                    colorCode = "success-optimal";
                } else {
                    actionTitle = "Gộp thủ công (Manual Merge)";
                    actionType = "MANUAL_MERGE";
                    colorCode = "warning-high";
                }
            } else if (log.getAction() == MatchActionEnum.REJECT) {
                actionTitle = "Từ chối liên kết (Reject Match)";
                actionType = "REJECT";
                colorCode = "error-optimal";
            } else {
                actionTitle = "Tách hồ sơ (Unmerge)";
                actionType = "UNMERGE";
                colorCode = "brand-primary";
            }

            String patientCode = (mc != null && mc.getPatient() != null) ? mc.getPatient().getLocalPatientCode() : "N/A";
            String sourceName = (mc != null && mc.getPatient() != null && mc.getPatient().getSourceSystem() != null)
                    ? mc.getPatient().getSourceSystem().getName()
                    : "Hệ thống nguồn";

            String detail = log.getReason();
            if (detail == null || detail.isBlank()) {
                double scoreVal = (mc != null && mc.getMatchScore() != null) ? mc.getMatchScore().doubleValue() : 0.0;
                detail = String.format("Gộp hồ sơ %s từ %s. Điểm tin cậy: %.1f%%.", patientCode, sourceName, scoreVal);
            }

            String performer = log.getPerformedBy() != null ? log.getPerformedBy().getName() : "Hệ thống (Rule Engine)";
            Double score = (mc != null && mc.getMatchScore() != null) ? mc.getMatchScore().doubleValue() : null;

            historyList.add(new PatientMasterHistoryResponse(
                    actionTitle,
                    actionType,
                    log.getCreatedAt(),
                    performer,
                    detail,
                    colorCode,
                    score
            ));
        }

        List<MatchCandidate> candidates = matchCandidateRepository.findByCandidateMasterId(masterId);
        for (MatchCandidate mc : candidates) {
            if (!processedCandidateIds.contains(mc.getId()) &&
                    (mc.getDecision() == MatchDecisionEnum.AUTO_APPROVED || mc.getDecision() == MatchDecisionEnum.MANUAL_APPROVED)) {

                boolean isAuto = mc.getDecision() == MatchDecisionEnum.AUTO_APPROVED;
                String actionTitle = isAuto ? "Tự động gộp (Auto Merge)" : "Gộp thủ công (Manual Merge)";
                String actionType = isAuto ? "AUTO_MERGE" : "MANUAL_MERGE";
                String colorCode = isAuto ? "success-optimal" : "warning-high";

                String patientCode = mc.getPatient() != null ? mc.getPatient().getLocalPatientCode() : "N/A";
                String sourceName = (mc.getPatient() != null && mc.getPatient().getSourceSystem() != null)
                        ? mc.getPatient().getSourceSystem().getName()
                        : "Hệ thống nguồn";

                String performer = mc.getReviewedBy() != null ? mc.getReviewedBy().getName() : "Hệ thống (Rule Engine)";
                double scoreVal = mc.getMatchScore() != null ? mc.getMatchScore().doubleValue() : 0.0;
                String detail = String.format("Gộp hồ sơ %s từ %s. Điểm tin cậy: %.1f%%.", patientCode, sourceName, scoreVal);

                historyList.add(new PatientMasterHistoryResponse(
                        actionTitle,
                        actionType,
                        mc.getReviewedAt() != null ? mc.getReviewedAt() : mc.getCreatedAt(),
                        performer,
                        detail,
                        colorCode,
                        scoreVal
                ));
            }
        }

        if (master.getUpdatedAt() != null && master.getCreatedAt() != null &&
                master.getUpdatedAt().isAfter(master.getCreatedAt().plusSeconds(2))) {
            historyList.add(new PatientMasterHistoryResponse(
                    "Cập nhật thủ công (Manual Edit)",
                    "MANUAL_EDIT",
                    master.getUpdatedAt(),
                    "Quản trị viên / Cán bộ y tế",
                    "Cập nhật thông tin định danh hồ sơ gốc.",
                    "brand-primary",
                    null
            ));
        }

        if (master.getCreatedAt() != null) {
            historyList.add(new PatientMasterHistoryResponse(
                    "Khởi tạo hồ sơ gốc (Initial Master Created)",
                    "INITIAL",
                    master.getCreatedAt(),
                    "Hệ thống (Rule Engine)",
                    "Khởi tạo hồ sơ gốc với mã định danh EID: " + master.getEnterpriseId(),
                    "brand-primary",
                    null
            ));
        }

        historyList.sort(Comparator.comparing(PatientMasterHistoryResponse::date, Comparator.nullsLast(Comparator.reverseOrder())));

        return historyList;
    }

    public String generateEnterpriseId() {
        return "EID-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PatientMasterResponse toResponse(PatientMaster master) {
        Long linkedCount = patientRepository.countByMasterPatient(master);

        return new PatientMasterResponse(
                master.getId(),
                master.getEnterpriseId(),
                master.getFullName(),
                master.getDateOfBirth(),
                master.getGender(),
                master.getNationalId(),
                master.getHealthInsuranceNo(),
                master.getPhoneNumber(),
                master.getAddress(),
                master.getStatus(),
                master.getMergedIntoId(),
                linkedCount,
                master.getCreatedAt(),
                master.getUpdatedAt());
    }
}
