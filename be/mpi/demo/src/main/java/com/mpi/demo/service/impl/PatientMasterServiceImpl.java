package com.mpi.demo.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientMasterRequest;
import com.mpi.demo.dto.response.PatientMasterResponse;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.PatientMasterRepository;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.service.PatientMasterService;
import com.mpi.demo.specification.PatientMasterSpecification;

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
