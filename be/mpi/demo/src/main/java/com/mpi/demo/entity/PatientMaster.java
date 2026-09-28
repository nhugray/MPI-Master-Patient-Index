package com.mpi.demo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.PatientStatusEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "patient_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enterprise_id", unique = true, nullable = false, length = 30)
    private String enterpriseId; // Format: EMPI-2026-000001

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private PatientStatusEnum status = PatientStatusEnum.ACTIVE;

    @Column(name = "merged_into_id")
    private Long mergedIntoId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
