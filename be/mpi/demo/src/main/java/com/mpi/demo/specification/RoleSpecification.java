package com.mpi.demo.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.mpi.demo.dto.request.RoleSearchRequest;
import com.mpi.demo.entity.Role;

import jakarta.persistence.criteria.Predicate;

public class RoleSpecification {

    public static Specification<Role> build(RoleSearchRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (request != null && isNotBlank(request.keyword())) {
                String like = "%" + request.keyword().toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
