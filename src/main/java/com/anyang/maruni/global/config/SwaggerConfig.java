package com.anyang.maruni.global.config;

import java.util.List;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

import com.anyang.maruni.global.config.properties.SwaggerProperties;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.SuccessResponseDescription;
import com.anyang.maruni.global.swagger.SwaggerExampleCustomizer;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "${swagger.api.title:MARUNI API Documentation}",
        description = "${swagger.api.description:REST API Documentation for MARUNI elderly care service}",
        version = "${swagger.api.version:v1.0.0}",
        contact = @Contact(
            name = "${swagger.contact.name:MARUNI Development Team}",
            email = "${swagger.contact.email:dev@maruni.com}",
            url = "${swagger.contact.url:https://github.com/maruni-project}"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    )
)
@SecurityScheme(
    name = "JWT",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER,
    paramName = "Authorization"
)
@Configuration
public class SwaggerConfig {

    private final SwaggerProperties swaggerProperties;
    private final SwaggerExampleCustomizer exampleCustomizer;
    
    public SwaggerConfig(SwaggerProperties swaggerProperties, SwaggerExampleCustomizer exampleCustomizer) {
        this.swaggerProperties = swaggerProperties;
        this.exampleCustomizer = exampleCustomizer;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl(swaggerProperties.getServer().getUrl());
        server.setDescription(swaggerProperties.getServer().getDescription());

        return new OpenAPI().servers(List.of(server));
    }

    @Bean
    public OperationCustomizer customize() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            CustomExceptionDescription customExceptionDescription = handlerMethod.getMethodAnnotation(
                CustomExceptionDescription.class);
            SuccessResponseDescription successResponseDescription = handlerMethod.getMethodAnnotation(
                SuccessResponseDescription.class);
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);

            // CustomExceptionDescription 어노테이션을 단 메소드에만 적용
            if (customExceptionDescription != null) {
                exampleCustomizer.addErrorExamples(operation.getResponses(), customExceptionDescription.value());
            }
            
            // SuccessResponseDescription 어노테이션을 단 메소드에만 적용
            if (successResponseDescription != null) {
                exampleCustomizer.addSuccessExample(operation.getResponses(), successResponseDescription.value());
            }
            
            if (preAuthorize != null) {
                operation.addSecurityItem(new SecurityRequirement().addList("JWT"));
            }

            return operation;
        };
    }
}
