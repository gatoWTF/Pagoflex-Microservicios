package com.pagoflex.roles.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("PagoFlex - roles-service")
                .version("1.0.0")
                .description("Roles de las cuentas: usuario final, ejecutivo de empresa y agente PagoFlex."));
    }
}
