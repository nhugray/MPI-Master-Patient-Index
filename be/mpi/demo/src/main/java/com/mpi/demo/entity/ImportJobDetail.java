package com.mpi.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mpi.demo.constant.ImportRowStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "import_job_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;
    
    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;
    
    @Column(name = "row_data", columnDefinition = "JSON", nullable = false)
    private String rowData;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ImportRowStatusEnum status;
    
    @Column(name = "match_score", precision = 5, scale = 2)
    private BigDecimal matchScore;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;
    
    @Column(name = "matched_master_id")
    private Long matchedMasterId;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "warning_message", columnDefinition = "TEXT")
    private String warningMessage;
    
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}
