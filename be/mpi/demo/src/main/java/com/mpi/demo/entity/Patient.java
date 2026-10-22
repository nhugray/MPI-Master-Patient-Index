package com.mpi.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.MatchStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "patient")
@Getter
@Setter
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_system_id", nullable = false)
    private SourceSystem sourceSystem;

    @Column(name = "local_patient_code", nullable = false, length = 100)
    private String localPatientCode;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private GenderEnum gender;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(name = "health_insurance_no", length = 20)
    private String healthInsuranceNo;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "address", length = 500)
    private String address;

    @ManyToOne
    @JoinColumn(name = "master_patient_id")
    private PatientMaster masterPatient;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 15)
    private MatchStatusEnum matchStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Patient() {
    }

    public Patient(Long id, SourceSystem sourceSystem, String localPatientCode, String fullName, LocalDate dateOfBirth,
            GenderEnum gender, String nationalId, String healthInsuranceNo, String phoneNumber, String address,
            PatientMaster masterPatient, MatchStatusEnum matchStatus, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.sourceSystem = sourceSystem;
        this.localPatientCode = localPatientCode;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationalId = nationalId;
        this.healthInsuranceNo = healthInsuranceNo;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.masterPatient = masterPatient;
        this.matchStatus = matchStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
