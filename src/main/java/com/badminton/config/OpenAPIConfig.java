package com.badminton.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🏸 Badminton Court Management API")
                        .version("1.0.0")
                        .description("REST API cho hệ thống quản lý sân cầu lông\n\n" +
                                "**Features:**\n" +
                                "- 🔐 Authentication & Authorization (JWT)\n" +
                                "- 📅 Booking System\n" +
                                "- 💳 Payment Integration (MoMo)\n" +
                                "- 🛍️ E-commerce\n" +
                                "- 👑 User Tier System\n" +
                                "- 🤖 AI Chatbot\n" +
                                "- 📍 Location-based Search")
                        .contact(new Contact()
                                .name("Hoàng Thanh Hồng")
                                .email("hoanghong76543@gmail.com")
                                .url("https://github.com/hoangthanh999/demobackendb"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")));
    }
}
