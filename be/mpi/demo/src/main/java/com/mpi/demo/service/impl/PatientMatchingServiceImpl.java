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
import com.mpi.demo.service.PatientMatchingService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
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

    public PatientMatchingServiceImpl(PatientMasterRepository patientMasterRepository,
            MatchCandidateRepository matchCandidateRepository,
            AlgorithmWeightConfigRepository weightConfigRepository,
            MatchDecisionLogRepository matchDecisionLogRepository,
            UserRepository userRepository,
            PatientRepository patientRepository) {
        this.patientMasterRepository = patientMasterRepository;
        this.matchCandidateRepository = matchCandidateRepository;
        this.weightConfigRepository = weightConfigRepository;
        this.matchDecisionLogRepository = matchDecisionLogRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
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
                    .ifPresent(m -> masterCandidates.put(m.getId(), m));
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
        candidate.setReviewedAt(java.time.LocalDateTime.now());
        matchCandidateRepository.save(candidate);

        MatchActionEnum action;
        Patient patient = candidate.getPatient();

        if (decision == MatchDecisionEnum.MANUAL_APPROVED) {
            action = MatchActionEnum.MERGE;
            PatientMaster master = candidate.getCandidateMaster();
            patient.setMasterPatient(master);
            patient.setMatchStatus(MatchStatusEnum.MATCHED);
            Patient savedPatient = patientRepository.save(patient);
            candidate.setPatient(savedPatient);
        } else if (decision == MatchDecisionEnum.REJECTED) {
            action = MatchActionEnum.REJECT;
            patient.setMatchStatus(MatchStatusEnum.REJECTED);
            Patient savedPatient = patientRepository.save(patient);
            candidate.setPatient(savedPatient);
        } else {
            action = MatchActionEnum.MERGE;
            PatientMaster master = candidate.getCandidateMaster();
            patient.setMasterPatient(master);
            patient.setMatchStatus(MatchStatusEnum.MATCHED);
            Patient savedPatient = patientRepository.save(patient);
            candidate.setPatient(savedPatient);
        }

        String defaultReason = action == MatchActionEnum.MERGE
                ? "Chuyên viên duyệt gộp hồ sơ vào mã EID: " + candidate.getCandidateMaster().getEnterpriseId()
                : "Chuyên viên từ chối liên kết hồ sơ";

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
        return matchCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ duyệt."));
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

        return MatchCandidate.builder()
                .patient(patient)
                .candidateMaster(master)
                .matchScore(score)
                .decision(decision)
                .weightVersion(config.getVersion())
                .build();
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
