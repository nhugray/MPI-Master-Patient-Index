package com.mpi.demo.service;

import org.springframework.data.domain.Pageable;

import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientMasterRequest;
import com.mpi.demo.dto.response.PatientMasterResponse;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;
import com.mpi.demo.helper.ResultPagination;

public interface PatientMasterService {
    PatientMaster createMaster(Patient patient);

    void linkToMaster(Long patientId, Long masterId);

    // PatientMaster mergeMasters(Long sourceId, Long targetId, Long userId);

    void unlinkPatient(Long patientId);

    String generateEnterpriseId();

    ResultPagination searchMasters(PatientMasterSearchRequest request, Pageable pageable);

    PatientMasterResponse getById(Long id);

    PatientMasterResponse update(UpdatePatientMasterRequest request);

}
