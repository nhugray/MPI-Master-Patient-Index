package com.mpi.demo.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.mpi.demo.dto.request.PatientSearchRequest;
import com.mpi.demo.entity.Patient;

import jakarta.persistence.criteria.Predicate;

public class PatientSpecification {

    public static Specification<Patient> build(
            PatientSearchRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            List<Predicate> keywordPredicates = new ArrayList<>();

            if (isNotBlank(request.fullName())) {
                keywordPredicates.add(
                        cb.like(
                                cb.lower(root.get("fullName")),
                                "%" + request.fullName().toLowerCase().trim() + "%"));
            }

            if (isNotBlank(request.nationalId())) {
                keywordPredicates.add(
                        cb.like(
                                cb.lower(root.get("nationalId")),
                                "%" + request.nationalId().toLowerCase().trim() + "%"));
            }

            if (isNotBlank(request.phoneNumber())) {
                String phone = request.phoneNumber().replaceAll("\\s+", "").trim();
                if (!phone.isEmpty()) {
                    keywordPredicates.add(
                            cb.like(
                                    cb.lower(root.get("phoneNumber")),
                                    "%" + phone.toLowerCase() + "%"));
                }
            }

            if (isNotBlank(request.localPatientCode())) {
                keywordPredicates.add(
                        cb.like(
                                cb.lower(root.get("localPatientCode")),
                                "%" + request.localPatientCode().toLowerCase().trim() + "%"));
            }

            if (!keywordPredicates.isEmpty()) {
                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }

            if (request.gender() != null) {
                predicates.add(
                        cb.equal(
                                root.get("gender"),
                                request.gender()));
            }

            if (isNotBlank(request.healthInsuranceNo())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("healthInsuranceNo")),
                                "%" + request.healthInsuranceNo().toLowerCase().trim() + "%"));
            }

            if (isNotBlank(request.address())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("address")),
                                "%" + request.address().toLowerCase().trim() + "%"));
            }

            if (request.sourceSystemId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("sourceSystem").get("id"),
                                request.sourceSystemId()));
            }

            if (request.matchStatus() != null) {
                predicates.add(
                        cb.equal(
                                root.get("matchStatus"),
                                request.matchStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
