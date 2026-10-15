package com.ems.repository;

import com.ems.entity.Employee;
import com.ems.entity.EmploymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the dynamic WHERE clause for the list/search/export endpoints.
 * Every filter is optional; filters that are supplied are combined with AND.
 */
public final class EmployeeSpecifications {

    private static final char ESCAPE = '!';

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> withFilters(String search, String department, EmploymentStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(search)) {
                String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern, ESCAPE),
                        cb.like(cb.lower(root.get("email")), pattern, ESCAPE)
                ));
            }
            if (hasText(department)) {
                predicates.add(cb.equal(cb.lower(root.get("department")), department.trim().toLowerCase(Locale.ROOT)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Makes %, _ and the escape char itself literal, so searching for "50%" does not act as a wildcard. */
    static String escapeLike(String value) {
        return value
                .replace(String.valueOf(ESCAPE), ESCAPE + String.valueOf(ESCAPE))
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
