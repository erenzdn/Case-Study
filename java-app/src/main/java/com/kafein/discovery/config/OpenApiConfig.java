package com.kafein.discovery.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("LLM-Based Database Data Discovery System")
                .description("""
                    PII classification and database metadata extraction API.
                    
                    ## Authentication Flow
                    1. Call `POST /auth` with **Basic Auth** (username: `admin`, password: `admin123`)
                    2. Copy the `access_token` from the response
                    3. Click **Authorize** 🔒 → enter `Bearer <your-token>` in the **bearerAuth** field
                    4. All protected endpoints will now work with your JWT token
                    
                    > Token expires in 60 minutes. Call `/auth` again to refresh.
                    """)
                .version("1.0.0")
            )
            // Global Bearer JWT security (applied to all endpoints)
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter the JWT token obtained from POST /auth")
                )
                // Keep Basic Auth scheme for /auth endpoint only
                .addSecuritySchemes("basicAuth",
                    new SecurityScheme()
                        .name("basicAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Used ONLY for POST /auth to obtain JWT token")
                )
            );
    }
}
