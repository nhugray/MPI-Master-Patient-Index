package com.mpi.demo.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.constant.MatchDecisionEnum;
import com.mpi.demo.constant.MatchStatusEnum;
import com.mpi.demo.dto.request.CreatePatientRequest;
import com.mpi.demo.dto.request.PatientSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientRequest;
import com.mpi.demo.dto.response.PatientResponse;
import com.mpi.demo.entity.MatchCandidate;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.SourceSystem;
import com.mpi.demo.exception.DuplicateResourceException;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.MatchCandidateRepository;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.repository.SourceSystemRepository;
import com.mpi.demo.service.PatientMasterService;
import com.mpi.demo.service.PatientMatchingService;
import com.mpi.demo.service.PatientService;
import com.mpi.demo.specification.PatientSpecification;

@Service
public class PatientServiceImpl implements PatientService {

        private final PatientRepository patientRepository;
        private final SourceSystemRepository sourceSystemRepository;
        private final MatchCandidateRepository matchCandidateRepository;
        private final PatientMatchingService patientMatchingService;
        private final PatientMasterService patientMasterService;

        public PatientServiceImpl(PatientRepository patientRepository,
                        SourceSystemRepository sourceSystemRepository,
                        MatchCandidateRepository matchCandidateRepository,
                        PatientMatchingService patientMatchingService,
                        PatientMasterService patientMasterService) {
                this.matchCandidateRepository = matchCandidateRepository;
                this.patientRepository = patientRepository;
                this.sourceSystemRepository = sourceSystemRepository;
                this.patientMatchingService = patientMatchingService;
                this.patientMasterService = patientMasterService;
        }

        @Override
        public ResultPagination search(PatientSearchRequest search, Pageable pageable) {
                final Pageable effectivePageable = pageable.getSort().isUnsorted()
                                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
                                : pageable;
                Specification<Patient> spec = PatientSpecification.build(search);
                Page<PatientResponse> pageResult = patientRepository.findBy(spec, q -> q.page(effectivePageable))
                                .map(PatientResponse::fromEntity);
                return ResultPagination.fromPage(pageResult);
        }

        @Override
        public PatientResponse getById(Long id) {
                Patient patient = patientRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân", "id", id));
                return PatientResponse.fromEntity(patient);
        }

        @Override
        @Transactional
        public PatientResponse create(CreatePatientRequest request) {
                SourceSystem sourceSystem = sourceSystemRepository.findById(request.getSourceSystemId())
                                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống nguồn", "id",
                                                request.getSourceSystemId()));

                if (this.patientRepository.existsBySourceSystemIdAndLocalPatientCode(
                                request.getSourceSystemId(), request.getLocalPatientCode())) {
                        throw new DuplicateResourceException("Bệnh nhân",
                                        "mã bệnh nhân cục bộ tại hệ thống nguồn",
                                        request.getLocalPatientCode());
                }

                Patient patient = new Patient();
                patient.setSourceSystem(sourceSystem);
                patient.setLocalPatientCode(request.getLocalPatientCode());
                patient.setFullName(request.getFullName());
                patient.setDateOfBirth(request.getDateOfBirth());
                patient.setGender(request.getGender());
                patient.setNationalId(request.getNationalId());
                patient.setHealthInsuranceNo(request.getHealthInsuranceNo());
                patient.setPhoneNumber(request.getPhoneNumber());
                patient.setAddress(request.getAddress());
                patient.setMatchStatus(MatchStatusEnum.PENDING);

                Patient saved = patientRepository.save(patient);

                List<MatchCandidate> candidates = patientMatchingService.findMatchCandidates(saved);
                BigDecimal topScore = null;

                if (candidates.isEmpty()) {
                        patientMasterService.createMaster(saved);
                } else {
                        MatchCandidate topCandidate = candidates.get(0);
                        topScore = topCandidate.getMatchScore();

                        for (MatchCandidate candidate : candidates) {
                                candidate.setPatient(saved);
                                matchCandidateRepository.save(candidate);
                        }

                        if (topCandidate.getDecision() == MatchDecisionEnum.AUTO_APPROVED) {
                                patientMasterService.linkToMaster(saved.getId(), topCandidate.getCandidateMaster().getId());
                        } else if (topCandidate.getDecision() == MatchDecisionEnum.PENDING) {
                                saved.setMatchStatus(MatchStatusEnum.PENDING);
                                patientRepository.save(saved);
                        } else {
                                patientMasterService.createMaster(saved);
                        }
                }

                Patient updated = patientRepository.findById(saved.getId()).orElse(saved);
                return PatientResponse.fromEntity(updated, topScore);
        }

        @Override
        public PatientResponse update(UpdatePatientRequest request) {
                Patient patient = this.patientRepository.findById(request.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân", "mã bệnh nhân",
                                                request.getId()));

                SourceSystem sourceSystem = sourceSystemRepository.findById(request.getSourceSystemId())
                                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống nguồn", "id",
                                                request.getSourceSystemId()));

                if (!patient.getSourceSystem().getId().equals(request.getSourceSystemId())
                                || !patient.getLocalPatientCode().equals(request.getLocalPatientCode())) {
                        if (this.patientRepository.existsBySourceSystemIdAndLocalPatientCode(
                                        request.getSourceSystemId(), request.getLocalPatientCode())) {
                                throw new DuplicateResourceException("Bệnh nhân",
                                                "mã bệnh nhân cục bộ tại hệ thống nguồn",
                                                request.getLocalPatientCode());
                        }
                }

                patient.setSourceSystem(sourceSystem);
                patient.setLocalPatientCode(request.getLocalPatientCode());
                patient.setFullName(request.getFullName());
                patient.setDateOfBirth(request.getDateOfBirth());
                patient.setGender(request.getGender());
                patient.setHealthInsuranceNo(request.getHealthInsuranceNo());
                patient.setPhoneNumber(request.getPhoneNumber());
                patient.setNationalId(request.getNationalId());
                patient.setAddress(request.getAddress());

                Patient saved = patientRepository.save(patient);
                return PatientResponse.fromEntity(saved);
        }

        @Override
        public void delete(Long id) {
                Patient existingPatient = patientRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân", "mã bệnh nhân", id));
                patientRepository.delete(existingPatient);
        }

        @Override
        public ResultPagination getByMasterPatientId(Long masterPatientId, Pageable pageable) {
                final Pageable effectivePageable = pageable.getSort().isUnsorted()
                                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                                                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
                                : pageable;
                Page<Patient> pageResult = patientRepository.findByMasterPatientId(masterPatientId, effectivePageable);
                List<Long> patientIds = pageResult.getContent().stream().map(Patient::getId).toList();
                Map<Long, BigDecimal> scores = new HashMap<>();
                if (!patientIds.isEmpty()) {
                        matchCandidateRepository.findByPatientIdInAndCandidateMasterId(patientIds, masterPatientId)
                                        .forEach(mc -> scores.merge(mc.getPatient().getId(), mc.getMatchScore(),
                                                        (a, b) -> a.max(b)));
                }
                Page<PatientResponse> responseResult = pageResult
                                .map(p -> PatientResponse.fromEntity(p, scores.get(p.getId())));
                return ResultPagination.fromPage(responseResult);
        }

}
