package com.ems.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test: HTTP -> controller -> service -> JPA -> H2.
 * Each test rolls back its own data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private Map<String, Object> body(String name, String email, String department, String status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fullName", name);
        m.put("email", email);
        m.put("phoneNumber", "+91 98765 43210");
        m.put("department", department);
        m.put("designation", "Developer");
        m.put("dateOfJoining", "2024-07-01");
        m.put("salary", 850000.00);
        m.put("status", status);
        return m;
    }

    private long create(String name, String email, String department, String status) throws Exception {
        MvcResult result = mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body(name, email, department, status))))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("POST creates an employee and returns 201 with a Location header")
    void createReturns201() throws Exception {
        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body("Priya Sharma", "Priya@Example.com", "Engineering", "ACTIVE"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("priya@example.com"))
                .andExpect(jsonPath("$.dateOfJoining").value("2024-07-01"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST with invalid data returns 400 with a message per field")
    void createValidationErrors() throws Exception {
        Map<String, Object> bad = body("", "not-an-email", "Engineering", "ACTIVE");
        bad.put("phoneNumber", "abc");
        bad.put("salary", -5);

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.salary").exists());
    }

    @Test
    @DisplayName("POST with an unknown status value returns 400, not 500")
    void createWithBadEnum() throws Exception {
        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body("A B", "ab@example.com", "HR", "RETIRED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("ACTIVE or INACTIVE")));
    }

    @Test
    @DisplayName("POST with an email already in use returns 409 (case-insensitive)")
    void duplicateEmail() throws Exception {
        create("First", "dup@example.com", "HR", "ACTIVE");

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body("Second", "DUP@example.com", "HR", "ACTIVE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("GET /{id} returns the employee, or 404 when it does not exist")
    void getById() throws Exception {
        long id = create("Priya Sharma", "priya@example.com", "Engineering", "ACTIVE");

        mvc.perform(get("/api/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Priya Sharma"));

        mvc.perform(get("/api/employees/{id}", 987654))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id 987654"));
    }

    @Test
    @DisplayName("PUT updates an employee; keeping the same email is allowed, taking another's is not")
    void updateChangesEmployee() throws Exception {
        long id = create("Priya Sharma", "priya@example.com", "Engineering", "ACTIVE");
        create("Other Person", "other@example.com", "HR", "ACTIVE");

        Map<String, Object> changes = body("Priya S.", "priya@example.com", "Finance", "INACTIVE");
        mvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(changes)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Priya S."))
                .andExpect(jsonPath("$.department").value("Finance"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        changes.put("email", "other@example.com");
        mvc.perform(put("/api/employees/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(changes)))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/employees/{id}", 987654)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(changes)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE removes the employee (204) and a second delete returns 404")
    void deleteRemovesEmployee() throws Exception {
        long id = create("To Delete", "delete@example.com", "HR", "ACTIVE");

        mvc.perform(delete("/api/employees/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/employees/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET list paginates, and search + department + status filters combine")
    void listSearchFilterPagination() throws Exception {
        for (int i = 1; i <= 12; i++) {
            create("Engineer " + i, "eng" + i + "@example.com", "Engineering", i % 2 == 0 ? "ACTIVE" : "INACTIVE");
        }
        create("Alice Johnson", "alice.j@corp.com", "Finance", "ACTIVE");
        create("Bob Smith", "bob.smith@corp.com", "Finance", "INACTIVE");

        // pagination: 14 rows, size 5 -> 3 pages
        mvc.perform(get("/api/employees").param("size", "5").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(14))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mvc.perform(get("/api/employees").param("size", "5").param("page", "2"))
                .andExpect(jsonPath("$.content.length()").value(4))
                .andExpect(jsonPath("$.last").value(true));

        // search by name (case-insensitive, partial)
        mvc.perform(get("/api/employees").param("search", "ALICE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Alice Johnson"));

        // search by email
        mvc.perform(get("/api/employees").param("search", "bob.smith@"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Bob Smith"));

        // filter by department
        mvc.perform(get("/api/employees").param("department", "finance"))
                .andExpect(jsonPath("$.totalElements").value(2));

        // filter by status
        mvc.perform(get("/api/employees").param("status", "INACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(7));

        // everything combined
        mvc.perform(get("/api/employees")
                        .param("search", "corp.com").param("department", "Finance").param("status", "ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Alice Johnson"));

        // a literal % must not behave as a wildcard
        mvc.perform(get("/api/employees").param("search", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));

        // sorting
        mvc.perform(get("/api/employees").param("sortBy", "fullName").param("sortDir", "asc").param("size", "1"))
                .andExpect(jsonPath("$.content[0].fullName").value("Alice Johnson"));
    }

    @Test
    @DisplayName("GET list rejects bad paging / sort / status parameters with 400")
    void listBadParameters() throws Exception {
        mvc.perform(get("/api/employees").param("size", "500")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees").param("sortBy", "nope")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/employees").param("status", "FIRED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("status")));
    }

    @Test
    @DisplayName("GET /departments returns distinct department names")
    void departments() throws Exception {
        create("A", "a@example.com", "Engineering", "ACTIVE");
        create("B", "b@example.com", "Engineering", "ACTIVE");
        create("C", "c@example.com", "Finance", "ACTIVE");

        mvc.perform(get("/api/employees/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$", hasItems("Engineering", "Finance")));
    }

    @Test
    @DisplayName("GET /export returns a CSV attachment that honours the filters")
    void exportCsv() throws Exception {
        create("Priya Sharma", "priya@example.com", "Engineering", "ACTIVE");
        create("Bob Smith", "bob@example.com", "Finance", "ACTIVE");

        MvcResult result = mvc.perform(get("/api/employees/export").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("employees.csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        String csv = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("Employee ID,Full Name,Email");
        assertThat(csv).contains("Priya Sharma");
        assertThat(csv).doesNotContain("Bob Smith");
    }

    @Test
    @DisplayName("Unknown routes return the same JSON error shape")
    void unknownRoute() throws Exception {
        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }
}
