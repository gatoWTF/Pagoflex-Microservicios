package com.pagoflex.compromisos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("PagoFlex - compromisos-service")
                .version("1.0.0")
                .description("Compromisos de pago (cuotas), pagos, comprobantes y reportes de problemas."));
    }
}
