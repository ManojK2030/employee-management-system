package com.ems.mapper;

import com.ems.dto.EmployeeResponse;
import com.ems.entity.Employee;

public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getFullName(),
                e.getEmail(),
                e.getPhoneNumber(),
                e.getDepartment(),
                e.getDesignation(),
                e.getDateOfJoining(),
                e.getSalary(),
                e.getStatus()
        );
    }
}
