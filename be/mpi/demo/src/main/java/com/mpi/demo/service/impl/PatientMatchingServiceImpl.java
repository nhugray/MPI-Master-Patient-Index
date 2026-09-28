package com.mpi.demo.service.impl;

import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.entity.MatchCandidate;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PatientMatchingServiceImpl implements PatientMatchingService {
    private final PatientMasterRepository patientMasterRepository;
    private final MatchCandidateRepository matchCandidateRepository;

    public PatientMatchingServiceImpl(PatientMasterRepository patientMasterRepository,
            MatchCandidateRepository matchCandidateRepository) {
        this.patientMasterRepository = patientMasterRepository;
        this.matchCandidateRepository = matchCandidateRepository;
    }

    @Override
    public List<MatchCandidate> findMatchCandidates(Patient patient) {
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
                .map(master -> buildMatchCandidate(patient, master))
                .filter(mc -> mc.getMatchScore().compareTo(new BigDecimal("30.00")) >= 0)
                .sorted(Comparator.comparing(MatchCandidate::getMatchScore).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateMatchScore(Patient patient, PatientMaster master) {
        BigDecimal score = BigDecimal.ZERO;

        if (isExactMatch(patient.getNationalId(), master.getNationalId())) {
            score = score.add(new BigDecimal("40.00"));
        }

        if (patient.getFullName() != null && master.getFullName() != null) {
            double nameSimilarity = calculateStringSimilarity(patient.getFullName(), master.getFullName());
            score = score.add(BigDecimal.valueOf(nameSimilarity * 25));
        }

        if (patient.getDateOfBirth() != null && patient.getDateOfBirth().equals(master.getDateOfBirth())) {
            score = score.add(new BigDecimal("20.00"));
        }

        if (isExactMatch(patient.getPhoneNumber(), master.getPhoneNumber())) {
            score = score.add(new BigDecimal("10.00"));
        }

        if (patient.getGender() != null && patient.getGender() == master.getGender()) {
            score = score.add(new BigDecimal("5.00"));
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public MatchDecisionEnum getAutoDecision(BigDecimal matchScore) {
        if (matchScore.compareTo(new BigDecimal("85.00")) >= 0) {
            return MatchDecisionEnum.AUTO_APPROVED;
        }
        if (matchScore.compareTo(new BigDecimal("50.00")) >= 0) {
            return MatchDecisionEnum.PENDING;
        }
        return MatchDecisionEnum.REJECTED;
    }

    @Override
    public void processMatchDecision(Long candidateId, MatchDecisionEnum decision, Long reviewerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Page<MatchCandidate> getPendingReviews(Pageable pageable) {
        return matchCandidateRepository.findByDecisionOrderByCreatedAtDesc(MatchDecisionEnum.PENDING, pageable);
    }

    private MatchCandidate buildMatchCandidate(Patient patient, PatientMaster master) {
        BigDecimal score = calculateMatchScore(patient, master);
        MatchDecisionEnum decision = getAutoDecision(score);

        return MatchCandidate.builder()
                .patient(patient)
                .candidateMaster(master)
                .matchScore(score)
                .decision(decision)
                .weightVersion("v1")
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
