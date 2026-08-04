package com.mpi.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.mpi.demo.constant.PatientStatusEnum;
import com.mpi.demo.dto.request.CreatePatientRequest;
import com.mpi.demo.dto.request.PatientSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientRequest;
import com.mpi.demo.dto.response.PatientResponse;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.exception.DuplicateResourceException;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.service.PatientService;
import com.mpi.demo.specification.PatientSpecification;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public ResultPagination search(PatientSearchRequest search, Pageable pageable) {
        Specification<Patient> spec = PatientSpecification.build(search);
        Page<PatientResponse> pageResult = patientRepository.findBy(spec, q -> q.page(pageable))
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
    public PatientResponse create(CreatePatientRequest request) {
        if (this.patientRepository.existsByNationalId(request.getNationalId())) {
            throw new DuplicateResourceException("Bệnh nhân", "CCCD", request.getNationalId());
        }
        Patient patient = new Patient();
        patient.setFullName(request.getFullName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setNationalId(request.getNationalId());
        patient.setHealthInsuranceNo(request.getHealthInsuranceNo());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setStatus(
                request.getStatus() != null ? request.getStatus() : PatientStatusEnum.ACTIVE);
        Patient saved = patientRepository.save(patient);
        return PatientResponse.fromEntity(saved);
    }

    @Override
    public PatientResponse update(UpdatePatientRequest request) {
        Patient patient = this.patientRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân", "mã bệnh nhân", request.getId()));

        if (this.patientRepository.existsByNationalId(request.getNationalId())
                && !patient.getNationalId().equals(request.getNationalId())) {
            throw new DuplicateResourceException("Bệnh nhân", "CCCD", request.getNationalId());
        }
        patient.setFullName(request.getFullName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setHealthInsuranceNo(request.getHealthInsuranceNo());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setNationalId(request.getNationalId());
        patient.setStatus(request.getStatus() != null ? request.getStatus() : patient.getStatus());
        Patient saved = patientRepository.save(patient);
        return PatientResponse.fromEntity(saved);
    }

    @Override
    public void deletePatient(Long id) {
        Patient existingPatient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân", "mã bệnh nhân", id));
        patientRepository.delete(existingPatient);
    }

}
