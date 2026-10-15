package com.ems.service;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.PageResponse;
import com.ems.entity.Employee;
import com.ems.entity.EmploymentStatus;
import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.repository.EmployeeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeServiceImpl service;

    // ------------------------------------------------------------ test data helpers

    private static EmployeeRequest request(String email) {
        return new EmployeeRequest(
                "  Priya Sharma ", email, "+91 98765 43210", " Engineering ", "Backend Developer",
                LocalDate.of(2024, 7, 1), new BigDecimal("850000.00"), EmploymentStatus.ACTIVE);
    }

    private static Employee employee(Long id, String email) {
        Employee e = new Employee();
        e.setId(id);
        e.setFullName("Priya Sharma");
        e.setEmail(email);
        e.setPhoneNumber("+91 98765 43210");
        e.setDepartment("Engineering");
        e.setDesignation("Backend Developer");
        e.setDateOfJoining(LocalDate.of(2024, 7, 1));
        e.setSalary(new BigDecimal("850000.00"));
        e.setStatus(EmploymentStatus.ACTIVE);
        return e;
    }

    // ------------------------------------------------------------ create

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves the employee with trimmed fields and a lower-cased email")
        void savesNormalizedEmployee() {
            when(repository.existsByEmailIgnoreCase("priya.sharma@example.com")).thenReturn(false);
            when(repository.save(any(Employee.class))).thenAnswer(inv -> {
                Employee saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            EmployeeResponse response = service.create(request("  Priya.Sharma@Example.COM "));

            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(repository).save(captor.capture());
            Employee persisted = captor.getValue();
            assertThat(persisted.getEmail()).isEqualTo("priya.sharma@example.com");
            assertThat(persisted.getFullName()).isEqualTo("Priya Sharma");
            assertThat(persisted.getDepartment()).isEqualTo("Engineering");

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("priya.sharma@example.com");
            assertThat(response.status()).isEqualTo(EmploymentStatus.ACTIVE);
        }

        @Test
        @DisplayName("rejects a duplicate email and never saves")
        void rejectsDuplicateEmail() {
            when(repository.existsByEmailIgnoreCase("priya.sharma@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.create(request("priya.sharma@example.com")))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("priya.sharma@example.com")
                    .extracting(ex -> ((DuplicateResourceException) ex).getField())
                    .isEqualTo("email");

            verify(repository, never()).save(any());
        }
    }

    // ------------------------------------------------------------ read

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        void returnsEmployeeWhenFound() {
            when(repository.findById(5L)).thenReturn(Optional.of(employee(5L, "priya.sharma@example.com")));

            EmployeeResponse response = service.getById(5L);

            assertThat(response.id()).isEqualTo(5L);
            assertThat(response.fullName()).isEqualTo("Priya Sharma");
            assertThat(response.salary()).isEqualByComparingTo("850000.00");
        }

        @Test
        void throwsWhenMissing() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Employee not found with id 99");
        }
    }

    // ------------------------------------------------------------ update

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates every field of an existing employee")
        void updatesFields() {
            Employee existing = employee(7L, "old@example.com");
            when(repository.findById(7L)).thenReturn(Optional.of(existing));
            when(repository.existsByEmailIgnoreCaseAndIdNot("priya.sharma@example.com", 7L)).thenReturn(false);
            when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeRequest changes = new EmployeeRequest(
                    "Priya S", "Priya.Sharma@example.com", "9876543210", "Finance", "Analyst",
                    LocalDate.of(2023, 1, 15), new BigDecimal("500000"), EmploymentStatus.INACTIVE);

            EmployeeResponse response = service.update(7L, changes);

            assertThat(response.id()).isEqualTo(7L);
            assertThat(response.fullName()).isEqualTo("Priya S");
            assertThat(response.email()).isEqualTo("priya.sharma@example.com");
            assertThat(response.department()).isEqualTo("Finance");
            assertThat(response.status()).isEqualTo(EmploymentStatus.INACTIVE);
            assertThat(response.salary()).isEqualByComparingTo("500000");
        }

        @Test
        @DisplayName("lets an employee keep their own email (the duplicate check excludes their own id)")
        void allowsKeepingOwnEmail() {
            when(repository.findById(7L)).thenReturn(Optional.of(employee(7L, "priya.sharma@example.com")));
            when(repository.existsByEmailIgnoreCaseAndIdNot("priya.sharma@example.com", 7L)).thenReturn(false);
            when(repository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

            EmployeeResponse response = service.update(7L, request("priya.sharma@example.com"));

            assertThat(response.email()).isEqualTo("priya.sharma@example.com");
            verify(repository, never()).existsByEmailIgnoreCase(anyString());
        }

        @Test
        @DisplayName("rejects an email that belongs to a different employee")
        void rejectsEmailOfAnotherEmployee() {
            when(repository.findById(7L)).thenReturn(Optional.of(employee(7L, "old@example.com")));
            when(repository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 7L)).thenReturn(true);

            assertThatThrownBy(() -> service.update(7L, request("taken@example.com")))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void throwsWhenEmployeeMissing() {
            when(repository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(42L, request("a@example.com")))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).save(any());
        }
    }

    // ------------------------------------------------------------ delete

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        void deletesExistingEmployee() {
            when(repository.existsById(3L)).thenReturn(true);

            service.delete(3L);

            verify(repository).deleteById(3L);
        }

        @Test
        void throwsWhenEmployeeMissing() {
            when(repository.existsById(3L)).thenReturn(false);

            assertThatThrownBy(() -> service.delete(3L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(repository, never()).deleteById(anyLong());
        }
    }

    // ------------------------------------------------------------ search / pagination

    @Nested
    @DisplayName("search")
    class Search {

        @SuppressWarnings("unchecked")
        private Page<Employee> stubPage(List<Employee> content, Pageable pageable, long total) {
            Page<Employee> page = new PageImpl<>(content, pageable, total);
            when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            return page;
        }

        @Test
        @DisplayName("maps the page and passes page/size/sort to the repository")
        void mapsPageAndBuildsPageable() {
            Pageable expected = PageRequest.of(1, 5,
                    Sort.by(Sort.Direction.ASC, "fullName").and(Sort.by(Sort.Direction.ASC, "id")));
            stubPage(List.of(employee(1L, "a@example.com"), employee(2L, "b@example.com")), expected, 12);

            PageResponse<EmployeeResponse> result =
                    service.search("pri", "Engineering", EmploymentStatus.ACTIVE, 1, 5, "fullName", "asc");

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findAll(any(Specification.class), captor.capture());
            Pageable used = captor.getValue();
            assertThat(used.getPageNumber()).isEqualTo(1);
            assertThat(used.getPageSize()).isEqualTo(5);
            assertThat(used.getSort().getOrderFor("fullName").getDirection()).isEqualTo(Sort.Direction.ASC);
            // tie-breaker keeps pagination stable
            assertThat(used.getSort().getOrderFor("id")).isNotNull();

            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).email()).isEqualTo("a@example.com");
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(5);
            assertThat(result.totalElements()).isEqualTo(12);
            assertThat(result.totalPages()).isEqualTo(3);
            assertThat(result.first()).isFalse();
            assertThat(result.last()).isFalse();
        }

        @Test
        @DisplayName("returns an empty page when nothing matches")
        void emptyResult() {
            stubPage(List.of(), PageRequest.of(0, 10), 0);

            PageResponse<EmployeeResponse> result = service.search(null, null, null, 0, 10, "id", "desc");

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        void rejectsNegativePage() {
            assertThatThrownBy(() -> service.search(null, null, null, -1, 10, "id", "desc"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("page");
        }

        @Test
        void rejectsSizeOutsideAllowedRange() {
            assertThatThrownBy(() -> service.search(null, null, null, 0, 0, "id", "desc"))
                    .isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> service.search(null, null, null, 0, 101, "id", "desc"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("rejects a sort field that is not on the whitelist")
        void rejectsUnknownSortField() {
            assertThatThrownBy(() -> service.search(null, null, null, 0, 10, "password; drop table", "asc"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot sort by");
        }

        @Test
        void rejectsUnknownSortDirection() {
            assertThatThrownBy(() -> service.search(null, null, null, 0, 10, "id", "sideways"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("sortDir");
        }
    }

    // ------------------------------------------------------------ departments

    @Test
    @DisplayName("getDepartments removes case-insensitive duplicates and blanks")
    void departmentsAreDeduplicated() {
        when(repository.findDistinctDepartments())
                .thenReturn(List.of("Engineering", "engineering", "Finance", " ", "HR"));

        assertThat(service.getDepartments()).containsExactly("Engineering", "Finance", "HR");
    }

    // ------------------------------------------------------------ CSV export

    @Nested
    @DisplayName("exportCsv")
    class ExportCsv {

        @Test
        @DisplayName("writes the header and one row per employee")
        @SuppressWarnings("unchecked")
        void writesHeaderAndRows() {
            when(repository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(employee(1L, "priya.sharma@example.com")));

            String csv = service.exportCsv(null, null, null);

            String[] lines = csv.split("\r\n");
            assertThat(lines).hasSize(2);
            assertThat(lines[0]).isEqualTo(
                    "Employee ID,Full Name,Email,Phone Number,Department,Designation,Date of Joining,Salary,Status");
            assertThat(lines[1]).isEqualTo(
                    "1,Priya Sharma,priya.sharma@example.com,+91 98765 43210,Engineering,Backend Developer,"
                            + "2024-07-01,850000.00,ACTIVE");
        }

        @Test
        @DisplayName("quotes commas and double quotes")
        void quotesSpecialCharacters() {
            assertThat(EmployeeServiceImpl.csvField("Smith, John")).isEqualTo("\"Smith, John\"");
            assertThat(EmployeeServiceImpl.csvField("The \"Boss\"")).isEqualTo("\"The \"\"Boss\"\"\"");
            assertThat(EmployeeServiceImpl.csvField(null)).isEmpty();
        }

        @Test
        @DisplayName("neutralises spreadsheet formulas but leaves phone numbers alone")
        void guardsAgainstFormulaInjection() {
            assertThat(EmployeeServiceImpl.csvField("=SUM(A1:A9)")).isEqualTo("'=SUM(A1:A9)");
            assertThat(EmployeeServiceImpl.csvField("@cmd")).isEqualTo("'@cmd");
            assertThat(EmployeeServiceImpl.csvField("+91 98765 43210")).isEqualTo("+91 98765 43210");
            assertThat(EmployeeServiceImpl.csvField("080-12345678")).isEqualTo("080-12345678");
        }
    }
}
