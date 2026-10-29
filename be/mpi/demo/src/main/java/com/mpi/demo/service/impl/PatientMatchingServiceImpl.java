package com.mpi.demo.service.impl;

import com.mpi.demo.constant.MatchActionEnum;
import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.constant.MatchStatusEnum;
import com.mpi.demo.entity.AlgorithmWeightConfig;
import com.mpi.demo.entity.MatchCandidate;
import com.mpi.demo.entity.MatchDecisionLog;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;
import com.mpi.demo.entity.User;
import com.mpi.demo.repository.AlgorithmWeightConfigRepository;
import com.mpi.demo.repository.MatchCandidateRepository;
import com.mpi.demo.repository.MatchDecisionLogRepository;
import com.mpi.demo.repository.PatientMasterRepository;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.repository.UserRepository;
import com.mpi.demo.service.PatientMasterService;
import com.mpi.demo.service.PatientMatchingService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PatientMatchingServiceImpl implements PatientMatchingService {
    @Value("${mpi.matching.auto-approval-threshold:92.00}")
    private BigDecimal autoApprovalThreshold;

    @Value("${mpi.matching.manual-review-threshold:50.00}")
    private BigDecimal manualReviewThreshold;

    private final PatientMasterRepository patientMasterRepository;
    private final MatchCandidateRepository matchCandidateRepository;
    private final AlgorithmWeightConfigRepository weightConfigRepository;
    private final MatchDecisionLogRepository matchDecisionLogRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PatientMasterService patientMasterService;

    public PatientMatchingServiceImpl(PatientMasterRepository patientMasterRepository,
            MatchCandidateRepository matchCandidateRepository,
            AlgorithmWeightConfigRepository weightConfigRepository,
            MatchDecisionLogRepository matchDecisionLogRepository,
            UserRepository userRepository,
            PatientRepository patientRepository,
            PatientMasterService patientMasterService) {
        this.patientMasterRepository = patientMasterRepository;
        this.matchCandidateRepository = matchCandidateRepository;
        this.weightConfigRepository = weightConfigRepository;
        this.matchDecisionLogRepository = matchDecisionLogRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.patientMasterService = patientMasterService;
    }

    private AlgorithmWeightConfig loadActiveConfig() {
        return weightConfigRepository.findByIsActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa có phiên bản cấu hình trọng số nào đang được kích hoạt"));
    }

    @Override
    public List<MatchCandidate> findMatchCandidates(Patient patient) {
        AlgorithmWeightConfig config = loadActiveConfig();
        Map<Long, PatientMaster> masterCandidates = new HashMap<>();

        if (patient.getDateOfBirth() != null && patient.getFullName() != null && patient.getFullName().length() >= 3) {
            String first3 = patient.getFullName().substring(0, 3).toUpperCase();
            patientMasterRepository.findByDateOfBirthAndFirst3Chars(patient.getDateOfBirth(), first3)
                    .forEach(m -> masterCandidates.put(m.getId(), m));
        }

        if (patient.getPhoneNumber() != null && patient.getPhoneNumber().length() >= 4) {
            String last4 = patient.getPhoneNumber().substring(patient.getPhoneNumber().length() - 4);
            patientMasterRepository.findByPhoneLast4Digits(last4)
                    .forEach(m -> masterCandidates.put(m.getId(), m));
        }

        if (patient.getNationalId() != null) {
            patientMasterRepository.findByNationalId(patient.getNationalId())
                    .forEach(m -> masterCandidates.put(m.getId(), m));
        }

        return masterCandidates.values().stream()
                .map(master -> buildMatchCandidate(patient, master, config))
                .filter(mc -> mc.getMatchScore().compareTo(new BigDecimal("30.00")) >= 0)
                .sorted(Comparator.comparing(MatchCandidate::getMatchScore).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateMatchScore(Patient patient, PatientMaster master) {
        return calculateMatchScore(patient, master, loadActiveConfig());
    }

    private BigDecimal calculateMatchScore(Patient patient, PatientMaster master, AlgorithmWeightConfig config) {
        BigDecimal score = BigDecimal.ZERO;

        if (isExactMatch(patient.getNationalId(), master.getNationalId())) {
            score = score.add(config.getNationalIdWeight());
        }

        if (patient.getFullName() != null && master.getFullName() != null) {
            double nameSimilarity = calculateStringSimilarity(patient.getFullName(), master.getFullName());
            score = score.add(config.getNameWeight().multiply(BigDecimal.valueOf(nameSimilarity)));
        }

        if (patient.getDateOfBirth() != null && patient.getDateOfBirth().equals(master.getDateOfBirth())) {
            score = score.add(config.getDobWeight());
        }

        if (isExactMatch(patient.getPhoneNumber(), master.getPhoneNumber())) {
            score = score.add(config.getPhoneWeight());
        }

        if (patient.getAddress() != null && master.getAddress() != null) {
            double addressSimilarity = calculateStringSimilarity(patient.getAddress(), master.getAddress());
            score = score.add(config.getAddressWeight().multiply(BigDecimal.valueOf(addressSimilarity)));
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public MatchDecisionEnum getAutoDecision(BigDecimal matchScore) {
        return getAutoDecision(matchScore, loadActiveConfig());
    }

    private MatchDecisionEnum getAutoDecision(BigDecimal matchScore, AlgorithmWeightConfig config) {
        BigDecimal autoThreshold = this.autoApprovalThreshold != null ? this.autoApprovalThreshold
                : config.getAutoApprovalThreshold();
        BigDecimal manualThreshold = this.manualReviewThreshold != null ? this.manualReviewThreshold
                : config.getManualReviewThreshold();

        if (matchScore.compareTo(autoThreshold) >= 0) {
            return MatchDecisionEnum.AUTO_APPROVED;
        }
        if (matchScore.compareTo(manualThreshold) >= 0) {
            return MatchDecisionEnum.PENDING;
        }
        return MatchDecisionEnum.REJECTED;
    }

    @Override
    @Transactional
    public void processMatchDecision(Long candidateId, MatchDecisionEnum decision, Long reviewerId, String reviewNote) {
        MatchCandidate candidate = matchCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ duyệt."));

        User reviewer = null;
        if (reviewerId != null) {
            reviewer = userRepository.findById(reviewerId).orElse(null);
            candidate.setReviewedBy(reviewer);
        }

        candidate.setDecision(decision);
        candidate.setReviewedAt(LocalDateTime.now());
        matchCandidateRepository.save(candidate);

        MatchActionEnum action;
        Patient patient = candidate.getPatient();
        String defaultReason;

        if (decision == MatchDecisionEnum.MANUAL_APPROVED || decision == MatchDecisionEnum.AUTO_APPROVED) {
            action = MatchActionEnum.MERGE;
            PatientMaster targetMaster = candidate.getCandidateMaster();
            PatientMaster currentMaster = patient.getMasterPatient();
            boolean deletedWrongMaster = false;

            if (currentMaster != null && !currentMaster.getId().equals(targetMaster.getId())) {
                boolean isCandidateMaster = !matchCandidateRepository.findByCandidateMasterId(currentMaster.getId())
                        .isEmpty();
                long linkedCount = patientRepository.countByMasterPatient(currentMaster);

                if (!isCandidateMaster && linkedCount <= 1) {
                    // Chuyển liên kết bệnh nhân sang targetMaster
                    patient.setMasterPatient(targetMaster);
                    patient.setMatchStatus(MatchStatusEnum.MATCHED);
                    Patient savedPatient = patientRepository.saveAndFlush(patient);
                    candidate.setPatient(savedPatient);

                    // Xóa hồ sơ gốc tạo nhầm lúc từ chối
                    patientMasterRepository.delete(currentMaster);
                    patientMasterRepository.flush();
                    deletedWrongMaster = true;
                } else {
                    // Bệnh nhân đang liên kết với một Master khác (ví dụ M1 trước đó)
                    patient.setMasterPatient(targetMaster);
                    patient.setMatchStatus(MatchStatusEnum.MATCHED);
                    Patient savedPatient = patientRepository.save(patient);
                    candidate.setPatient(savedPatient);

                    // Tìm ứng viên cũ của currentMaster từng được duyệt để chuyển sang REJECTED và
                    // ghi log UNMERGE
                    List<MatchCandidate> previousApprovedCandidates = matchCandidateRepository
                            .findByPatientId(patient.getId()).stream()
                            .filter(mc -> mc.getCandidateMaster().getId().equals(currentMaster.getId())
                                    && (mc.getDecision() == MatchDecisionEnum.MANUAL_APPROVED
                                            || mc.getDecision() == MatchDecisionEnum.AUTO_APPROVED))
                            .collect(Collectors.toList());

                    String patientCode = patient.getLocalPatientCode() != null ? patient.getLocalPatientCode()
                            : ("ID-" + patient.getId());

                    for (MatchCandidate prev : previousApprovedCandidates) {
                        prev.setDecision(MatchDecisionEnum.REJECTED);
                        prev.setReviewedAt(java.time.LocalDateTime.now());
                        prev.setReviewedBy(reviewer);
                        matchCandidateRepository.save(prev);

                        String unmergeReason = String.format(
                                "Hồ sơ bệnh nhân %s đã được tách (unlink) khỏi mã EID: %s để chuyển sang gộp vào mã EID: %s",
                                patientCode, currentMaster.getEnterpriseId(),
                                targetMaster.getEnterpriseId());

                        MatchDecisionLog unmergeLog = MatchDecisionLog.builder()
                                .matchCandidate(prev)
                                .action(MatchActionEnum.UNMERGE)
                                .performedBy(reviewer)
                                .reason(unmergeReason)
                                .build();

                        matchDecisionLogRepository.save(unmergeLog);
                    }
                }
            } else {
                patient.setMasterPatient(targetMaster);
                patient.setMatchStatus(MatchStatusEnum.MATCHED);
                Patient savedPatient = patientRepository.save(patient);
                candidate.setPatient(savedPatient);
            }

            defaultReason = "Chuyên viên duyệt gộp hồ sơ vào mã EID: " + targetMaster.getEnterpriseId()
                    + (deletedWrongMaster ? " (Đã xóa hồ sơ gốc tạo nhầm)" : "");

            // Hướng 1: Tự động đóng/từ chối tất cả các ứng viên nghi trùng khác của cùng
            // bệnh nhân
            List<MatchCandidate> otherPendingCandidates = matchCandidateRepository.findByPatientId(patient.getId())
                    .stream()
                    .filter(mc -> !mc.getId().equals(candidate.getId())
                            && mc.getDecision() == MatchDecisionEnum.PENDING)
                    .collect(Collectors.toList());

            for (MatchCandidate other : otherPendingCandidates) {
                other.setDecision(MatchDecisionEnum.REJECTED);
                other.setReviewedAt(java.time.LocalDateTime.now());
                other.setReviewedBy(reviewer);
                matchCandidateRepository.save(other);

                String autoRejectReason = "Hệ thống tự động từ chối do hồ sơ bệnh nhân đã được duyệt gộp vào mã EID: "
                        + targetMaster.getEnterpriseId();

                MatchDecisionLog autoRejectLog = MatchDecisionLog.builder()
                        .matchCandidate(other)
                        .action(MatchActionEnum.REJECT)
                        .performedBy(reviewer)
                        .reason(autoRejectReason)
                        .build();

                matchDecisionLogRepository.save(autoRejectLog);
            }

        } else if (decision == MatchDecisionEnum.REJECTED) {
            action = MatchActionEnum.REJECT;
            PatientMaster currentMaster = patient.getMasterPatient();

            // Nếu bệnh nhân chưa có master riêng hoặc đang trỏ vào candidateMaster
            if (currentMaster == null || currentMaster.getId().equals(candidate.getCandidateMaster().getId())) {
                patient.setMasterPatient(null);
                patientRepository.saveAndFlush(patient);

                // Khởi tạo hồ sơ gốc mới cho bệnh nhân này
                PatientMaster newMaster = patientMasterService.createMaster(patient);
                patient.setMatchStatus(MatchStatusEnum.REJECTED);
                Patient savedPatient = patientRepository.save(patient);
                candidate.setPatient(savedPatient);

                defaultReason = "Chuyên viên từ chối liên kết hồ sơ và khởi tạo hồ sơ gốc mới mã EID: "
                        + newMaster.getEnterpriseId();
            } else {
                if (patient.getMatchStatus() != MatchStatusEnum.MATCHED) {
                    patient.setMatchStatus(MatchStatusEnum.REJECTED);
                    Patient savedPatient = patientRepository.save(patient);
                    candidate.setPatient(savedPatient);
                }

                defaultReason = "Chuyên viên từ chối liên kết hồ sơ";
            }
        } else {
            action = MatchActionEnum.REJECT;
            defaultReason = "Chuyên viên cập nhật trạng thái hồ sơ";
        }

        MatchDecisionLog decisionLog = MatchDecisionLog.builder()
                .matchCandidate(candidate)
                .action(action)
                .performedBy(reviewer)
                .reason(reviewNote != null && !reviewNote.isBlank() ? reviewNote : defaultReason)
                .build();

        matchDecisionLogRepository.save(decisionLog);
    }

    @Override
    public Page<MatchCandidate> searchCandidates(MatchDecisionEnum decision, Pageable pageable) {
        if (decision == null) {
            return matchCandidateRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return matchCandidateRepository.findByDecisionOrderByCreatedAtDesc(decision, pageable);
    }

    @Override
    public MatchCandidate getCandidateById(Long candidateId) {
        MatchCandidate candidate = matchCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ho so duyet."));
        if (candidate.getScoreBreakdown() == null && candidate.getPatient() != null
                && candidate.getCandidateMaster() != null) {
            AlgorithmWeightConfig config = loadActiveConfig();
            String breakdownJson = generateScoreBreakdown(candidate.getPatient(), candidate.getCandidateMaster(),
                    config);
            candidate.setScoreBreakdown(breakdownJson);
            matchCandidateRepository.save(candidate);
        }
        return candidate;
    }

    @Override
    public Map<String, Long> getDecisionCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("PENDING", matchCandidateRepository.countByDecision(MatchDecisionEnum.PENDING));
        counts.put("AUTO_APPROVED", matchCandidateRepository.countByDecision(MatchDecisionEnum.AUTO_APPROVED));
        counts.put("MANUAL_APPROVED", matchCandidateRepository.countByDecision(MatchDecisionEnum.MANUAL_APPROVED));
        counts.put("REJECTED", matchCandidateRepository.countByDecision(MatchDecisionEnum.REJECTED));
        counts.put("TOTAL", matchCandidateRepository.count());
        return counts;
    }

    @Override
    public Page<MatchCandidate> getPendingReviews(Pageable pageable) {
        return matchCandidateRepository.findByDecisionOrderByCreatedAtDesc(MatchDecisionEnum.PENDING, pageable);
    }

    private MatchCandidate buildMatchCandidate(Patient patient, PatientMaster master, AlgorithmWeightConfig config) {
        BigDecimal score = calculateMatchScore(patient, master, config);
        MatchDecisionEnum decision = getAutoDecision(score, config);
        String scoreBreakdown = generateScoreBreakdown(patient, master, config);

        return MatchCandidate.builder()
                .patient(patient)
                .candidateMaster(master)
                .matchScore(score)
                .decision(decision)
                .scoreBreakdown(scoreBreakdown)
                .weightVersion(config.getVersion())
                .build();
    }

    private String generateScoreBreakdown(Patient patient, PatientMaster master, AlgorithmWeightConfig config) {
        List<Map<String, Object>> breakdown = new ArrayList<>();

        double nameSimilarity = 0.0;
        if (patient.getFullName() != null && master.getFullName() != null) {
            nameSimilarity = calculateStringSimilarity(patient.getFullName(), master.getFullName());
        }
        BigDecimal nameScore = config.getNameWeight().multiply(BigDecimal.valueOf(nameSimilarity)).setScale(2,
                RoundingMode.HALF_UP);
        breakdown.add(createBreakdownItem("fullName", "Họ và tên", nameScore, config.getNameWeight()));

        boolean dobMatch = patient.getDateOfBirth() != null && patient.getDateOfBirth().equals(master.getDateOfBirth());
        BigDecimal dobScore = dobMatch ? config.getDobWeight().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        breakdown.add(createBreakdownItem("dateOfBirth", "Ngày sinh", dobScore, config.getDobWeight()));

        boolean nationalIdMatch = isExactMatch(patient.getNationalId(), master.getNationalId());
        BigDecimal nationalIdScore = nationalIdMatch ? config.getNationalIdWeight().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        breakdown.add(createBreakdownItem("nationalId", "CCCD/CMND", nationalIdScore, config.getNationalIdWeight()));

        boolean phoneMatch = isExactMatch(patient.getPhoneNumber(), master.getPhoneNumber());
        BigDecimal phoneScore = phoneMatch ? config.getPhoneWeight().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        breakdown.add(createBreakdownItem("phoneNumber", "Số điện thoại", phoneScore, config.getPhoneWeight()));

        double addressSimilarity = 0.0;
        if (patient.getAddress() != null && master.getAddress() != null) {
            addressSimilarity = calculateStringSimilarity(patient.getAddress(), master.getAddress());
        }
        BigDecimal addressScore = config.getAddressWeight().multiply(BigDecimal.valueOf(addressSimilarity)).setScale(2,
                RoundingMode.HALF_UP);
        breakdown.add(createBreakdownItem("address", "Địa chỉ", addressScore, config.getAddressWeight()));

        try {
            return new ObjectMapper().writeValueAsString(breakdown);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> createBreakdownItem(String field, String fieldLabel, BigDecimal score,
            BigDecimal maxScore) {
        Map<String, Object> item = new HashMap<>();
        item.put("field", field);
        item.put("fieldLabel", fieldLabel);
        item.put("score", score);
        item.put("maxScore", maxScore);
        return item;
    }

    private boolean isExactMatch(String s1, String s2) {
        if (s1 == null || s2 == null)
            return false;
        return s1.trim().equalsIgnoreCase(s2.trim());
    }

    private double calculateStringSimilarity(String s1, String s2) {
        String n1 = normalize(s1);
        String n2 = normalize(s2);
        if (n1.equals(n2))
            return 1.0;
        int distance = levenshteinDistance(n1, n2);
        int maxLen = Math.max(n1.length(), n2.length());
        return maxLen == 0 ? 1.0 : 1.0 - (double) distance / maxLen;
    }

    private String normalize(String s) {
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
}
