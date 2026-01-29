package com.badminton.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                                .components(new Components().addSecuritySchemes("Bearer Authentication",
                                                createAPIKeyScheme()))
                                .info(new Info()
                                                .title("Badminton Management API")
                                                .description("API documentation for Badminton Court & Shop Management System")
                                                .version("1.0")
                                                .contact(new Contact()
                                                                .name("Dev Team")
                                                                .email("admin@badminton.com")
                                                                .url("https://badminton-app.com"))
                                                .license(new License()
                                                                .name("Apache 2.0")
                                                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
        }

        private SecurityScheme createAPIKeyScheme() {
                return new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .bearerFormat("JWT")
                                .scheme("bearer");
        }
}
// fgfhgh