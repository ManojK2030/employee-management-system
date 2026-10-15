package com.ems.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI employeeApi() {
        return new OpenAPI().info(new Info()
                .title("Employee Management System API")
                .version("1.0.0")
                .description("REST API for managing employee records: CRUD, search, filters, pagination and CSV export."));
    }
}
