package com.mpi.demo.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.mpi.demo.dto.request.UserSearchRequest;
import com.mpi.demo.entity.Role;
import com.mpi.demo.entity.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class UserSpecification {

    public static Specification<User> build(UserSearchRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (request != null) {
                if (isNotBlank(request.keyword())) {
                    String like = "%" + request.keyword().toLowerCase().trim() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), like),
                            cb.like(cb.lower(root.get("email")), like)));
                }

                if (request.gender() != null) {
                    predicates.add(cb.equal(root.get("gender"), request.gender()));
                }

                if (request.status() != null) {
                    predicates.add(cb.equal(root.get("status"), request.status()));
                }

                if (request.roleId() != null) {
                    Join<User, Role> roles = root.join("roles", JoinType.INNER);
                    predicates.add(cb.equal(roles.get("id"), request.roleId()));
                    query.distinct(true);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
