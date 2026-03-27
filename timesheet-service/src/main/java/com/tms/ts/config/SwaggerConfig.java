package com.tms.ts.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway"),
                        new Server().url("http://localhost:8082").description("Local Timesheet Service")
                ))
                .info(new Info()
                        .title("Timesheet Service API")
                        .description("Timesheet Management Service")
                        .version("1.0"));
    }

    @Bean
    public OpenApiCustomizer hideGatewayInjectedHeaders() {
        Set<String> hiddenHeaders = Set.of("X-User-Id", "X-User-Email", "X-User-Role");

        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getParameters() == null) {
                            return;
                        }

                        List<io.swagger.v3.oas.models.parameters.Parameter> visibleParameters =
                                operation.getParameters().stream()
                                        .filter(parameter -> !hiddenHeaders.contains(parameter.getName()))
                                        .collect(Collectors.toList());
                        operation.setParameters(visibleParameters);
                    })
            );
        };
    }
}
