package com.cargoapp.backend.users.repository;

import com.cargoapp.backend.users.entity.UserEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserSpecification {

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
