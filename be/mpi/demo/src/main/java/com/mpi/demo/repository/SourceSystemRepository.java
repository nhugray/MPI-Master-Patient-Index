package com.mpi.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mpi.demo.entity.SourceSystem;

public interface SourceSystemRepository extends JpaRepository<SourceSystem, Long> {
}
