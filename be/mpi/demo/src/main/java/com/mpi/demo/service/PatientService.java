package com.mpi.demo.service;

import org.springframework.data.domain.Pageable;

import com.mpi.demo.dto.request.CreatePatientRequest;
import com.mpi.demo.dto.request.PatientSearchRequest;
import com.mpi.demo.dto.request.UpdatePatientRequest;
import com.mpi.demo.dto.response.PatientResponse;
import com.mpi.demo.helper.ResultPagination;

public interface PatientService {

    PatientResponse getById(Long id);

    PatientResponse create(CreatePatientRequest request);

    PatientResponse update(UpdatePatientRequest request);

    void deletePatient(Long id);

    ResultPagination search(PatientSearchRequest search, Pageable pageable);
}
