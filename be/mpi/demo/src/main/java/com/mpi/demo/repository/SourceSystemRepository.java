package com.mpi.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.mpi.demo.entity.SourceSystem;

public interface SourceSystemRepository extends JpaRepository<SourceSystem, Long>, JpaSpecificationExecutor<SourceSystem> {
    Boolean existsByCode(String code);

    Page<SourceSystem> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<SourceSystem> findByCode(String code);
}
