package com.mpi.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.mpi.demo.entity.Patient;
import com.mpi.demo.entity.PatientMaster;

public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    Boolean existsBySourceSystemIdAndLocalPatientCode(Long sourceSystemId, String localPatientCode);

    Page<Patient> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    Optional<Patient> findByNationalId(String nationalId);

    Optional<Patient> findBySourceSystemIdAndLocalPatientCode(Long sourceSystemId, String localPatientCode);

    Page<Patient> findBySourceSystemId(Long sourceSystemId, Pageable pageable);

    Long countByMasterPatient(PatientMaster masterPatient);

}