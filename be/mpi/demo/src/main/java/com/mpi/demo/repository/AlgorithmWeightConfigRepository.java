package com.mpi.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mpi.demo.entity.AlgorithmWeightConfig;

public interface AlgorithmWeightConfigRepository extends JpaRepository<AlgorithmWeightConfig, Long> {

    Optional<AlgorithmWeightConfig> findByIsActiveTrue();

    Optional<AlgorithmWeightConfig> findByVersion(String version);

    List<AlgorithmWeightConfig> findAllByOrderByCreatedAtDesc();

    boolean existsByVersion(String version);

}
