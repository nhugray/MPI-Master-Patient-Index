package com.mpi.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.mpi.demo.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {
    Boolean existsByNationalId(String nationalId);

    Page<Patient> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    Optional<Patient> findByNationalId(String nationalId);
}