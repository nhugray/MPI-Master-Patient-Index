package com.mpi.demo.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.mpi.demo.dto.request.SourceSystemSearchRequest;
import com.mpi.demo.entity.SourceSystem;

import jakarta.persistence.criteria.Predicate;

public class SourceSystemSpecification {

    public static Specification<SourceSystem> build(
            SourceSystemSearchRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            List<Predicate> keywordPredicates = new ArrayList<>();

            if (isNotBlank(request.name())) {
                keywordPredicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + request.name().toLowerCase().trim() + "%"));
            }

            if (isNotBlank(request.code())) {
                keywordPredicates.add(
                        cb.like(
                                cb.lower(root.get("code")),
                                "%" + request.code().toLowerCase().trim() + "%"));
            }

            if (!keywordPredicates.isEmpty()) {
                predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
            }

            if (request.facilityId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("facility").get("id"),
                                request.facilityId()));
            }

            if (request.isActive() != null) {
                predicates.add(
                        cb.equal(
                                root.get("isActive"),
                                request.isActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
