package com.mpi.demo.service.impl;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.constant.PatientStatusEnum;
import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.dto.response.PatientMasterResponse;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.PatientMasterRepository;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.service.PatientMasterService;

import jakarta.persistence.criteria.Predicate;

@Service
public class PatientMasterServiceImpl implements PatientMasterService {
    private final PatientMasterRepository patientMasterRepository;
    private final PatientRepository patientRepository;

    public PatientMasterServiceImpl(PatientMasterRepository patientMasterRepository,
            PatientRepository patientRepository) {
        this.patientMasterRepository = patientMasterRepository;
        this.patientRepository = patientRepository;
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
        patientRepository.save(patient);

        return master;
    }

    @Override
    public void linkToMaster(Long patientId, Long masterId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Không tiềm thấy mã bệnh nhân nguồn: " + patientId));
        PatientMaster master = patientMasterRepository.findById(masterId)
                .orElseThrow(() -> new RuntimeException("Không tiềm thấy mã bệnh nhân đích: " + masterId));

        patient.setMasterPatient(master);
        patientRepository.save(patient);
    }

    @Override
    public void unlinkPatient(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Không tiềm thấy mã bệnh nhân: " + patientId));

        patient.setMasterPatient(null);
        patientRepository.save(patient);
    }

    @Override
    public Page<PatientMaster> searchMasters(String keyword, Pageable pageable) {
        return patientMasterRepository.findAll(pageable);
    }

    @Override
    public ResultPagination searchMasters(PatientMasterSearchRequest request, Pageable pageable) {
        Specification<PatientMaster> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            // Keyword search (enterpriseId, fullName, nationalId, healthInsuranceNo, phoneNumber)
            if (request.keyword() != null && !request.keyword().isBlank()) {
                String keywordPattern = "%" + request.keyword().trim().toUpperCase() + "%";
                Predicate keywordPredicate = cb.or(
                        cb.like(cb.upper(root.get("enterpriseId")), keywordPattern),
                        cb.like(cb.upper(root.get("fullName")), keywordPattern),
                        cb.like(cb.upper(root.get("nationalId")), keywordPattern),
                        cb.like(cb.upper(root.get("healthInsuranceNo")), keywordPattern),
                        cb.like(cb.upper(root.get("phoneNumber")), keywordPattern));
                predicate = cb.and(predicate, keywordPredicate);
            }

            // Enterprise ID
            if (request.enterpriseId() != null && !request.enterpriseId().isBlank()) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("enterpriseId"), request.enterpriseId().trim()));
            }

            // National ID
            if (request.nationalId() != null && !request.nationalId().isBlank()) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("nationalId"), request.nationalId().trim()));
            }

            // Health Insurance No
            if (request.healthInsuranceNo() != null && !request.healthInsuranceNo().isBlank()) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("healthInsuranceNo"), request.healthInsuranceNo().trim()));
            }

            // Phone Number
            if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("phoneNumber"), request.phoneNumber().trim()));
            }

            // Gender
            if (request.gender() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("gender"), request.gender()));
            }

            // Status
            if (request.status() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), request.status()));
            } else {
                // Default: only show ACTIVE
                predicate = cb.and(predicate, cb.equal(root.get("status"), PatientStatusEnum.ACTIVE));
            }

            // Age range filter
            if (request.ageFrom() != null || request.ageTo() != null) {
                LocalDate today = LocalDate.now();

                if (request.ageTo() != null) {
                    LocalDate minDob = today.minusYears(request.ageTo() + 1).plusDays(1);
                    predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("dateOfBirth"), minDob));
                }

                if (request.ageFrom() != null) {
                    LocalDate maxDob = today.minusYears(request.ageFrom());
                    predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("dateOfBirth"), maxDob));
                }
            }

            return predicate;
        };

        Page<PatientMaster> page = patientMasterRepository.findAll(spec, pageable);
        Page<PatientMasterResponse> responsePage = page.map(this::toResponse);

        return ResultPagination.fromPage(responsePage);
    }

    @Override
    public PatientMasterResponse getById(Long id) {
        PatientMaster master = patientMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ gốc với ID: " + id));
        return toResponse(master);
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
