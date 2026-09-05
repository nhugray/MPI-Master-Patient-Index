package com.mpi.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.mpi.demo.entity.Facility;

public interface FacilityRepository extends JpaRepository<Facility, Long>, JpaSpecificationExecutor<Facility> {
    Boolean existsByCode(String code);

    Page<Facility> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Facility> findByCode(String code);
}
