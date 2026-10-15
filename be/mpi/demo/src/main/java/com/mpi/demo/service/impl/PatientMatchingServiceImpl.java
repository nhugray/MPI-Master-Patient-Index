package com.mpi.demo.service.impl;

import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.entity.AlgorithmWeightConfig;
import com.mpi.demo.entity.MatchCandidate;
import com.mpi.demo.repository.AlgorithmWeightConfigRepository;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;
import com.mpi.demo.repository.MatchCandidateRepository;
import com.mpi.demo.repository.PatientMasterRepository;
import com.mpi.demo.service.PatientMatchingService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PatientMatchingServiceImpl implements PatientMatchingService {
    private final PatientMasterRepository patientMasterRepository;
    private final MatchCandidateRepository matchCandidateRepository;
    private final AlgorithmWeightConfigRepository weightConfigRepository;

    public PatientMatchingServiceImpl(PatientMasterRepository patientMasterRepository,
            MatchCandidateRepository matchCandidateRepository,
            AlgorithmWeightConfigRepository weightConfigRepository) {
        this.patientMasterRepository = patientMasterRepository;
        this.matchCandidateRepository = matchCandidateRepository;
        this.weightConfigRepository = weightConfigRepository;
    }

    private AlgorithmWeightConfig loadActiveConfig() {
        return weightConfigRepository.findByIsActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "Chưa có phiên bản cấu hình trọng số nào đang được kích hoạt"));
    }

    @Override
    public List<MatchCandidate> findMatchCandidates(Patient patient) {
        AlgorithmWeightConfig config = loadActiveConfig();
        Set<PatientMaster> masterCandidates = new HashSet<>();

        if (patient.getDateOfBirth() != null && patient.getFullName() != null && patient.getFullName().length() >= 3) {
            String first3 = patient.getFullName().substring(0, 3).toUpperCase();
            masterCandidates.addAll(
                    patientMasterRepository.findByDateOfBirthAndFirst3Chars(patient.getDateOfBirth(), first3));
        }

        if (patient.getPhoneNumber() != null && patient.getPhoneNumber().length() >= 4) {
            String last4 = patient.getPhoneNumber().substring(patient.getPhoneNumber().length() - 4);
            masterCandidates.addAll(patientMasterRepository.findByPhoneLast4Digits(last4));
        }

        if (patient.getNationalId() != null) {
            patientMasterRepository.findByNationalId(patient.getNationalId())
                    .ifPresent(masterCandidates::add);
        }

        return masterCandidates.stream()
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
        if (matchScore.compareTo(config.getAutoApprovalThreshold()) >= 0) {
            return MatchDecisionEnum.AUTO_APPROVED;
        }
        if (matchScore.compareTo(config.getManualReviewThreshold()) >= 0) {
            return MatchDecisionEnum.PENDING;
        }
        return MatchDecisionEnum.REJECTED;
    }

    @Override
    public void processMatchDecision(Long candidateId, MatchDecisionEnum decision, Long reviewerId, String reviewNote) {
        MatchCandidate candidate = matchCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ duyệt."));

        candidate.setDecision(decision);
        candidate.setReviewedAt(java.time.LocalDateTime.now());

        matchCandidateRepository.save(candidate);

        if (decision == MatchDecisionEnum.MANUAL_APPROVED) {
            Patient patient = candidate.getPatient();
            PatientMaster master = candidate.getCandidateMaster();
            patient.setMasterPatient(master);
        }
    }

    @Override
    public Page<MatchCandidate> searchCandidates(MatchDecisionEnum decision, Pageable pageable) {
        if (decision == null) {
            return matchCandidateRepository.findAll(pageable);
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

    private boolean isExactMatch(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.trim().equalsIgnoreCase(str2.trim());
    }

    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        String str1 = normalizeVietnamese(s1);
        String str2 = normalizeVietnamese(s2);

        if (str1.equals(str2)) {
            return 1.0;
        }

        if (str1.contains(str2) || str2.contains(str1)) {
            return 0.85;
        }

        return 0.0;
    }

    private String normalizeVietnamese(String str) {
        if (str == null) {
            return "";
        }

        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);

        String result = temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[Đđ]", "d")
                .toLowerCase()
                .trim();

        return result.replaceAll("\\s+", " ");
    }
}
