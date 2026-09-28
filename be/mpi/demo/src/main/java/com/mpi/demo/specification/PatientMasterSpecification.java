package com.mpi.demo.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.mpi.demo.dto.request.PatientMasterSearchRequest;
import com.mpi.demo.entity.PatientMaster;

import jakarta.persistence.criteria.Predicate;

public class PatientMasterSpecification {

    public static Specification<PatientMaster> build(PatientMasterSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isNotBlank(request.keyword())) {
                String keywordPattern = "%" + request.keyword().trim().toUpperCase() + "%";
                Predicate keywordPredicate = cb.or(
                        cb.like(cb.upper(root.get("enterpriseId")), keywordPattern),
                        cb.like(cb.upper(root.get("fullName")), keywordPattern),
                        cb.like(cb.upper(root.get("nationalId")), keywordPattern),
                        cb.like(cb.upper(root.get("healthInsuranceNo")), keywordPattern),
                        cb.like(cb.upper(root.get("phoneNumber")), keywordPattern));
                predicates.add(keywordPredicate);
            }

            if (isNotBlank(request.enterpriseId())) {
                predicates.add(cb.equal(root.get("enterpriseId"), request.enterpriseId().trim()));
            }

            if (isNotBlank(request.nationalId())) {
                predicates.add(cb.equal(root.get("nationalId"), request.nationalId().trim()));
            }

            if (isNotBlank(request.healthInsuranceNo())) {
                predicates.add(cb.equal(root.get("healthInsuranceNo"), request.healthInsuranceNo().trim()));
            }

            if (isNotBlank(request.phoneNumber())) {
                predicates.add(cb.equal(root.get("phoneNumber"), request.phoneNumber().trim()));
            }

            if (request.gender() != null) {
                predicates.add(cb.equal(root.get("gender"), request.gender()));
            }

            if (request.status() != null) {
                predicates.add(cb.equal(root.get("status"), request.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
