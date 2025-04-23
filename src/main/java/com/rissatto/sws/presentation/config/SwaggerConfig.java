package com.rissatto.sws.presentation.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Wallet API", version = "1.0", description = "Simple Wallet Service API Documentation"))
public class SwaggerConfig {
}
