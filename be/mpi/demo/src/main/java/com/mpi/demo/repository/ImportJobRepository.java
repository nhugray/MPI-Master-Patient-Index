package com.mpi.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.mpi.demo.constant.ImportJobStatusEnum;
import com.mpi.demo.entity.ImportJob;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long>, JpaSpecificationExecutor<ImportJob> {
    Page<ImportJob> findBySourceSystemIdOrderByCreatedAtDesc(Long sourceSystemId, Pageable pageable);

    Page<ImportJob> findByStatusOrderByCreatedAtDesc(ImportJobStatusEnum status, Pageable pageable);

    Page<ImportJob> findByCreatedByOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT ij FROM ImportJob ij WHERE ij.status IN ('PENDING', 'VALIDATING', 'PROCESSING') ORDER BY ij.createdAt DESC")
    Page<ImportJob> findActiveJobs(Pageable pageable);

}