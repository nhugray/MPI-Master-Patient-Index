package com.mpi.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.mpi.demo.constant.FileTypeEnum;
import com.mpi.demo.constant.ImportJobStatusEnum;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "import_job")
@Getter
@Setter
public class ImportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_token", length = 100)
    private String fileToken;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private FileTypeEnum fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_system_id", nullable = false)
    private SourceSystem sourceSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ImportJobStatusEnum status = ImportJobStatusEnum.PENDING;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "processed_rows")
    private Integer processedRows = 0;

    @Column(name = "successful_rows")
    private Integer successfulRows = 0;

    @Column(name = "failed_rows")
    private Integer failedRows = 0;

    @Column(name = "duplicate_rows")
    private Integer duplicateRows = 0;

    @Column(name = "warning_rows")
    private Integer warningRows = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "configuration", columnDefinition = "JSON")
    private String configuration;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "importJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportJobDetail> details = new ArrayList<>();

    public ImportJob() {
        super();
    }

    public ImportJob(Long id, String fileToken, String fileName, Long fileSize, FileTypeEnum fileType,
            SourceSystem sourceSystem, ImportJobStatusEnum status, Integer totalRows, Integer processedRows,
            Integer successfulRows, Integer failedRows, Integer duplicateRows, Integer warningRows,
            LocalDateTime startedAt, LocalDateTime completedAt, String errorMessage, String configuration,
            Long createdBy, LocalDateTime createdAt, LocalDateTime updatedAt, List<ImportJobDetail> details) {
        this.id = id;
        this.fileToken = fileToken;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.sourceSystem = sourceSystem;
        this.status = status != null ? status : ImportJobStatusEnum.PENDING;
        this.totalRows = totalRows != null ? totalRows : 0;
        this.processedRows = processedRows != null ? processedRows : 0;
        this.successfulRows = successfulRows != null ? successfulRows : 0;
        this.failedRows = failedRows != null ? failedRows : 0;
        this.duplicateRows = duplicateRows != null ? duplicateRows : 0;
        this.warningRows = warningRows != null ? warningRows : 0;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
        this.configuration = configuration;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.details = details != null ? details : new ArrayList<>();
    }

    public static ImportJobBuilder builder() {
        return new ImportJobBuilder();
    }

    public static class ImportJobBuilder {
        private Long id;
        private String fileToken;
        private String fileName;
        private Long fileSize;
        private FileTypeEnum fileType;
        private SourceSystem sourceSystem;
        private ImportJobStatusEnum status = ImportJobStatusEnum.PENDING;
        private Integer totalRows = 0;
        private Integer processedRows = 0;
        private Integer successfulRows = 0;
        private Integer failedRows = 0;
        private Integer duplicateRows = 0;
        private Integer warningRows = 0;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String errorMessage;
        private String configuration;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ImportJobDetail> details = new ArrayList<>();

        ImportJobBuilder() {
        }

        public ImportJobBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ImportJobBuilder fileToken(String fileToken) {
            this.fileToken = fileToken;
            return this;
        }

        public ImportJobBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public ImportJobBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public ImportJobBuilder fileType(FileTypeEnum fileType) {
            this.fileType = fileType;
            return this;
        }

        public ImportJobBuilder sourceSystem(SourceSystem sourceSystem) {
            this.sourceSystem = sourceSystem;
            return this;
        }

        public ImportJobBuilder status(ImportJobStatusEnum status) {
            this.status = status;
            return this;
        }

        public ImportJobBuilder totalRows(Integer totalRows) {
            this.totalRows = totalRows;
            return this;
        }

        public ImportJobBuilder processedRows(Integer processedRows) {
            this.processedRows = processedRows;
            return this;
        }

        public ImportJobBuilder successfulRows(Integer successfulRows) {
            this.successfulRows = successfulRows;
            return this;
        }

        public ImportJobBuilder failedRows(Integer failedRows) {
            this.failedRows = failedRows;
            return this;
        }

        public ImportJobBuilder duplicateRows(Integer duplicateRows) {
            this.duplicateRows = duplicateRows;
            return this;
        }

        public ImportJobBuilder warningRows(Integer warningRows) {
            this.warningRows = warningRows;
            return this;
        }

        public ImportJobBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ImportJobBuilder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public ImportJobBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public ImportJobBuilder configuration(String configuration) {
            this.configuration = configuration;
            return this;
        }

        public ImportJobBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public ImportJobBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ImportJobBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ImportJobBuilder details(List<ImportJobDetail> details) {
            this.details = details;
            return this;
        }

        public ImportJob build() {
            return new ImportJob(id, fileToken, fileName, fileSize, fileType, sourceSystem, status, totalRows,
                    processedRows, successfulRows, failedRows, duplicateRows, warningRows, startedAt, completedAt,
                    errorMessage, configuration, createdBy, createdAt, updatedAt, details);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
