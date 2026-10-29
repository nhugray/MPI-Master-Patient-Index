package com.mpi.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientMasterRequest;
import com.mpi.demo.dto.response.PatientMasterHistoryResponse;
import com.mpi.demo.dto.response.PatientMasterResponse;
import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;
import com.mpi.demo.helper.ResultPagination;

public interface PatientMasterService {
    PatientMaster createMaster(Patient patient);

    void linkToMaster(Long patientId, Long masterId);

    void unlinkPatient(Long patientId);

    void deleteMaster(Long id);

    String generateEnterpriseId();

    ResultPagination searchMasters(PatientMasterSearchRequest request, Pageable pageable);

    PatientMasterResponse getById(Long id);

    PatientMasterResponse update(UpdatePatientMasterRequest request);

    List<PatientMasterHistoryResponse> getHistory(Long masterId);

    Map<String, Long> getMasterCounts();
}
