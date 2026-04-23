package com.imaginemos.todoapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI todoApiOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Todo API")
                .version("v1")
                .description("REST API for managing tasks with checkable items, statuses and search.")
                .contact(new Contact().name("Imaginemos").email("dev@imaginemos.com"))
                .license(new License().name("MIT")));
    }
}
