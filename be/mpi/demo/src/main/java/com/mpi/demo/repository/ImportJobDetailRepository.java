package com.mpi.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.mpi.demo.constant.ImportRowStatusEnum;
import com.mpi.demo.entity.ImportJobDetail;

public interface ImportJobDetailRepository
        extends JpaRepository<ImportJobDetail, Long>, JpaSpecificationExecutor<ImportJobDetail> {
    Page<ImportJobDetail> findByImportJobIdAndStatus(Long importJobId, ImportRowStatusEnum status,Pageable pageable);

    Page<ImportJobDetail> findByImportJobIdOrderByRowNumber(Long importJobId,Pageable pageable);

    @Query("SELECT COUNT(d) FROM ImportJobDetail d WHERE d.importJob.id = ?1 AND d.status = ?2")
    long countByImportJobIdAndStatus(Long importJobId, ImportRowStatusEnum status); 
}
