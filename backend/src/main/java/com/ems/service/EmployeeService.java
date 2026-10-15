package com.ems.service;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.PageResponse;
import com.ems.entity.EmploymentStatus;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse getById(Long id);

    EmployeeResponse update(Long id, EmployeeRequest request);

    void delete(Long id);

    PageResponse<EmployeeResponse> search(String search, String department, EmploymentStatus status,
                                          int page, int size, String sortBy, String sortDir);

    List<String> getDepartments();

    /** CSV (header + one row per matching employee) for the same filters the list endpoint accepts. */
    String exportCsv(String search, String department, EmploymentStatus status);
}
