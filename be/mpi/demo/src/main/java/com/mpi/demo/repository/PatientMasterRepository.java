package com.mpi.demo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.mpi.demo.constant.PatientStatusEnum;
import com.mpi.demo.entity.PatientMaster;

public interface PatientMasterRepository
              extends JpaRepository<PatientMaster, Long>, JpaSpecificationExecutor<PatientMaster> {

       @Query("SELECT pm FROM PatientMaster pm WHERE " +
                     "pm.dateOfBirth = ?1 AND " +
                     "UPPER(SUBSTRING(pm.fullName, 1, 3)) = ?2 AND " +
                     "pm.status = 'ACTIVE'")
       List<PatientMaster> findByDateOfBirthAndFirst3Chars(LocalDate dob, String first3Chars);

       @Query("SELECT pm FROM PatientMaster pm WHERE " +
                     "pm.phoneNumber LIKE %?1 AND " +
                     "pm.status = 'ACTIVE'")
       List<PatientMaster> findByPhoneLast4Digits(String last4Digits);

       @Query("SELECT pm FROM PatientMaster pm WHERE " +
                     "pm.fullName LIKE %?1% AND " +
                     "pm.status = 'ACTIVE'")
       List<PatientMaster> findByNameSoundex(String normalizedName);

       @Query("SELECT pm FROM PatientMaster pm WHERE " +
                     "pm.nationalId = ?1 AND " +
                     "pm.status = 'ACTIVE'")
       Optional<PatientMaster> findByNationalId(String nationalId);

       Optional<PatientMaster> findByNationalIdAndStatus(String nationalId, PatientStatusEnum status);

       Optional<PatientMaster> findByHealthInsuranceNoAndStatus(String healthInsuranceNo, PatientStatusEnum status);

       default Optional<PatientMaster> findByHealthInsuranceNo(String healthInsuranceNo) {
              return findByHealthInsuranceNoAndStatus(healthInsuranceNo, PatientStatusEnum.ACTIVE);
       }

       List<PatientMaster> findByPhoneNumberAndStatus(String phoneNumber, PatientStatusEnum status);

       default List<PatientMaster> findByPhoneNumber(String phoneNumber) {
              return findByPhoneNumberAndStatus(phoneNumber, PatientStatusEnum.ACTIVE);
       }

       Optional<PatientMaster> findByEnterpriseId(String enterpriseId);
}
