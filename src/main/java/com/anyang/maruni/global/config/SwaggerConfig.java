package com.anyang.maruni.global.config;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

import com.anyang.maruni.global.advice.ParameterData;
import com.anyang.maruni.global.response.dto.CommonApiResponse;
import com.anyang.maruni.global.response.error.ErrorCode;
import com.anyang.maruni.global.response.success.SuccessCode;
import com.anyang.maruni.global.swagger.CustomExceptionDescription;
import com.anyang.maruni.global.swagger.ExampleHolder;
import com.anyang.maruni.global.swagger.SuccessResponseDescription;
import com.anyang.maruni.global.swagger.SwaggerResponseDescription;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "Spring Login API 문서",
        description = "JWT 인증 기반 로그인 시스템의 REST API 문서입니다.",
        version = "v1.0.0",
        contact = @Contact(
            name = "김규일",
            email = "rlarbdlf222@gmail.com",
            url = "https://github.com/Kimgyuilli"
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

    @Value("${swagger.server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${swagger.server.description:개발 서버}")
    private String serverDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl(serverUrl);
        server.setDescription(serverDescription);

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
                generateErrorCodeResponseExample(operation, customExceptionDescription.value());
            }
            
            // SuccessResponseDescription 어노테이션을 단 메소드에만 적용
            if (successResponseDescription != null) {
                generateSuccessResponseExample(operation, successResponseDescription.value());
            }
            
            if (preAuthorize != null) {
                operation.addSecurityItem(new SecurityRequirement().addList("JWT"));
            }

            return operation;
        };
    }

    private void generateErrorCodeResponseExample(Operation operation, SwaggerResponseDescription type) {
        ApiResponses responses = operation.getResponses();

        Set<ErrorCode> errorCodeList = type.getErrorCodeList();

        Map<Integer, List<ExampleHolder>> statusWithExampleHolders =
            errorCodeList.stream()
                .map(
                    errorCode ->
                        ExampleHolder.builder()
                            .holder(getSwaggerExample(errorCode))
                            .code(errorCode.getHttpCode())
                            .name(errorCode.toString())
                            .build()
                )
                .collect(groupingBy(ExampleHolder::getCode));
        addExamplesToResponses(responses, statusWithExampleHolders);
    }

    private Example getSwaggerExample(ErrorCode errorCode) {
        Object details = null;

        if (Objects.equals(errorCode.getCode(), ErrorCode.PARAMETER_VALIDATION_ERROR.getCode())) {
            ParameterData parameterData = new ParameterData("memberEmail", "invalid-email", "이메일 형식이 올바르지 않습니다");
            details = List.of(parameterData);
        }

        CommonApiResponse<Void> failResponse = CommonApiResponse.fail(errorCode);

        Example example = new Example();
        example.description(errorCode.getMessage());
        example.setValue(failResponse);
        return example;
    }

    private void addExamplesToResponses(ApiResponses responses,
        Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {
        statusWithExampleHolders.forEach(
            (status, v) -> {
                Content content = new Content();
                MediaType mediaType = new MediaType();
                ApiResponse apiResponse = new ApiResponse();
                v.forEach(
                    exampleHolder ->
                        mediaType.addExamples(exampleHolder.getName(), exampleHolder.getHolder()));
                content.addMediaType("application/json", mediaType);
                apiResponse.setDescription("에러 응답");
                apiResponse.setContent(content);
                responses.addApiResponse(status.toString(), apiResponse);
            });
    }

    private void generateSuccessResponseExample(Operation operation, SuccessCode successCode) {
        ApiResponses responses = operation.getResponses();

        Example successExample = getSuccessSwaggerExample(successCode);
        ExampleHolder exampleHolder = ExampleHolder.builder()
            .holder(successExample)
            .code(200)  // 성공 응답은 200으로 통일
            .name(successCode.toString())
            .build();

        Content content = new Content();
        MediaType mediaType = new MediaType();
        ApiResponse apiResponse = new ApiResponse();
        
        mediaType.addExamples(exampleHolder.getName(), exampleHolder.getHolder());
        content.addMediaType("application/json", mediaType);
        apiResponse.setDescription("성공 응답");
        apiResponse.setContent(content);
        
        responses.addApiResponse("200", apiResponse);
    }

    private Example getSuccessSwaggerExample(SuccessCode successCode) {
        CommonApiResponse<Void> successResponse = CommonApiResponse.success(successCode);

        Example example = new Example();
        example.description(successCode.getMessage());
        example.setValue(successResponse);
        return example;
    }
}
