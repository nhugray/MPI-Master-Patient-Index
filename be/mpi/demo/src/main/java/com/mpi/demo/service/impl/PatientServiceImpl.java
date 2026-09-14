package com.mpi.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.mpi.demo.constant.MatchStatusEnum;
import com.mpi.demo.dto.request.CreatePatientRequest;
import com.mpi.demo.dto.request.PatientSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientRequest;
import com.mpi.demo.dto.response.PatientResponse;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.SourceSystem;
import com.mpi.demo.exception.DuplicateResourceException;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.repository.SourceSystemRepository;
import com.mpi.demo.service.PatientService;
import com.mpi.demo.specification.PatientSpecification;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final SourceSystemRepository sourceSystemRepository;

    public PatientServiceImpl(PatientRepository patientRepository,
            SourceSystemRepository sourceSystemRepository) {
        this.patientRepository = patientRepository;
        this.sourceSystemRepository = sourceSystemRepository;
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
        SourceSystem sourceSystem = sourceSystemRepository.findById(request.getSourceSystemId())
                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống nguồn", "id", request.getSourceSystemId()));

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
        return PatientResponse.fromEntity(saved);
    }

    @Override
    public PatientResponse update(UpdatePatientRequest request) {
        Patient patient = this.patientRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân", "mã bệnh nhân", request.getId()));

        SourceSystem sourceSystem = sourceSystemRepository.findById(request.getSourceSystemId())
                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống nguồn", "id", request.getSourceSystemId()));

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

}
