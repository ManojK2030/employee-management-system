package com.ems.service;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.PageResponse;
import com.ems.entity.Employee;
import com.ems.entity.EmploymentStatus;
import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.mapper.EmployeeMapper;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.EmployeeSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    static final int MAX_PAGE_SIZE = 100;

    /** Whitelist: the sortBy request parameter is never passed to the database unchecked. */
    static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "fullName", "email", "department", "designation", "dateOfJoining", "salary", "status");

    private static final String CSV_HEADER =
            "Employee ID,Full Name,Email,Phone Number,Department,Designation,Date of Joining,Salary,Status";
    private static final String CSV_NEWLINE = "\r\n";
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";
    /** Plain phone-like values (+91 98765-43210) are safe in Excel, so they are not prefixed. */
    private static final Pattern NUMERIC_LIKE = Pattern.compile("^[+-]?[0-9][0-9 \\-]*$");

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw emailInUse(email);
        }

        Employee employee = new Employee();
        applyRequest(employee, request, email);
        Employee saved = employeeRepository.save(employee);
        log.info("Created employee id={}", saved.getId());
        return EmployeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse getById(Long id) {
        return EmployeeMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findOrThrow(id);

        String email = normalizeEmail(request.email());
        // "AndIdNot": the employee may keep their own email, but not take someone else's.
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw emailInUse(email);
        }

        applyRequest(employee, request, email);
        Employee saved = employeeRepository.save(employee);
        log.info("Updated employee id={}", saved.getId());
        return EmployeeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        employeeRepository.deleteById(id);
        log.info("Deleted employee id={}", id);
    }

    @Override
    public PageResponse<EmployeeResponse> search(String search, String department, EmploymentStatus status,
                                                 int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Specification<Employee> spec = EmployeeSpecifications.withFilters(search, department, status);

        Page<Employee> result = employeeRepository.findAll(spec, pageable);
        return PageResponse.from(result.map(EmployeeMapper::toResponse));
    }

    @Override
    public List<String> getDepartments() {
        // De-duplicate case-insensitively so "engineering" and "Engineering" show up once in the dropdown.
        Map<String, String> unique = new LinkedHashMap<>();
        for (String department : employeeRepository.findDistinctDepartments()) {
            if (department != null && !department.isBlank()) {
                unique.putIfAbsent(department.toLowerCase(Locale.ROOT), department);
            }
        }
        return new ArrayList<>(unique.values());
    }

    @Override
    public String exportCsv(String search, String department, EmploymentStatus status) {
        Specification<Employee> spec = EmployeeSpecifications.withFilters(search, department, status);
        List<Employee> employees = employeeRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));

        StringBuilder csv = new StringBuilder(CSV_HEADER).append(CSV_NEWLINE);
        for (Employee e : employees) {
            csv.append(String.join(",",
                    csvField(String.valueOf(e.getId())),
                    csvField(e.getFullName()),
                    csvField(e.getEmail()),
                    csvField(e.getPhoneNumber()),
                    csvField(e.getDepartment()),
                    csvField(e.getDesignation()),
                    csvField(String.valueOf(e.getDateOfJoining())),
                    csvField(e.getSalary() == null ? "" : e.getSalary().toPlainString()),
                    csvField(String.valueOf(e.getStatus()))
            )).append(CSV_NEWLINE);
        }
        return csv.toString();
    }

    // ---------------------------------------------------------------- helpers

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private static DuplicateResourceException emailInUse(String email) {
        return new DuplicateResourceException("email", "An employee with email '" + email + "' already exists");
    }

    /** Emails are stored trimmed and lower-case so "A@x.com" and "a@x.com" cannot both exist. */
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void applyRequest(Employee employee, EmployeeRequest request, String normalizedEmail) {
        employee.setFullName(request.fullName().trim());
        employee.setEmail(normalizedEmail);
        employee.setPhoneNumber(request.phoneNumber().trim());
        employee.setDepartment(request.department().trim());
        employee.setDesignation(request.designation().trim());
        employee.setDateOfJoining(request.dateOfJoining());
        employee.setSalary(request.salary());
        employee.setStatus(request.status());
    }

    static Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new BadRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (sortBy == null || !SORTABLE_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    "Cannot sort by '" + sortBy + "'. Allowed fields: " + new TreeSet<>(SORTABLE_FIELDS));
        }
        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDir)
                .orElseThrow(() -> new BadRequestException("sortDir must be 'asc' or 'desc'"));

        Sort sort = Sort.by(direction, sortBy);
        if (!"id".equals(sortBy)) {
            // Tie-breaker: without a unique sort key, rows with equal values can move between pages.
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }

    /** RFC 4180 quoting, plus a guard against spreadsheet formula injection (=, +, -, @ at the start). */
    static String csvField(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty()
                && FORMULA_TRIGGERS.indexOf(safe.charAt(0)) >= 0
                && !NUMERIC_LIKE.matcher(safe).matches()) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
