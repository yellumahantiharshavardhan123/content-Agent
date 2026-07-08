package com.arjunsports.contentagent.modules.approval.repository;

import com.arjunsports.contentagent.modules.approval.entity.Approval;
import com.arjunsports.contentagent.modules.approval.entity.ApprovalStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Composes the optional search/filter parameters accepted by {@code GET /api/approval/pending}. */
public final class ApprovalSpecifications {

    private ApprovalSpecifications() {
    }

    public static Specification<Approval> build(ApprovalStatus status, String search, Instant dateFrom, Instant dateTo) {
        List<Specification<Approval>> specs = new ArrayList<>();
        specs.add(hasStatus(status));
        specs.add(searchTitle(search));
        specs.add(createdFrom(dateFrom));
        specs.add(createdTo(dateTo));

        return specs.stream()
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse(null);
    }

    private static Specification<Approval> hasStatus(ApprovalStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Approval> searchTitle(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("contentTitle")), pattern);
    }

    private static Specification<Approval> createdFrom(Instant dateFrom) {
        if (dateFrom == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom);
    }

    private static Specification<Approval> createdTo(Instant dateTo) {
        if (dateTo == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), dateTo);
    }
}
