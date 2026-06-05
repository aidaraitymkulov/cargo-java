package com.cargoapp.backend.users.repository;

import com.cargoapp.backend.users.entity.UserEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserSpecification {

    public static Specification<UserEntity> search(String query, UUID branchId) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("personalCode")), pattern)
                ));
            }

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<UserEntity> filter(String prefix, String code, UUID branchId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (prefix != null && !prefix.isBlank()) {
                predicates.add(cb.like(root.get("personalCode"), prefix + "%"));
            }

            if (code != null && !code.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("personalCode")), "%" + code.toLowerCase() + "%"));
            }

            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
